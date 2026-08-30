package com.ats.fitcertificate.service;

import com.ats.fitcertificate.dto.RequirementMatch;
import com.ats.fitcertificate.dto.RequirementMatchResult;

import java.util.List;

/**
 * Derives a tier label from the requirement-match table.
 * Internal only — the number is never shown on the certificate.
 *
 * <ul>
 *   <li>all met → Excellent Fit</li>
 *   <li>mostly met, no missing → Strong Fit</li>
 *   <li>mostly met, few gaps → Good Fit</li>
 *   <li>mixed → Average Fit</li>
 *   <li>mostly missing → Weak Fit</li>
 * </ul>
 */
public final class TierClassifier {

    public static final String EXCELLENT = "Excellent Fit";
    public static final String STRONG = "Strong Fit";
    public static final String GOOD = "Good Fit";
    public static final String AVERAGE = "Average Fit";
    public static final String WEAK = "Weak Fit";

    private TierClassifier() {}

    /** Classify and set the tier on the result, validating/overriding the LLM's label. */
    public static void classifyAndValidate(RequirementMatchResult result) {
        String derived = classify(result.getRequirements());
        // Always trust the requirement table over the LLM's self-assessment.
        result.setTier(derived);
    }

    /** Pure tier derivation from the requirement list. */
    public static String classify(List<RequirementMatch> requirements) {
        if (requirements == null || requirements.isEmpty()) return WEAK;
        int total = requirements.size();
        long met = requirements.stream().filter(RequirementMatch::isMet).count();
        long partial = requirements.stream().filter(RequirementMatch::isPartial).count();
        long missing = requirements.stream().filter(RequirementMatch::isMissing).count();

        // all met
        if (met == total) return EXCELLENT;
        // mostly met, no missing (rest are partial)
        if (missing == 0 && met >= total * 0.6) return STRONG;
        // mostly met, few gaps
        if (met >= total * 0.5 && missing <= total * 0.3) return GOOD;
        // mostly missing
        if (missing >= total * 0.6) return WEAK;
        // mixed
        return AVERAGE;
    }

    /** Tier color for the badge gradient. */
    public static String tierColor(String tier) {
        if (EXCELLENT.equals(tier)) return "#16A34A";
        if (STRONG.equals(tier)) return "#EA580C";
        if (GOOD.equals(tier)) return "#F59E0B";
        if (AVERAGE.equals(tier)) return "#C98A2B";
        return "#A8A29E"; // Weak
    }
}
