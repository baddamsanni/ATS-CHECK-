package com.ats.fitcertificate.dto;

/**
 * One row in the JD-requirement → resume-evidence match table.
 */
public class RequirementMatch {
    private String requirement;
    private String status; // "met", "partial", "missing"
    private String evidence;

    public RequirementMatch() {}

    public RequirementMatch(String requirement, String status, String evidence) {
        this.requirement = requirement;
        this.status = status;
        this.evidence = evidence == null ? "" : evidence;
    }

    public String getRequirement() { return requirement; }
    public void setRequirement(String requirement) { this.requirement = requirement; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence == null ? "" : evidence; }

    public boolean isMet() { return "met".equalsIgnoreCase(status); }
    public boolean isPartial() { return "partial".equalsIgnoreCase(status); }
    public boolean isMissing() { return "missing".equalsIgnoreCase(status); }
}
