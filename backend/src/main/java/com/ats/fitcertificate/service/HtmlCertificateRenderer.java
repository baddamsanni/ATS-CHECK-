package com.ats.fitcertificate.service;

import com.ats.fitcertificate.dto.RequirementMatch;
import com.ats.fitcertificate.dto.RequirementMatchResult;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Compact single-page A4 certificate. "Precision Engineering" theme: an
 * all-white page, dark-slate text, geometric sans-serif type, and a cobalt/
 * emerald accent — a clean, tier-1-tech dashboard look.
 */
@Service
public class HtmlCertificateRenderer implements CertificateRenderer {

    // Generous starting limits — content is only trimmed further if it would spill to a second page.
    // The header now consolidates title/badge/name into a single compact bar, freeing enough
    // vertical space for the summary card to comfortably show ~5 lines uncropped.
    private static final int MAX_SUMMARY = 480;
    private static final int MAX_EVIDENCE = 160;
    private static final int MAX_REQUIREMENT = 90;
    private static final int MAX_REQUIREMENTS = 8;
    private static final int MAX_SKILLS = 14;

    @Override
    public byte[] render(RequirementMatchResult result, String verificationId) {
        String tier = result.getTier() == null || result.getTier().isBlank()
                ? TierClassifier.classify(result.getRequirements()) : result.getTier();
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));

        List<RequirementMatch> sorted = new ArrayList<>(result.getRequirements() == null
                ? List.of() : result.getRequirements());
        sorted.sort(Comparator.comparingInt(this::statusOrder));

        // Try progressively tighter content budgets until the certificate fits on exactly one page.
        int[][] budgets = {
                {MAX_SUMMARY, MAX_EVIDENCE, MAX_REQUIREMENT, MAX_REQUIREMENTS},
                {380, 120, 70, 8},
                {280, 90, 55, 7},
                {200, 65, 45, 6},
        };
        byte[] pdf = null;
        for (int[] budget : budgets) {
            String html = buildHtml(result, tier, date, sorted, budget[0], budget[1], budget[2], budget[3]);
            pdf = htmlToPdf(html);
            if (countPages(pdf) <= 1) return pdf;
        }
        return pdf;
    }

    private int countPages(byte[] pdf) {
        try (PDDocument doc = PDDocument.load(pdf)) {
            return doc.getNumberOfPages();
        } catch (Exception e) {
            return 1;
        }
    }

    private int statusOrder(RequirementMatch r) {
        if (r.isMet()) return 0;
        if (r.isPartial()) return 1;
        return 2;
    }

    private String buildHtml(RequirementMatchResult result, String tier, String date,
                             List<RequirementMatch> sorted, int maxSummary, int maxEvidence,
                             int maxRequirement, int maxRequirements) {
        String name = esc(result.getCandidateName() == null || result.getCandidateName().isBlank()
                ? "The Candidate" : result.getCandidateName());
        String role = esc(result.getRoleTitle() == null || result.getRoleTitle().isBlank()
                ? "Software Engineer" : result.getRoleTitle());
        String summary = esc(truncate(result.getSummary(), maxSummary));
        List<String> skills = truncateList(result.getMatchedSkills(), MAX_SKILLS);
        List<RequirementMatch> displayReqs = sorted.size() > maxRequirements
                ? sorted.subList(0, maxRequirements) : sorted;

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>");
        sb.append("<style type=\"text/css\">").append(CSS).append("</style></head><body>");

        sb.append("<div class=\"page\">");

        // Single consolidated top bar: title, tier badge, and candidate name/role/date
        // all in one row — this replaces the old header band + separate hero section,
        // freeing vertical space for a longer, uncropped summary.
        sb.append("<div class=\"header-band\"><table class=\"header-table\"><tr>");
        sb.append("<td class=\"header-title-cell\"><h1>RESUME CERTIFIED&gt;</h1></td>");
        sb.append("<td class=\"header-badge-cell\"><div class=\"tier-badge\" style=\"").append(tierBadgeStyle(tier)).append("\">")
          .append(esc(tier.toUpperCase())).append("</div></td>");
        sb.append("<td class=\"header-info-cell\">");
        sb.append("<div class=\"header-name\">").append(name).append("</div>");
        sb.append("<div class=\"header-meta\">").append(role).append(" &#183; ").append(date).append("</div>");
        sb.append("</td>");
        sb.append("</tr></table></div>");

        // Summary card
        sb.append("<div class=\"summary-card\"><p class=\"summary\">").append(summary).append("</p></div>");

        // Requirement Match — single heading, tight 3-col table
        sb.append(sectionTitle("Requirement Match"));
        sb.append("<div class=\"table-card\">");
        sb.append("<table class=\"req-table\"><thead><tr>");
        sb.append("<th class=\"th-req\">Requirement</th>");
        sb.append("<th class=\"th-status\">Status</th>");
        sb.append("<th class=\"th-evidence\">Evidence</th>");
        sb.append("</tr></thead><tbody>");
        for (int i = 0; i < displayReqs.size(); i++) {
            RequirementMatch r = displayReqs.get(i);
            sb.append("<tr class=\"").append(i % 2 == 0 ? "row-even" : "row-odd").append("\">");
            sb.append("<td class=\"td-req\">").append(esc(truncate(r.getRequirement(), maxRequirement))).append("</td>");
            sb.append("<td class=\"td-status\">").append(statusChip(r)).append("</td>");
            sb.append("<td class=\"td-evidence").append(r.isMissing() ? " muted" : "").append("\">")
              .append(r.isMissing() ? "&#8212;" : esc(truncate(r.getEvidence(), maxEvidence))).append("</td>");
            sb.append("</tr>");
        }
        sb.append("</tbody></table>");
        sb.append("</div>");

        // Matched skills
        if (!skills.isEmpty()) {
            sb.append(sectionTitle("Matched Skills"));
            sb.append("<div class=\"skills-card\">");
            for (String s : skills) sb.append("<span class=\"skill\">").append(esc(s)).append("</span>");
            sb.append("</div>");
        }

        sb.append("</div></body></html>");
        return sb.toString();
    }

    private String sectionTitle(String text) {
        return "<div class=\"section\"><div class=\"section-title\">" + text + "</div>"
                + "<div class=\"section-underline\"></div></div>";
    }

    /** Tier badge: emerald/cobalt fill for stronger tiers, dark slate for weaker ones. */
    private String tierBadgeStyle(String tier) {
        if (TierClassifier.EXCELLENT.equalsIgnoreCase(tier)) return "background:#059669;color:#FFFFFF;";
        if (TierClassifier.STRONG.equalsIgnoreCase(tier)) return "background:#10B981;color:#FFFFFF;";
        if (TierClassifier.GOOD.equalsIgnoreCase(tier)) return "background:#2563EB;color:#FFFFFF;";
        if (TierClassifier.AVERAGE.equalsIgnoreCase(tier)) return "background:#64748B;color:#FFFFFF;";
        return "background:#1E293B;color:#FFFFFF;";
    }

    private String statusChip(RequirementMatch r) {
        String bg, fg, label;
        if (r.isMet()) { bg = "#D1FAE5"; fg = "#065F46"; label = "Met"; }
        else if (r.isPartial()) { bg = "#FEF3C7"; fg = "#92400E"; label = "Partial"; }
        else { bg = "#F1F5F9"; fg = "#64748B"; label = "Missing"; }
        return "<span class=\"chip\" style=\"background:" + bg + ";color:" + fg + ";\">" + label + "</span>";
    }

    /** Every letter of this certificate renders in the Inter typeface — a modern geometric
     *  sans-serif embedded directly into the PDF (regular/medium/semibold/bold weights). */
    private static final String FONT_FAMILY = "Inter";

    private byte[] htmlToPdf(String html) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            registerFont(builder, "fonts/Inter-Regular.ttf", 400, FontStyle.NORMAL);
            registerFont(builder, "fonts/Inter-Medium.ttf", 500, FontStyle.NORMAL);
            registerFont(builder, "fonts/Inter-SemiBold.ttf", 600, FontStyle.NORMAL);
            registerFont(builder, "fonts/Inter-Bold.ttf", 700, FontStyle.NORMAL);
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to render PDF: " + e.getMessage(), e);
        }
    }

    private void registerFont(PdfRendererBuilder builder, String classpathLocation, int weight, FontStyle style) {
        builder.useFont(() -> {
            try {
                InputStream is = new ClassPathResource(classpathLocation).getInputStream();
                return is;
            } catch (Exception e) {
                throw new RuntimeException("Failed to load font: " + classpathLocation, e);
            }
        }, FONT_FAMILY, weight, style, true);
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) return s;
        int end = s.lastIndexOf(' ', max - 4);
        if (end < 0) end = max - 4;
        return s.substring(0, end) + "...";
    }

    private static List<String> truncateList(List<String> list, int max) {
        if (list == null) return List.of();
        return list.subList(0, Math.min(list.size(), max));
    }

    private static final String CSS = """
        @page { size: A4; margin: 0; }
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
          font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
          color: #1E293B;
          background: #FFFFFF;
        }
        .page { padding: 0 0 30px 0; }

        /* Header — plain white, thin bottom divider, no heavy dark band */
        .header-band { background: #FFFFFF; padding: 20px 44px 16px 44px; border-bottom: 1px solid #E2E8F0; }
        .header-table { width: 100%; border-collapse: collapse; }
        .header-title-cell { vertical-align: middle; width: 38%; }
        .header-title-cell h1 { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size: 21px; font-weight: 700; color: #1E293B; letter-spacing: -0.3px; }
        .header-badge-cell { vertical-align: middle; width: 27%; text-align: center; }
        .header-info-cell { vertical-align: middle; width: 35%; text-align: right; }
        .tier-badge { display: inline-block; white-space: nowrap; font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size: 12px; font-weight: 700; letter-spacing: 1.5px; padding: 10px 26px; border-radius: 22px; }
        .header-name { font-size: 13px; font-weight: 700; color: #1E293B; }
        .header-meta { font-size: 10.5px; font-weight: 500; color: #64748B; margin-top: 2px; }

        /* Summary card — subtle border + soft lift shadow */
        .summary-card { background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 12px; padding: 16px 22px; margin: 20px 44px 0 44px; }
        .summary { font-size: 12.5px; line-height: 1.6; color: #1E293B; }

        /* Section headings — cobalt accent, sharp letter-spacing */
        .section { margin: 24px 44px 8px 44px; }
        .section-title { font-size: 11px; font-weight: 700; color: #2563EB; text-transform: uppercase; letter-spacing: 1.5px; margin-bottom: 6px; }
        .section-underline { width: 48px; height: 3px; border-radius: 2px; background: #2563EB; }

        /* Requirement table — light thin borders, dashboard feel */
        .table-card { background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 12px; margin: 0 44px; overflow: hidden; }
        .req-table { width: 100%; border-collapse: collapse; }
        .req-table th { text-align: left; font-size: 9.5px; font-weight: 700; color: #64748B; text-transform: uppercase; letter-spacing: 1px; padding: 10px 16px; background: #F8FAFC; border-bottom: 1px solid #F1F5F9; }
        .th-status { width: 15%; }
        .th-req { width: 30%; }
        .th-evidence { width: 55%; }
        .req-table td { padding: 11px 16px; font-size: 11.5px; vertical-align: top; line-height: 1.45; border-bottom: 1px solid #F1F5F9; }
        .req-table tr:last-child td { border-bottom: none; }
        .row-odd { background: #FAFBFC; }
        .td-req { font-weight: 500; color: #1E293B; }
        .td-evidence { font-weight: 400; color: #475569; }
        .td-evidence.muted { color: #94A3B8; }
        .chip { display: inline-block; font-size: 10px; font-weight: 700; padding: 4px 10px 4px 8px; border-radius: 11px; white-space: nowrap; }
        .dot { display: inline-block; width: 6px; height: 6px; border-radius: 3px; margin-right: 6px; }

        /* Matched skills — crisp slate chips, no border */
        .skills-card { background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 12px; margin: 0 44px; padding: 14px 18px; }
        .skill { display: inline-block; font-size: 12px; font-weight: 500; color: #1E293B; background: #F1F5F9; border-radius: 14px; padding: 5px 14px; margin: 0 6px 6px 0; }
        """;
}
