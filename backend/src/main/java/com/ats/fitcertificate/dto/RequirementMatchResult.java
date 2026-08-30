package com.ats.fitcertificate.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured output from the narrator (v3 schema).
 * Contains a tier label, named summary, requirement-match table, and short skill keywords.
 */
public class RequirementMatchResult {
    private String candidateName;
    private String roleTitle;
    private String tier; // "Excellent Fit", "Strong Fit", "Good Fit", "Average Fit", "Weak Fit"
    private String summary; // 3-4 sentence confident named paragraph
    private List<RequirementMatch> requirements = new ArrayList<>();
    private List<String> matchedSkills = new ArrayList<>(); // short skill keywords

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public String getRoleTitle() { return roleTitle; }
    public void setRoleTitle(String roleTitle) { this.roleTitle = roleTitle; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<RequirementMatch> getRequirements() { return requirements; }
    public void setRequirements(List<RequirementMatch> requirements) {
        this.requirements = requirements == null ? new ArrayList<>() : requirements;
    }

    public List<String> getMatchedSkills() { return matchedSkills; }
    public void setMatchedSkills(List<String> matchedSkills) {
        this.matchedSkills = matchedSkills == null ? new ArrayList<>() : matchedSkills;
    }
}
