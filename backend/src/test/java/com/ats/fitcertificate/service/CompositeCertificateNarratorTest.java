package com.ats.fitcertificate.service;

import com.ats.fitcertificate.dto.RequirementMatch;
import com.ats.fitcertificate.dto.RequirementMatchResult;
import com.ats.fitcertificate.exception.UpstreamException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CompositeCertificateNarratorTest {

    private final SkillDictionary dict = new SkillDictionary();
    private final HeuristicCertificateNarrator heuristic = new HeuristicCertificateNarrator(dict);

    private MergeGatewayNarrator unconfiguredGateway() {
        return new MergeGatewayNarrator(org.springframework.web.client.RestClient.builder(), "",
                "url", "google/gemini-3-flash-preview", 100);
    }

    @Test
    void usesHeuristicWhenNothingConfigured() {
        MergeGatewayNarrator gateway = unconfiguredGateway();
        ClaudeCertificateNarrator noKey = new ClaudeCertificateNarrator(
                org.springframework.web.client.RestClient.builder().build(), "", "m", 100);
        CompositeCertificateNarrator composite = new CompositeCertificateNarrator(gateway, noKey, heuristic);
        assertEquals("heuristic", composite.activeBackend());
        RequirementMatchResult r = composite.narrate("Java resume", "Java JD");
        assertNotNull(r);
        assertNotNull(r.getTier());
    }

    @Test
    void usesMergeGatewayWhenConfigured() {
        MergeGatewayNarrator gateway = mock(MergeGatewayNarrator.class);
        when(gateway.isConfigured()).thenReturn(true);
        when(gateway.narrate(anyString(), anyString(), any())).thenReturn(v3Result("Excellent Fit"));
        ClaudeCertificateNarrator claude = mock(ClaudeCertificateNarrator.class);
        CompositeCertificateNarrator composite = new CompositeCertificateNarrator(gateway, claude, heuristic);
        assertTrue(composite.isUsingMergeGateway());
        RequirementMatchResult r = composite.narrate("r", "j", "google/gemini-3-flash-preview");
        assertEquals("Excellent Fit", r.getTier());
        verify(gateway, times(1)).narrate(anyString(), anyString(), any());
        verify(claude, never()).narrate(anyString(), anyString());
    }

    @Test
    void fallsBackToClaudeWhenGatewayFails() {
        MergeGatewayNarrator gateway = mock(MergeGatewayNarrator.class);
        when(gateway.isConfigured()).thenReturn(true);
        when(gateway.narrate(anyString(), anyString(), any())).thenThrow(new UpstreamException("timeout"));
        ClaudeCertificateNarrator claude = mock(ClaudeCertificateNarrator.class);
        when(claude.isConfigured()).thenReturn(true);
        when(claude.narrate(anyString(), anyString())).thenReturn(v3Result("Strong Fit"));
        CompositeCertificateNarrator composite = new CompositeCertificateNarrator(gateway, claude, heuristic);
        RequirementMatchResult r = composite.narrate("r", "j", null);
        assertEquals("Strong Fit", r.getTier());
        verify(claude, times(1)).narrate(anyString(), anyString());
    }

    @Test
    void fallsBackToHeuristicWhenAllFail() {
        MergeGatewayNarrator gateway = mock(MergeGatewayNarrator.class);
        when(gateway.isConfigured()).thenReturn(true);
        when(gateway.narrate(anyString(), anyString(), any())).thenThrow(new UpstreamException("down"));
        ClaudeCertificateNarrator claude = mock(ClaudeCertificateNarrator.class);
        when(claude.isConfigured()).thenReturn(true);
        when(claude.narrate(anyString(), anyString())).thenThrow(new UpstreamException("down"));
        CompositeCertificateNarrator composite = new CompositeCertificateNarrator(gateway, claude, heuristic);
        RequirementMatchResult r = composite.narrate("Java resume", "Java JD");
        assertNotNull(r);
        assertNotNull(r.getTier());
    }

    @Test
    void usesClaudeWhenGatewayNotConfigured() {
        MergeGatewayNarrator gateway = unconfiguredGateway();
        ClaudeCertificateNarrator claude = mock(ClaudeCertificateNarrator.class);
        when(claude.isConfigured()).thenReturn(true);
        when(claude.narrate(anyString(), anyString())).thenReturn(v3Result("Good Fit"));
        CompositeCertificateNarrator composite = new CompositeCertificateNarrator(gateway, claude, heuristic);
        assertEquals("claude", composite.activeBackend());
        RequirementMatchResult r = composite.narrate("r", "j", null);
        assertEquals("Good Fit", r.getTier());
    }

    @Test
    void passesModelOverrideToGateway() {
        MergeGatewayNarrator gateway = mock(MergeGatewayNarrator.class);
        when(gateway.isConfigured()).thenReturn(true);
        when(gateway.narrate(anyString(), anyString(), eq("minimax/minimax-m2.7"))).thenReturn(v3Result("Weak Fit"));
        ClaudeCertificateNarrator claude = mock(ClaudeCertificateNarrator.class);
        CompositeCertificateNarrator composite = new CompositeCertificateNarrator(gateway, claude, heuristic);
        RequirementMatchResult r = composite.narrate("r", "j", "minimax/minimax-m2.7");
        assertEquals("Weak Fit", r.getTier());
        verify(gateway).narrate(anyString(), anyString(), eq("minimax/minimax-m2.7"));
    }

    private RequirementMatchResult v3Result(String tier) {
        RequirementMatchResult r = new RequirementMatchResult();
        r.setCandidateName("Test");
        r.setRoleTitle("Engineer");
        r.setTier(tier);
        r.setSummary("Test summary.");
        r.setRequirements(List.of(new RequirementMatch("Java", "met", "proof")));
        r.setMatchedSkills(List.of("Java"));
        return r;
    }
}
