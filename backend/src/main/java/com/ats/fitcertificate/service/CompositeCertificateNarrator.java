package com.ats.fitcertificate.service;

import com.ats.fitcertificate.dto.RequirementMatchResult;
import com.ats.fitcertificate.exception.UpstreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class CompositeCertificateNarrator implements CertificateNarrator {

    private static final Logger log = LoggerFactory.getLogger(CompositeCertificateNarrator.class);

    private final MergeGatewayNarrator mergeGateway;
    private final ClaudeCertificateNarrator claude;
    private final HeuristicCertificateNarrator heuristic;

    public CompositeCertificateNarrator(MergeGatewayNarrator mergeGateway,
                                        ClaudeCertificateNarrator claude,
                                        HeuristicCertificateNarrator heuristic) {
        this.mergeGateway = mergeGateway;
        this.claude = claude;
        this.heuristic = heuristic;
    }

    @Override
    public RequirementMatchResult narrate(String resumeText, String jobDescription) {
        return narrate(resumeText, jobDescription, null);
    }

    public RequirementMatchResult narrate(String resumeText, String jobDescription, String model) {
        if (mergeGateway.isConfigured()) {
            try { return mergeGateway.narrate(resumeText, jobDescription, model); }
            catch (UpstreamException e) { log.warn("Merge Gateway narration failed, falling back: {}", e.getMessage()); }
        }
        if (claude.isConfigured()) {
            try { return claude.narrate(resumeText, jobDescription); }
            catch (UpstreamException e) { log.warn("Claude narration failed, falling back: {}", e.getMessage()); }
        }
        return heuristic.narrate(resumeText, jobDescription);
    }

    public String activeBackend() {
        if (mergeGateway.isConfigured()) return "merge-gateway";
        if (claude.isConfigured()) return "claude";
        return "heuristic";
    }

    public boolean isUsingClaude() { return claude.isConfigured(); }
    public boolean isUsingMergeGateway() { return mergeGateway.isConfigured(); }
}
