package com.ats.fitcertificate.service;

import com.ats.fitcertificate.dto.RequirementMatch;
import com.ats.fitcertificate.dto.RequirementMatchResult;
import com.ats.fitcertificate.exception.UpstreamException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM narrator that routes through Merge Gateway using the OpenAI-compatible
 * chat-completions surface. Produces v3 schema: candidate name, tier, named
 * summary, requirement-match table, and short skill keywords.
 */
@Service
public class MergeGatewayNarrator implements CertificateNarrator {

    private static final Logger log = LoggerFactory.getLogger(MergeGatewayNarrator.class);

    public static final List<ModelOption> SUPPORTED_MODELS = List.of(
            new ModelOption("google/gemini-3-flash-preview", "Gemini 3 Flash Preview",
                    "Google — fast, capable, great default"),
            new ModelOption("minimax/minimax-m2.7", "MiniMax M2.7",
                    "MiniMax — strong reasoning, cost-efficient"),
            new ModelOption("zai/glm-5.3-flash", "GLM 5.3 Flash",
                    "Zhipu AI — fast lightweight flash variant"),
            new ModelOption("anthropic/claude-sonnet-4-6", "Claude Sonnet 4.6",
                    "Anthropic — deep reasoning via gateway routing")
    );

    private static final String SYSTEM_PROMPT =
            "You are an objective senior technical recruiter. You are given a candidate's RESUME text and a " +
            "JOB DESCRIPTION. Produce the data for a one-page \"Résumé Fit Certificate\". Output ONLY valid JSON " +
            "matching the schema — no markdown, no commentary.\n\n" +
            "Hard rules (fixing known failures):\n" +
            "1. Real requirements, not keywords. Extract 6-8 genuine, phrased requirements from the JD " +
            "(e.g. \"Build responsive UIs with React and TypeScript\"), NOT bare words like \"Js\" or \"Aws\". " +
            "If the JD is a short keyword list, expand each into a proper requirement sentence for that role.\n" +
            "2. Real evidence, never keyword echoes. Evidence MUST cite concrete proof from the resume — " +
            "company, project, tool, or metric (e.g. \"Built React/Redux dashboards at Fifth Third Bank serving " +
            "2M+ users\"). NEVER output \"Resume contains: X\" or \"Resume references: X\". If there is no real " +
            "evidence, the status is not \"met\".\n" +
            "3. Status honesty. Use met / partial / missing based on actual resume proof. Not everything is " +
            "\"met\". Partial = mentioned but shallow. Missing = absent (evidence = \"\").\n" +
            "4. Summary must be clean and specific. 3-4 sentences. Name the candidate once, then use pronouns. " +
            "State years + domain, name their TOP 3 real strengths (no duplicates, no \"including java and " +
            "react\" filler), cite 2 concrete proofs, and end with why they suit THIS role. Never repeat a " +
            "skill word twice. Never restate the tier.\n" +
            "5. Proper casing. Normalize skills to correct form: JavaScript, AWS, CI/CD, REST APIs, Node.js — " +
            "not \"Js\", \"Aws\".\n" +
            "6. Skills = deduped keyword chips, correctly cased, max ~14. No sentences.\n" +
            "7. Tier derived from statuses (internal): all met -> \"Excellent Fit\"; mostly met, no missing -> " +
            "\"Strong Fit\"; mostly met, minor gaps -> \"Good Fit\"; mixed -> \"Average Fit\"; mostly missing -> " +
            "\"Weak Fit\". Display words only, never a score.";

    private static final String USER_TEMPLATE =
            "JOB DESCRIPTION:\n<<<%s>>>\n\nRESUME:\n<<<%s>>>\n\n" +
            "Return JSON with this exact schema:\n" +
            "{\n" +
            "  \"candidateName\": \"\",\n" +
            "  \"roleTitle\": \"\",\n" +
            "  \"tier\": \"Excellent Fit | Strong Fit | Good Fit | Average Fit | Weak Fit\",\n" +
            "  \"summary\": \"\",\n" +
            "  \"requirements\": [\n" +
            "    { \"requirement\": \"\", \"status\": \"met|partial|missing\", \"evidence\": \"\" }\n" +
            "  ],\n" +
            "  \"matchedSkills\": [\"\"]\n" +
            "}";

    private static final String STRICT_REMINDER =
            "Respond with ONLY the raw JSON object. No markdown, no code fences, no prose.";

    private final RestClient restClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey;
    private final String defaultModel;
    private final int maxTokens;

    public MergeGatewayNarrator(RestClient.Builder restClientBuilder,
                                @Value("${MERGE_GATEWAY_API_KEY:}") String apiKey,
                                @Value("${merge.gateway.base-url:https://api-gateway.merge.dev/v1/openai}") String baseUrl,
                                @Value("${merge.gateway.model:google/gemini-3-flash-preview}") String defaultModel,
                                @Value("${merge.gateway.max-tokens:1500}") int maxTokens) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.defaultModel = defaultModel;
        this.maxTokens = maxTokens;
    }

    public boolean isConfigured() { return apiKey != null && !apiKey.isBlank(); }
    public String getDefaultModel() { return defaultModel; }

    @Override
    public RequirementMatchResult narrate(String resumeText, String jobDescription) {
        return narrate(resumeText, jobDescription, defaultModel);
    }

    public RequirementMatchResult narrate(String resumeText, String jobDescription, String model) {
        if (!isConfigured()) {
            throw new UpstreamException("MERGE_GATEWAY_API_KEY is not configured.");
        }
        String useModel = (model == null || model.isBlank()) ? defaultModel : model;
        String userMessage = String.format(USER_TEMPLATE, sanitize(jobDescription), sanitize(resumeText));

        String raw = callGateway(useModel, userMessage, false);
        RequirementMatchResult result = parse(raw);
        if (result == null) {
            log.warn("Merge Gateway ({}) returned unparseable JSON on first attempt. Raw response: {}", useModel, truncateForLog(raw));
            raw = callGateway(useModel, userMessage + "\n\n" + STRICT_REMINDER, true);
            result = parse(raw);
        }
        if (result == null) {
            log.warn("Merge Gateway ({}) returned unparseable JSON on retry. Raw response: {}", useModel, truncateForLog(raw));
            throw new UpstreamException("Merge Gateway returned unparseable response from " + useModel + " after retry.");
        }
        return result;
    }

    private static String truncateForLog(String s) {
        if (s == null) return "null";
        return s.length() > 800 ? s.substring(0, 800) + "...[truncated]" : s;
    }

    private String callGateway(String model, String userMessage, boolean strict) {
        String system = strict ? SYSTEM_PROMPT + " " + STRICT_REMINDER : SYSTEM_PROMPT;
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        body.put("messages", List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", userMessage)
        ));
        body.put("reasoning_effort", "none");
        try {
            return restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            throw new UpstreamException("Merge Gateway API call failed for " + model + ": " + e.getMessage(), e);
        }
    }

    RequirementMatchResult parse(String responseJson) {
        if (responseJson == null || responseJson.isBlank()) return null;
        try {
            JsonNode root = mapper.readTree(responseJson.trim());
            String text = extractContent(root);
            if (text == null || text.isBlank()) return null;
            String json = stripFences(text);
            JsonNode obj = mapper.readTree(json);
            if (!obj.has("requirements") || !obj.has("summary")) return null;

            RequirementMatchResult r = new RequirementMatchResult();
            r.setCandidateName(obj.path("candidateName").asText(""));
            r.setRoleTitle(obj.path("roleTitle").asText(""));
            r.setTier(obj.path("tier").asText(""));
            r.setSummary(obj.path("summary").asText(""));
            r.setRequirements(parseRequirements(obj.path("requirements")));
            r.setMatchedSkills(toStringList(obj.path("matchedSkills")));
            return r;
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractContent(JsonNode root) {
        if (root.has("choices") && root.get("choices").isArray() && !root.get("choices").isEmpty()) {
            JsonNode first = root.get("choices").get(0);
            JsonNode message = first.path("message");
            if (message.has("content")) return message.get("content").asText("");
            if (first.has("content")) return first.get("content").asText("");
        }
        if (root.has("requirements")) return root.toString();
        return null;
    }

    private List<RequirementMatch> parseRequirements(JsonNode node) {
        List<RequirementMatch> out = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode n : node) {
                RequirementMatch rm = new RequirementMatch();
                rm.setRequirement(n.path("requirement").asText(""));
                rm.setStatus(n.path("status").asText("missing").toLowerCase());
                rm.setEvidence(n.path("evidence").asText(""));
                if (!rm.getRequirement().isBlank()) out.add(rm);
            }
        }
        return out;
    }

    private List<String> toStringList(JsonNode node) {
        List<String> out = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode n : node) {
                String s = n.asText("");
                if (!s.isBlank()) out.add(s.trim());
            }
        }
        return out;
    }

    private static String stripFences(String text) {
        String t = text.trim();
        if (t.startsWith("```")) {
            int firstNewline = t.indexOf('\n');
            if (firstNewline > 0) t = t.substring(firstNewline + 1);
            int lastFence = t.lastIndexOf("```");
            if (lastFence >= 0) t = t.substring(0, lastFence);
            t = t.trim();
        }
        return t;
    }

    private static String sanitize(String s) {
        if (s == null) return "";
        return s.replace("<<<", "«««").replace(">>>", "»»»");
    }

    public record ModelOption(String id, String displayName, String description) {}
}
