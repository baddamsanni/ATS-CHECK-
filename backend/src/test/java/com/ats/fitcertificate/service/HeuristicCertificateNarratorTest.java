package com.ats.fitcertificate.service;

import com.ats.fitcertificate.dto.RequirementMatchResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HeuristicCertificateNarratorTest {

    private final SkillDictionary dict = new SkillDictionary();
    private final HeuristicCertificateNarrator narrator = new HeuristicCertificateNarrator(dict);

    @Test
    void producesCompleteV3Result() {
        String jd = "Senior Backend Engineer: Java, Spring Boot, Kafka, AWS, Kubernetes, Terraform, PostgreSQL, Redis";
        String resume = "Jane Doe — 7 years Java Spring Boot Kafka AWS Kubernetes Terraform PostgreSQL Redis";
        RequirementMatchResult r = narrator.narrate(resume, jd);
        assertNotNull(r.getCandidateName());
        assertTrue(r.getCandidateName().contains("Jane"));
        assertNotNull(r.getRoleTitle());
        assertNotNull(r.getTier());
        assertNotNull(r.getSummary());
        assertTrue(r.getRequirements().size() >= 6);
        assertTrue(r.getMatchedSkills().size() > 0);
    }

    @Test
    void extractsCandidateName() {
        String resume = "John Smith\nSenior Engineer\n5 years experience";
        RequirementMatchResult r = narrator.narrate(resume, "Java developer");
        assertEquals("John Smith", r.getCandidateName());
    }

    @Test
    void matchedSkillsAreShortKeywords() {
        String jd = "Java Spring Boot Kafka AWS Kubernetes";
        String resume = "Jane Doe — Java Spring Boot Kafka AWS Kubernetes developer";
        RequirementMatchResult r = narrator.narrate(resume, jd);
        // Skills should be short keywords, not full sentences
        for (String s : r.getMatchedSkills()) {
            assertTrue(s.length() < 30, "Skill should be short: " + s);
        }
    }

    @Test
    void allMetGivesExcellentTier() {
        String jd = "Java Spring Boot Kafka AWS Kubernetes Terraform PostgreSQL Redis";
        String resume = "Jane Doe — Java Spring Boot Kafka AWS Kubernetes Terraform PostgreSQL Redis";
        RequirementMatchResult r = narrator.narrate(resume, jd);
        assertEquals(TierClassifier.EXCELLENT, r.getTier());
    }

    @Test
    void unrelatedResumeGivesWeakTier() {
        String jd = "Java Spring Boot Kafka AWS Kubernetes Terraform PostgreSQL Redis";
        String resume = "Jane Doe — Graphic designer with Photoshop and Illustrator skills.";
        RequirementMatchResult r = narrator.narrate(resume, jd);
        assertTrue(r.getTier().equals(TierClassifier.WEAK) || r.getTier().equals(TierClassifier.AVERAGE));
    }

    @Test
    void summaryContainsCandidateName() {
        String jd = "Java developer with Spring Boot";
        String resume = "Alice Brown — 5 years Java Spring Boot experience";
        RequirementMatchResult r = narrator.narrate(resume, jd);
        assertTrue(r.getSummary().contains("Alice"));
    }
}
