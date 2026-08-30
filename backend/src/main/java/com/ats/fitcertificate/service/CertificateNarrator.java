package com.ats.fitcertificate.service;

import com.ats.fitcertificate.dto.RequirementMatchResult;

/** Calls an LLM (or heuristic) to produce the requirement-match analysis. */
public interface CertificateNarrator {
    RequirementMatchResult narrate(String resumeText, String jobDescription);
}
