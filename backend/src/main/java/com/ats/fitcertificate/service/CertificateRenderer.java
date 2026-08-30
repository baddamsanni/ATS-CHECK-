package com.ats.fitcertificate.service;

import com.ats.fitcertificate.dto.RequirementMatchResult;

/** Renders the certificate PDF from a requirement-match result. */
public interface CertificateRenderer {
    byte[] render(RequirementMatchResult result, String verificationId);
}
