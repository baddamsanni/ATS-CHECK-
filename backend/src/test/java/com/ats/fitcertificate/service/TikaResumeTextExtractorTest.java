package com.ats.fitcertificate.service;

import com.ats.fitcertificate.exception.BadRequestException;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TikaResumeTextExtractorTest {

    private final TikaResumeTextExtractor extractor = new TikaResumeTextExtractor();

    @Test
    void extractsTextFromRealPdf() {
        byte[] pdf = makePdf("Jane Doe - Senior Backend Engineer. "
                + "Java, Spring Boot, AWS, Kafka, Kubernetes, Terraform.");
        String text = extractor.extract(new ByteArrayInputStream(pdf), "resume.pdf");
        assertTrue(text.toLowerCase().contains("jane doe"));
        assertTrue(text.toLowerCase().contains("spring boot"));
        assertTrue(text.toLowerCase().contains("kafka"));
    }

    @Test
    void requireUsableThrowsForTooShortText() {
        assertThrows(BadRequestException.class, () -> extractor.requireUsable("hi"));
        assertThrows(BadRequestException.class, () -> extractor.requireUsable(""));
        assertThrows(BadRequestException.class, () -> extractor.requireUsable(null));
    }

    @Test
    void requireUsableAcceptsLongEnoughText() {
        String ok = "This is a long enough resume text to pass the minimum length check.";
        assertEquals(ok, extractor.requireUsable(ok));
    }

    @Test
    void normalizeStripsNullsAndCollapsesWhitespace() {
        String raw = "Hello \u0000 World   with   tabs\tand\n\n\nblank\nlines";
        String out = TikaResumeTextExtractor.normalize(raw);
        assertFalse(out.contains("\u0000"), "nulls should be stripped");
        assertFalse(out.contains("  "), "double spaces should collapse");
        assertFalse(out.contains("\t"), "tabs should collapse");
        assertTrue(out.contains("Hello World with tabs and"));
        assertTrue(out.contains("blank"));
        assertTrue(out.contains("lines"));
    }

    @Test
    void normalizeHandlesNull() {
        assertEquals("", TikaResumeTextExtractor.normalize(null));
        assertEquals("", TikaResumeTextExtractor.normalize(""));
    }

    @Test
    void extractThrowsForUnparseableContent() {
        byte[] fake = "%PDF-1.4\nnot really a pdf\n%%EOF\n".getBytes();
        assertThrows(BadRequestException.class, () ->
                extractor.extract(new ByteArrayInputStream(fake), "fake.pdf"));
    }

    /** Build a minimal valid PDF containing the given text using OpenHTMLtoPDF. */
    private static byte[] makePdf(String text) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String html = "<!DOCTYPE html><html><body><p>" +
                    text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") +
                    "</p></body></html>";
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
