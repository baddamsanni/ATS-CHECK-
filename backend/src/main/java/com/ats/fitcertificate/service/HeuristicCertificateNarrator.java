package com.ats.fitcertificate.service;

import com.ats.fitcertificate.dto.RequirementMatch;
import com.ats.fitcertificate.dto.RequirementMatchResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;

/**
 * Local, deterministic narrator for v3 schema. Derives a requirement-match
 * table without any LLM. Extracts candidate name, infers role, builds summary.
 */
@Service
public class HeuristicCertificateNarrator implements CertificateNarrator {

    private final SkillDictionary dictionary;

    public HeuristicCertificateNarrator(SkillDictionary dictionary) {
        this.dictionary = dictionary;
    }

    @Override
    public RequirementMatchResult narrate(String resumeText, String jobDescription) {
        String resumeLower = resumeText.toLowerCase();
        List<String> jdKeywords = dictionary.extractKeywords(jobDescription);
        String roleTitle = inferRole(jobDescription);

        List<RequirementMatch> requirements = new ArrayList<>();
        LinkedHashSet<String> matchedSkillsSet = new LinkedHashSet<>();
        for (String kw : jdKeywords) {
            String kwLower = kw.toLowerCase();
            String display = SkillDictionary.canonicalCase(kw);
            String requirementPhrase = "Proficiency in " + display + " for " + roleTitle + " work";
            String status, evidence;
            if (resumeLower.contains(kwLower)) {
                status = "met";
                evidence = display + " appears throughout the candidate's listed skills and experience.";
                matchedSkillsSet.add(display);
            } else if (partialMatch(kwLower, resumeLower)) {
                status = "partial";
                evidence = "Related terms to " + display + " appear in the resume, but not a direct match.";
            } else {
                status = "missing";
                evidence = "";
            }
            requirements.add(new RequirementMatch(requirementPhrase, status, evidence));
        }
        if (requirements.size() < 6) addDefaultRequirements(requirements, resumeLower);

        List<String> matchedSkills = new ArrayList<>(matchedSkillsSet);
        if (matchedSkills.size() > 14) matchedSkills = new ArrayList<>(matchedSkills.subList(0, 14));

        String candidateName = extractName(resumeText);
        String tier = TierClassifier.classify(requirements);
        String summary = buildSummary(candidateName, roleTitle, requirements, resumeText, matchedSkills);

        RequirementMatchResult result = new RequirementMatchResult();
        result.setCandidateName(candidateName);
        result.setRoleTitle(roleTitle);
        result.setTier(tier);
        result.setSummary(summary);
        result.setRequirements(requirements);
        result.setMatchedSkills(matchedSkills);
        return result;
    }

    private String extractName(String resume) {
        // Try to find a name at the start of the resume.
        // Look at first few lines, and also try extracting a name pattern
        // from the start of the text (before any dash or separator).
        String[] lines = resume.trim().split("\n");
        for (int i = 0; i < Math.min(5, lines.length); i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            if (line.contains("@") || line.contains("http") || line.matches(".*\\d{3}.*")) continue;
            // Try the whole line if it's 2-4 words
            String name = tryName(line);
            if (name != null) return name;
            // Try the part before a dash separator (em-dash, en-dash, hyphen with spaces)
            String[] dashParts = line.split("\\s+[—–-]\\s+");
            if (dashParts.length > 1) {
                name = tryName(dashParts[0]);
                if (name != null) return name;
            }
        }
        // Fallback: try first 2-3 words of the entire text
        String firstChunk = resume.trim().split("[\n—–-]")[0].trim();
        String name = tryName(firstChunk);
        if (name != null) return name;
        return "The candidate";
    }

    private String tryName(String text) {
        String[] words = text.trim().split("\\s+");
        if (words.length >= 2 && words.length <= 4) {
            for (String w : words) {
                if (w.length() < 2 || !w.matches("[A-Za-z][A-Za-z.'-]*")) return null;
            }
            return text.trim().replaceAll("[^A-Za-z.\\-' ]", "").trim();
        }
        return null;
    }

    private boolean partialMatch(String keyword, String resume) {
        String[] parts = keyword.split("[\\s/]+");
        for (String p : parts) if (p.length() > 3 && resume.contains(p)) return true;
        return false;
    }

    private void addDefaultRequirements(List<RequirementMatch> requirements, String resumeLower) {
        String[][] defaults = {
                {"Relevant hands-on work experience", "experience"},
                {"Clear written and verbal communication", "communicat"},
                {"Structured problem-solving approach", "problem"},
                {"Effective collaboration within a team", "team"},
                {"Consistent, on-time project delivery", "project"},
                {"Relevant education or certifications", "degree|certif|bachelor|master"}
        };
        Set<String> existing = new java.util.HashSet<>();
        for (RequirementMatch r : requirements) existing.add(r.getRequirement().toLowerCase());
        for (String[] d : defaults) {
            if (requirements.size() >= 6) break;
            if (existing.contains(d[0].toLowerCase())) continue;
            boolean found = false;
            for (String p : d[1].split("\\|")) if (resumeLower.contains(p)) { found = true; break; }
            requirements.add(new RequirementMatch(d[0], found ? "met" : "missing",
                    found ? "The resume demonstrates this through relevant background and history." : ""));
        }
    }

    private String inferRole(String jd) {
        String l = jd.toLowerCase();
        if (l.contains("android")) return "Android Developer";
        if (l.contains("ios") || l.contains("swift")) return "iOS Developer";
        if (l.contains("frontend") || l.contains("react")) return "Frontend Developer";
        if (l.contains("backend") || l.contains("server")) return "Backend Engineer";
        if (l.contains("devops") || l.contains("sre")) return "DevOps / SRE";
        if (l.contains("data scientist") || l.contains("ml")) return "Data Scientist / ML Engineer";
        if (l.contains("fullstack") || l.contains("full-stack")) return "Full-Stack Developer";
        if (l.contains("product manager")) return "Product Manager";
        if (l.contains("designer") || l.contains("ux")) return "Designer";
        return "Software Engineer";
    }

    private String buildSummary(String name, String role, List<RequirementMatch> reqs, String resume, List<String> matchedSkills) {
        long met = reqs.stream().filter(RequirementMatch::isMet).count();
        long total = reqs.size();
        String firstName = name.equals("The candidate") ? "The candidate" : name.split("\\s+")[0];

        String years = "several";
        java.util.regex.Pattern yp = java.util.regex.Pattern.compile("(\\d+)\\+?\\s*years?");
        java.util.regex.Matcher ym = yp.matcher(resume);
        if (ym.find()) years = ym.group(1) + "+";

        // Pick up to 3 distinct top skills — never repeated.
        List<String> topAreas = matchedSkills.size() > 3 ? matchedSkills.subList(0, 3) : matchedSkills;
        String topAreasStr = topAreas.isEmpty() ? "the core technologies for this role" : String.join(", ", topAreas);

        String tier = TierClassifier.classify(reqs);
        String qualifier;
        if (tier.equals(TierClassifier.EXCELLENT)) qualifier = "highly qualified";
        else if (tier.equals(TierClassifier.STRONG)) qualifier = "well qualified";
        else if (tier.equals(TierClassifier.GOOD)) qualifier = "a solid candidate";
        else if (tier.equals(TierClassifier.AVERAGE)) qualifier = "a partial match";
        else qualifier = "a limited match";

        String pronoun = firstName.equals("The candidate") ? "They" : firstName;
        String pronounObj = firstName.equals("The candidate") ? "them" : firstName.toLowerCase();

        return String.format(
                "%s is %s for this %s role. %s brings %s years of experience with demonstrated proficiency in %s, " +
                "and directly satisfies %d of %d core requirements identified from the job description. " +
                "This makes %s a strong candidate to evaluate further for the position.",
                firstName, qualifier, role, pronoun, years, topAreasStr, met, total, pronounObj
        );
    }
}
