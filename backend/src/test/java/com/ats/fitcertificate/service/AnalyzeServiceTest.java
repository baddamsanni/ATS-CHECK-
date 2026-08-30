package com.ats.fitcertificate.service;

import com.ats.fitcertificate.dto.AnalyzeResponse;
import com.ats.fitcertificate.dto.RequirementMatch;
import com.ats.fitcertificate.dto.RequirementMatchResult;
import com.ats.fitcertificate.exception.BadRequestException;
import com.ats.fitcertificate.exception.UnsupportedFileTypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AnalyzeServiceTest {

    private ResumeTextExtractor extractor;
    private CompositeCertificateNarrator narrator;
    private CertificateRenderer renderer;
    private AnalyzeService service;

    @BeforeEach
    void setUp() {
        extractor = mock(ResumeTextExtractor.class);
        narrator = mock(CompositeCertificateNarrator.class);
        renderer = mock(CertificateRenderer.class);
        service = new AnalyzeService(extractor, narrator, renderer);
        try { when(extractor.requireUsable(anyString())).thenCallRealMethod(); } catch (Exception ignored) {}
    }

    @Test
    void fullFlowProducesV3Response() {
        InputStream in = new ByteArrayInputStream("%PDF-1.7 fake".getBytes());
        when(extractor.extract(any(), anyString())).thenReturn(longResume());
        when(narrator.narrate(anyString(), anyString(), any())).thenReturn(result("Good Fit"));
        when(renderer.render(any(), any())).thenReturn(new byte[]{0x25, 0x50, 0x44, 0x46, 0x21});

        AnalyzeResponse resp = service.analyze(in, "resume.pdf", "Java JD", null);

        assertEquals("Jane Doe", resp.getCandidateName());
        assertEquals("Backend Engineer", resp.getRoleTitle());
        // Tier is recomputed: 3 met + 1 partial + 1 missing = Good Fit
        assertEquals("Good Fit", resp.getTier());
        assertNotNull(resp.getSummary());
        assertTrue(resp.getRequirements().size() > 0);
        assertNotNull(resp.getCertificateBase64());
        assertNotNull(resp.getVerificationId());
    }

    @Test
    void rejectsInvalidFileType() {
        InputStream in = new ByteArrayInputStream("text".getBytes());
        assertThrows(UnsupportedFileTypeException.class, () ->
                service.analyze(in, "fake.pdf", "JD", null));
    }

    @Test
    void rejectsShortResume() {
        InputStream in = new ByteArrayInputStream("%PDF-1.7".getBytes());
        when(extractor.extract(any(), anyString())).thenReturn("hi");
        assertThrows(BadRequestException.class, () ->
                service.analyze(in, "r.pdf", "JD", null));
    }

    @Test
    void tierValidatedFromRequirements() {
        InputStream in = new ByteArrayInputStream("%PDF-1.7".getBytes());
        when(extractor.extract(any(), anyString())).thenReturn(longResume());
        // LLM says Weak Fit but all requirements are met — should be overridden to Excellent
        RequirementMatchResult r = result("Weak Fit");
        r.setRequirements(List.of(
                new RequirementMatch("A", "met", "x"), new RequirementMatch("B", "met", "x"),
                new RequirementMatch("C", "met", "x"), new RequirementMatch("D", "met", "x"),
                new RequirementMatch("E", "met", "x")));
        when(narrator.narrate(anyString(), anyString(), any())).thenReturn(r);
        when(renderer.render(any(), any())).thenReturn(new byte[]{0x25, 0x50, 0x44, 0x46});

        AnalyzeResponse resp = service.analyze(in, "r.pdf", "JD", null);
        assertEquals("Excellent Fit", resp.getTier());
    }

    @Test
    void matchedSkillsFromResult() {
        InputStream in = new ByteArrayInputStream("%PDF-1.7".getBytes());
        when(extractor.extract(any(), anyString())).thenReturn(longResume());
        when(narrator.narrate(anyString(), anyString(), any())).thenReturn(result("Strong Fit"));
        when(renderer.render(any(), any())).thenReturn(new byte[]{0x25, 0x50, 0x44, 0x46});

        AnalyzeResponse resp = service.analyze(in, "r.pdf", "JD", null);
        assertTrue(resp.getMatchedSkills().contains("Java"));
    }

    @Test
    void verificationIdStartsWithFC() {
        InputStream in = new ByteArrayInputStream("%PDF-1.7".getBytes());
        when(extractor.extract(any(), anyString())).thenReturn(longResume());
        when(narrator.narrate(anyString(), anyString(), any())).thenReturn(result("Good Fit"));
        when(renderer.render(any(), any())).thenReturn(new byte[]{0x25, 0x50, 0x44, 0x46});

        AnalyzeResponse resp = service.analyze(in, "r.pdf", "JD", null);
        assertTrue(resp.getVerificationId().startsWith("FC-"));
    }

    @Test
    void handlesNullModel() {
        InputStream in = new ByteArrayInputStream("%PDF-1.7".getBytes());
        when(extractor.extract(any(), anyString())).thenReturn(longResume());
        when(narrator.narrate(anyString(), anyString(), any())).thenReturn(result("Average Fit"));
        when(renderer.render(any(), any())).thenReturn(new byte[]{0x25, 0x50, 0x44, 0x46});

        AnalyzeResponse resp = service.analyze(in, "r.pdf", "JD", null);
        // 3 met + 1 partial + 1 missing → Good Fit
        assertEquals("Good Fit", resp.getTier());
    }

    private RequirementMatchResult result(String tier) {
        RequirementMatchResult r = new RequirementMatchResult();
        r.setCandidateName("Jane Doe");
        r.setRoleTitle("Backend Engineer");
        r.setTier(tier);
        r.setSummary("Jane is highly qualified for this role.");
        r.setRequirements(List.of(
                new RequirementMatch("Java", "met", "5 years"),
                new RequirementMatch("Spring Boot", "met", "3 years"),
                new RequirementMatch("Kafka", "partial", "some"),
                new RequirementMatch("AWS", "met", "certified"),
                new RequirementMatch("Terraform", "missing", "")));
        r.setMatchedSkills(List.of("Java", "Spring Boot", "AWS"));
        return r;
    }

    private String longResume() {
        return "Jane Doe — Senior Engineer with 7 years of experience. "
                + "Skills include Java, Spring Boot, Kafka, AWS, Kubernetes, Terraform, "
                + "PostgreSQL, Redis. Built distributed microservices on AWS EKS.";
    }
}
