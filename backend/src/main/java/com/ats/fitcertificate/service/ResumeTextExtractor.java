package com.ats.fitcertificate.service;

import com.ats.fitcertificate.exception.BadRequestException;

import java.io.InputStream;

/** Extracts raw text from a resume document (PDF/DOCX). */
public interface ResumeTextExtractor {
    String extract(InputStream in, String originalFilename);

    /** Minimum useful resume length after extraction. */
    int MIN_RESUME_CHARS = 40;

    default String requireUsable(String text) {
        if (text == null || text.isBlank() || text.trim().length() < MIN_RESUME_CHARS) {
            throw new BadRequestException(
                    "Could not extract enough text from the resume. " +
                    "Ensure it is a text-based PDF/DOCX (not a scanned image).");
        }
        return text;
    }
}
