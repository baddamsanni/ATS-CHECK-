package com.ats.fitcertificate.service;

import com.ats.fitcertificate.exception.UnsupportedFileTypeException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Validates uploaded files by magic bytes (not just extension).
 */
public class FileTypeValidator {

    private FileTypeValidator() {}

    public static void validatePdfOrDocx(InputStream in) {
        try {
            if (!in.markSupported()) {
                throw new UnsupportedFileTypeException("Stream does not support mark/reset.");
            }
            in.mark(8);
            byte[] header = in.readNBytes(8);
            in.reset();

            if (header.length < 4) {
                throw new UnsupportedFileTypeException("File is too small to be a valid document.");
            }

            // PDF: %PDF-
            if (header[0] == 0x25 && header[1] == 0x50 && header[2] == 0x44 && header[3] == 0x46) {
                return;
            }
            // DOCX/XLSX/PPTX (ZIP/OOXML): PK\x03\x04
            if (header[0] == 0x50 && header[1] == 0x4B
                    && (header[2] == 0x03 || header[2] == 0x04 || header[2] == 0x05 || header[2] == 0x07)
                    && (header[3] == 0x04 || header[3] == 0x06 || header[3] == 0x08)) {
                return;
            }
            // Old DOC (OLE2 Compound): D0 CF 11 E0
            if (header[0] == (byte) 0xD0 && header[1] == (byte) 0xCF
                    && header[2] == (byte) 0x11 && header[3] == (byte) 0xE0) {
                // Accept legacy .doc as well; Tika can parse it.
                return;
            }
            throw new UnsupportedFileTypeException(
                    "Unsupported file type. Only PDF and DOCX are accepted.");
        } catch (IOException e) {
            throw new UnsupportedFileTypeException("Could not read file header: " + e.getMessage());
        }
    }
}
