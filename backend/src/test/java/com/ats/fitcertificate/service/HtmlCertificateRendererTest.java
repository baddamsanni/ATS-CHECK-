package com.ats.fitcertificate.service;

import com.ats.fitcertificate.dto.RequirementMatch;
import com.ats.fitcertificate.dto.RequirementMatchResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HtmlCertificateRendererTest {

    private final HtmlCertificateRenderer renderer = new HtmlCertificateRenderer();

    @Test
    void rendersValidPdf() {
        byte[] pdf = renderer.render(fullResult("Excellent Fit"), "FC-TEST123456");
        assertNotNull(pdf);
        assertTrue(pdf.length > 100);
        assertEquals('%', (char) pdf[0]);
        assertEquals('P', (char) pdf[1]);
        assertEquals('D', (char) pdf[2]);
        assertEquals('F', (char) pdf[3]);
        String tail = new String(pdf, pdf.length - 20, 20);
        assertTrue(tail.contains("%%EOF"));
    }

    @Test
    void rendersAllTiers() {
        for (String tier : List.of("Excellent Fit", "Strong Fit", "Good Fit", "Average Fit", "Weak Fit")) {
            byte[] pdf = renderer.render(fullResult(tier), "FC-TEST123456");
            assertTrue(pdf.length > 100, "Failed for tier: " + tier);
        }
    }

    @Test
    void handlesEmptyRequirements() {
        RequirementMatchResult r = new RequirementMatchResult();
        r.setCandidateName("Test");
        r.setRoleTitle("Engineer");
        r.setTier("Weak Fit");
        r.setSummary("No data.");
        r.setRequirements(List.of());
        r.setMatchedSkills(List.of());
        byte[] pdf = renderer.render(r, "FC-TEST123456");
        assertNotNull(pdf);
        assertTrue(pdf.length > 100);
    }

    @Test
    void handlesNullFields() {
        RequirementMatchResult r = new RequirementMatchResult();
        r.setRequirements(List.of(new RequirementMatch("Java", "met", "proof")));
        byte[] pdf = renderer.render(r, "FC-TEST123456");
        assertNotNull(pdf);
        assertTrue(pdf.length > 100);
    }

    @Test
    void handlesManyRequirements() {
        RequirementMatchResult r = fullResult("Strong Fit");
        r.setRequirements(List.of(
                new RequirementMatch("Java", "met", "5y"),
                new RequirementMatch("Spring", "met", "3y"),
                new RequirementMatch("Kafka", "partial", "some"),
                new RequirementMatch("AWS", "met", "cert"),
                new RequirementMatch("K8s", "missing", ""),
                new RequirementMatch("Terraform", "missing", ""),
                new RequirementMatch("PostgreSQL", "met", "2y"),
                new RequirementMatch("Redis", "partial", "brief"),
                new RequirementMatch("Docker", "met", "daily"),
                new RequirementMatch("GraphQL", "missing", "")));
        byte[] pdf = renderer.render(r, "FC-TEST123456");
        assertNotNull(pdf);
        assertTrue(pdf.length > 100);
    }

    private RequirementMatchResult fullResult(String tier) {
        RequirementMatchResult r = new RequirementMatchResult();
        r.setCandidateName("Jane Doe");
        r.setRoleTitle("Backend Engineer");
        r.setTier(tier);
        r.setSummary("Jane is highly qualified for this Backend Engineer role. She brings 7+ years of experience with deep expertise in Java, Spring Boot, and AWS. She directly satisfies the core requirements of this job description with proven production experience.");
        r.setRequirements(List.of(
                new RequirementMatch("Java", "met", "5 years Java experience"),
                new RequirementMatch("Spring Boot", "met", "3 years Spring Boot"),
                new RequirementMatch("Kafka", "partial", "Some exposure"),
                new RequirementMatch("AWS", "met", "AWS certified"),
                new RequirementMatch("Terraform", "missing", "")));
        r.setMatchedSkills(List.of("Java", "Spring Boot", "AWS", "Kafka"));
        return r;
    }
}
