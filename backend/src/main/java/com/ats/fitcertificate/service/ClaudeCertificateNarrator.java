package com.ats.fitcertificate.service;

import com.ats.fitcertificate.dto.RequirementMatch;
import com.ats.fitcertificate.dto.RequirementMatchResult;
import com.ats.fitcertificate.exception.UpstreamException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ClaudeCertificateNarrator implements CertificateNarrator {

    private static final String SYSTEM_PROMPT =
            "You are an objective senior technical recruiter producing data for a one-page Resume Fit " +
            "Certificate. Output ONLY valid JSON, no markdown, no commentary.\n" +
            "Rules: (1) Extract 6-8 genuine, phrased requirements from the JD, not bare keywords — expand " +
            "short keyword-list JDs into proper requirement sentences for the role. " +
            "(2) Evidence MUST cite concrete resume proof — company, project, tool, or metric. NEVER output " +
            "\"Resume contains: X\" or \"Resume references: X\"; if there's no real evidence, status is not " +
            "met. (3) Use met/partial/missing honestly — not everything is met. (4) Summary: 3-4 sentences, " +
            "name the candidate once then use pronouns, state years + domain, name top 3 distinct strengths " +
            "(no duplicate skill words, no filler like 'including java and react'), cite 2 concrete proofs, " +
            "end with fit for THIS role. Never restate the tier. (5) Normalize skill casing: JavaScript, AWS, " +
            "CI/CD, REST APIs, Node.js. (6) matchedSkills = deduped, correctly cased keywords, max ~14, no " +
            "sentences. (7) tier: all met=Excellent Fit; mostly met no missing=Strong Fit; mostly met minor " +
            "gaps=Good Fit; mixed=Average Fit; mostly missing=Weak Fit.";

    private static final String USER_TEMPLATE =
            "JOB DESCRIPTION:\n<<<%s>>>\n\nRESUME:\n<<<%s>>>\n\n" +
            "Return JSON: {\"candidateName\":\"\",\"roleTitle\":\"\",\"tier\":\"Excellent Fit|Strong Fit|Good Fit|Average Fit|Weak Fit\",\"summary\":\"\",\"requirements\":[{\"requirement\":\"\",\"status\":\"met|partial|missing\",\"evidence\":\"\"}],\"matchedSkills\":[\"\"]}";

    private static final String STRICT_REMINDER =
            "Respond with ONLY the raw JSON object. No markdown, no code fences, no prose.";

    private final RestClient restClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey;
    private final String model;
    private final int maxTokens;

    public ClaudeCertificateNarrator(@Qualifier("anthropicRestClient") RestClient restClient,
                                     @Value("${ANTHROPIC_API_KEY:}") String apiKey,
                                     @Value("${anthropic.model:claude-sonnet-4-6}") String model,
                                     @Value("${anthropic.max-tokens:1500}") int maxTokens) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
    }

    public boolean isConfigured() { return apiKey != null && !apiKey.isBlank(); }

    @Override
    public RequirementMatchResult narrate(String resumeText, String jobDescription) {
        if (!isConfigured()) throw new UpstreamException("ANTHROPIC_API_KEY is not configured.");
        String userMessage = String.format(USER_TEMPLATE, sanitize(jobDescription), sanitize(resumeText));
        String raw = callClaude(userMessage, false);
        RequirementMatchResult result = parse(raw);
        if (result == null) {
            raw = callClaude(userMessage + "\n\n" + STRICT_REMINDER, true);
            result = parse(raw);
        }
        if (result == null) throw new UpstreamException("Claude returned unparseable response after retry.");
        return result;
    }

    private String callClaude(String userMessage, boolean strict) {
        Map<String, Object> body = Map.of(
                "model", model, "max_tokens", maxTokens,
                "system", SYSTEM_PROMPT + (strict ? " " + STRICT_REMINDER : ""),
                "messages", List.of(Map.of("role", "user", "content", userMessage))
        );
        try {
            return restClient.post().uri("/v1/messages")
                    .header("x-api-key", apiKey).header("anthropic-version", "2023-06-01")
                    .header("content-type", "application/json").body(body).retrieve().body(String.class);
        } catch (Exception e) {
            throw new UpstreamException("Claude API call failed: " + e.getMessage(), e);
        }
    }

    RequirementMatchResult parse(String responseJson) {
        if (responseJson == null || responseJson.isBlank()) return null;
        try {
            JsonNode root = mapper.readTree(responseJson.trim());
            String text = extractText(root);
            if (text == null || text.isBlank()) return null;
            JsonNode obj = mapper.readTree(stripFences(text));
            if (!obj.has("requirements") || !obj.has("summary")) return null;
            RequirementMatchResult r = new RequirementMatchResult();
            r.setCandidateName(obj.path("candidateName").asText(""));
            r.setRoleTitle(obj.path("roleTitle").asText(""));
            r.setTier(obj.path("tier").asText(""));
            r.setSummary(obj.path("summary").asText(""));
            r.setRequirements(parseReqs(obj.path("requirements")));
            r.setMatchedSkills(toStringList(obj.path("matchedSkills")));
            return r;
        } catch (Exception e) { return null; }
    }

    private static String extractText(JsonNode root) {
        if (root.has("content") && root.get("content").isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode part : root.get("content"))
                if ("text".equals(part.path("type").asText())) sb.append(part.path("text").asText(""));
            return sb.toString();
        }
        return root.toString();
    }

    private List<RequirementMatch> parseReqs(JsonNode node) {
        List<RequirementMatch> out = new ArrayList<>();
        if (node != null && node.isArray()) for (JsonNode n : node) {
            RequirementMatch rm = new RequirementMatch();
            rm.setRequirement(n.path("requirement").asText(""));
            rm.setStatus(n.path("status").asText("missing").toLowerCase());
            rm.setEvidence(n.path("evidence").asText(""));
            if (!rm.getRequirement().isBlank()) out.add(rm);
        }
        return out;
    }

    private List<String> toStringList(JsonNode node) {
        List<String> out = new ArrayList<>();
        if (node != null && node.isArray()) for (JsonNode n : node) {
            String s = n.asText(""); if (!s.isBlank()) out.add(s.trim());
        }
        return out;
    }

    private static String stripFences(String text) {
        String t = text.trim();
        if (t.startsWith("```")) {
            int nl = t.indexOf('\n'); if (nl > 0) t = t.substring(nl + 1);
            int lf = t.lastIndexOf("```"); if (lf >= 0) t = t.substring(0, lf);
            t = t.trim();
        }
        return t;
    }

    private static String sanitize(String s) {
        if (s == null) return ""; return s.replace("<<<", "«««").replace(">>>", "»»»");
    }
}
