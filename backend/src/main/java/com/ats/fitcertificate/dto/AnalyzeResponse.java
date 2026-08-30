package com.ats.fitcertificate.dto;

import java.util.List;

public class AnalyzeResponse {
    private String candidateName;
    private String roleTitle;
    private String tier;
    private String summary;
    private List<RequirementMatch> requirements;
    private List<String> matchedSkills;
    private String certificateBase64;
    private String verificationId;

    public AnalyzeResponse() {}

    public AnalyzeResponse(String candidateName, String roleTitle, String tier,
                           String summary, List<RequirementMatch> requirements,
                           List<String> matchedSkills, String certificateBase64,
                           String verificationId) {
        this.candidateName = candidateName;
        this.roleTitle = roleTitle;
        this.tier = tier;
        this.summary = summary;
        this.requirements = requirements;
        this.matchedSkills = matchedSkills;
        this.certificateBase64 = certificateBase64;
        this.verificationId = verificationId;
    }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public String getRoleTitle() { return roleTitle; }
    public void setRoleTitle(String roleTitle) { this.roleTitle = roleTitle; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<RequirementMatch> getRequirements() { return requirements; }
    public void setRequirements(List<RequirementMatch> requirements) { this.requirements = requirements; }

    public List<String> getMatchedSkills() { return matchedSkills; }
    public void setMatchedSkills(List<String> matchedSkills) { this.matchedSkills = matchedSkills; }

    public String getCertificateBase64() { return certificateBase64; }
    public void setCertificateBase64(String certificateBase64) { this.certificateBase64 = certificateBase64; }

    public String getVerificationId() { return verificationId; }
    public void setVerificationId(String verificationId) { this.verificationId = verificationId; }
}
