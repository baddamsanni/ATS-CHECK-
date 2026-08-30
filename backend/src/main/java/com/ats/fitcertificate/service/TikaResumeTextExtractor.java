package com.ats.fitcertificate.service;

import com.ats.fitcertificate.exception.BadRequestException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.xml.sax.ContentHandler;

import java.io.InputStream;

@Service
public class TikaResumeTextExtractor implements ResumeTextExtractor {

    // -1 disables the write limit; resumes are small.
    private static final int WRITE_LIMIT = -1;

    @Override
    public String extract(InputStream in, String originalFilename) {
        try {
            ContentHandler handler = new BodyContentHandler(WRITE_LIMIT);
            Metadata metadata = new Metadata();
            if (originalFilename != null) {
                metadata.set("resourceName", originalFilename);
            }
            AutoDetectParser parser = new AutoDetectParser();
            ParseContext context = new ParseContext();
            parser.parse(in, handler, metadata, context);
            String raw = handler.toString();
            return normalize(raw);
        } catch (Exception e) {
            throw new BadRequestException("Failed to parse resume document: " + e.getMessage());
        }
    }

    /** Strip nulls and collapse whitespace. */
    static String normalize(String raw) {
        if (raw == null) return "";
        String noNulls = raw.replace("\u0000", "");
        // Collapse runs of whitespace into single spaces but preserve newlines.
        String[] lines = noNulls.split("\\r?\\n");
        StringBuilder sb = new StringBuilder(noNulls.length());
        for (String line : lines) {
            String trimmed = line.replaceAll("[\\t\\f ]+", " ").trim();
            if (!trimmed.isEmpty()) {
                if (sb.length() > 0) sb.append('\n');
                sb.append(trimmed);
            }
        }
        return sb.toString();
    }
}
