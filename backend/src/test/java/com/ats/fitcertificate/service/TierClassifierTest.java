package com.ats.fitcertificate.service;

import com.ats.fitcertificate.dto.RequirementMatch;
import com.ats.fitcertificate.dto.RequirementMatchResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TierClassifierTest {

    @Test
    void allMetIsExcellent() {
        assertEquals(TierClassifier.EXCELLENT, TierClassifier.classify(List.of(
                m("A", "met"), m("B", "met"), m("C", "met"))));
    }

    @Test
    void mostlyMetNoMissingIsStrong() {
        assertEquals(TierClassifier.STRONG, TierClassifier.classify(List.of(
                m("A", "met"), m("B", "met"), m("C", "met"), m("D", "partial"), m("E", "partial"))));
    }

    @Test
    void mostlyMetFewGapsIsGood() {
        // 3 met, 1 partial, 1 missing out of 5 → met=60%, missing=20%
        assertEquals(TierClassifier.GOOD, TierClassifier.classify(List.of(
                m("A", "met"), m("B", "met"), m("C", "met"), m("D", "partial"), m("E", "missing"))));
    }

    @Test
    void mostlyMissingIsWeak() {
        assertEquals(TierClassifier.WEAK, TierClassifier.classify(List.of(
                m("A", "missing"), m("B", "missing"), m("C", "missing"), m("D", "missing"), m("E", "met"))));
    }

    @Test
    void mixedIsAverage() {
        // 2 met, 1 partial, 3 missing → met=33%, missing=50%
        assertEquals(TierClassifier.AVERAGE, TierClassifier.classify(List.of(
                m("A", "met"), m("B", "met"), m("C", "partial"), m("D", "missing"), m("E", "missing"), m("F", "missing"))));
    }

    @Test
    void emptyIsWeak() {
        assertEquals(TierClassifier.WEAK, TierClassifier.classify(List.of()));
        assertEquals(TierClassifier.WEAK, TierClassifier.classify(null));
    }

    @Test
    void classifyAndValidateOverridesLlmTier() {
        RequirementMatchResult r = new RequirementMatchResult();
        r.setTier("Weak Fit"); // wrong
        r.setRequirements(List.of(m("A", "met"), m("B", "met"), m("C", "met")));
        TierClassifier.classifyAndValidate(r);
        assertEquals(TierClassifier.EXCELLENT, r.getTier());
    }

    @Test
    void tierColorsCorrect() {
        assertEquals("#16A34A", TierClassifier.tierColor(TierClassifier.EXCELLENT));
        assertEquals("#EA580C", TierClassifier.tierColor(TierClassifier.STRONG));
        assertEquals("#F59E0B", TierClassifier.tierColor(TierClassifier.GOOD));
        assertEquals("#C98A2B", TierClassifier.tierColor(TierClassifier.AVERAGE));
        assertEquals("#A8A29E", TierClassifier.tierColor(TierClassifier.WEAK));
    }

    private RequirementMatch m(String req, String status) {
        return new RequirementMatch(req, status, "");
    }
}
