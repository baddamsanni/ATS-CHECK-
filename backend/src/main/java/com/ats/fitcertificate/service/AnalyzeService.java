package com.ats.fitcertificate.service;

import com.ats.fitcertificate.dto.AnalyzeResponse;
import com.ats.fitcertificate.dto.RequirementMatchResult;
import com.ats.fitcertificate.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;

/** Orchestrates: extract → narrate → classify tier → render. */
@Service
public class AnalyzeService {

    private final ResumeTextExtractor extractor;
    private final CompositeCertificateNarrator narrator;
    private final CertificateRenderer renderer;

    public AnalyzeService(ResumeTextExtractor extractor,
                          CompositeCertificateNarrator narrator,
                          CertificateRenderer renderer) {
        this.extractor = extractor;
        this.narrator = narrator;
        this.renderer = renderer;
    }

    public AnalyzeResponse analyze(InputStream resumeStream, String filename,
                                   String jobDescription, String model) {
        FileTypeValidator.validatePdfOrDocx(resumeStream);
        String resumeText = extractor.requireUsable(extractor.extract(resumeStream, filename));

        RequirementMatchResult result = narrator.narrate(resumeText, jobDescription, model);

        // Validate/override tier from the requirement table.
        TierClassifier.classifyAndValidate(result);

        String verificationId = generateVerificationId(result);
        byte[] pdf = renderer.render(result, verificationId);
        String b64 = Base64.getEncoder().encodeToString(pdf);

        return new AnalyzeResponse(
                result.getCandidateName(),
                result.getRoleTitle(),
                result.getTier(),
                result.getSummary(),
                result.getRequirements(),
                result.getMatchedSkills(),
                b64,
                verificationId
        );
    }

    private String generateVerificationId(RequirementMatchResult result) {
        try {
            String input = (result.getSummary() == null ? "" : result.getSummary())
                    + "|" + result.getTier() + "|" + System.currentTimeMillis();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return "FC-" + HexFormat.of().formatHex(hash).substring(0, 12).toUpperCase();
        } catch (Exception e) {
            return "FC-" + Long.toHexString(System.nanoTime());
        }
    }
}
