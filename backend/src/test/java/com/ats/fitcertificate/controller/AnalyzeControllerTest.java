package com.ats.fitcertificate.controller;

import com.ats.fitcertificate.dto.AnalyzeResponse;
import com.ats.fitcertificate.dto.RequirementMatch;
import com.ats.fitcertificate.exception.BadRequestException;
import com.ats.fitcertificate.exception.UnsupportedFileTypeException;
import com.ats.fitcertificate.exception.UpstreamException;
import com.ats.fitcertificate.service.AnalyzeService;
import com.ats.fitcertificate.service.MergeGatewayNarrator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalyzeController.class)
class AnalyzeControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AnalyzeService analyzeService;
    @MockBean private MergeGatewayNarrator mergeGatewayNarrator;

    @Test
    void returnsOkWithV3Json() throws Exception {
        when(analyzeService.analyze(any(), anyString(), anyString(), any())).thenReturn(buildResp("Excellent Fit"));
        when(mergeGatewayNarrator.getDefaultModel()).thenReturn("google/gemini-3-flash-preview");

        MockMultipartFile resume = new MockMultipartFile("resume", "r.pdf", "application/pdf", "%PDF-1.7".getBytes());
        mockMvc.perform(multipart("/api/analyze").file(resume).param("jobDescription", "Java JD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateName").value("Jane Doe"))
                .andExpect(jsonPath("$.roleTitle").value("Backend Engineer"))
                .andExpect(jsonPath("$.tier").value("Excellent Fit"))
                .andExpect(jsonPath("$.summary").exists())
                .andExpect(jsonPath("$.requirements[0].requirement").value("Java"))
                .andExpect(jsonPath("$.requirements[0].status").value("met"))
                .andExpect(jsonPath("$.matchedSkills[0]").value("Java"))
                .andExpect(jsonPath("$.certificateBase64").exists())
                .andExpect(jsonPath("$.verificationId").exists());
    }

    @Test
    void acceptsModelParam() throws Exception {
        when(analyzeService.analyze(any(), anyString(), anyString(), anyString())).thenReturn(buildResp("Strong Fit"));
        MockMultipartFile resume = new MockMultipartFile("resume", "r.pdf", "application/pdf", "%PDF-1.7".getBytes());
        mockMvc.perform(multipart("/api/analyze").file(resume).param("jobDescription", "JD").param("model", "minimax/minimax-m2.7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tier").value("Strong Fit"));
    }

    @Test
    void returns400WhenResumeMissing() throws Exception {
        mockMvc.perform(multipart("/api/analyze").param("jobDescription", "JD"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists());
    }

    @Test
    void returns400WhenJdMissing() throws Exception {
        MockMultipartFile resume = new MockMultipartFile("resume", "r.pdf", "application/pdf", "%PDF-1.7".getBytes());
        mockMvc.perform(multipart("/api/analyze").file(resume))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists());
    }

    @Test
    void returns400WhenJdBlank() throws Exception {
        MockMultipartFile resume = new MockMultipartFile("resume", "r.pdf", "application/pdf", "%PDF-1.7".getBytes());
        mockMvc.perform(multipart("/api/analyze").file(resume).param("jobDescription", "   "))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("A job description is required."));
    }

    @Test
    void returns400WhenJdTooLong() throws Exception {
        MockMultipartFile resume = new MockMultipartFile("resume", "r.pdf", "application/pdf", "%PDF-1.7".getBytes());
        mockMvc.perform(multipart("/api/analyze").file(resume).param("jobDescription", "a".repeat(20001)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists());
    }

    @Test
    void returns400OnBadRequest() throws Exception {
        when(analyzeService.analyze(any(), anyString(), anyString(), any())).thenThrow(new BadRequestException("bad"));
        MockMultipartFile resume = new MockMultipartFile("resume", "r.pdf", "application/pdf", "%PDF-1.7".getBytes());
        mockMvc.perform(multipart("/api/analyze").file(resume).param("jobDescription", "JD"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("bad"));
    }

    @Test
    void returns400OnUnsupportedFileType() throws Exception {
        when(analyzeService.analyze(any(), anyString(), anyString(), any())).thenThrow(new UnsupportedFileTypeException("bad type"));
        MockMultipartFile resume = new MockMultipartFile("resume", "r.txt", "text/plain", "text".getBytes());
        mockMvc.perform(multipart("/api/analyze").file(resume).param("jobDescription", "JD"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("bad type"));
    }

    @Test
    void returns502OnUpstreamError() throws Exception {
        when(analyzeService.analyze(any(), anyString(), anyString(), any())).thenThrow(new UpstreamException("down"));
        MockMultipartFile resume = new MockMultipartFile("resume", "r.pdf", "application/pdf", "%PDF-1.7".getBytes());
        mockMvc.perform(multipart("/api/analyze").file(resume).param("jobDescription", "JD"))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.error").value("down"));
    }

    @Test
    void returns500OnUnexpected() throws Exception {
        when(analyzeService.analyze(any(), anyString(), anyString(), any())).thenThrow(new RuntimeException("boom"));
        MockMultipartFile resume = new MockMultipartFile("resume", "r.pdf", "application/pdf", "%PDF-1.7".getBytes());
        mockMvc.perform(multipart("/api/analyze").file(resume).param("jobDescription", "JD"))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.error").exists());
    }

    @Test
    void modelsEndpointReturnsList() throws Exception {
        when(mergeGatewayNarrator.getDefaultModel()).thenReturn("google/gemini-3-flash-preview");
        when(mergeGatewayNarrator.isConfigured()).thenReturn(true);
        mockMvc.perform(get("/api/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.models").isArray())
                .andExpect(jsonPath("$.defaultModel").value("google/gemini-3-flash-preview"))
                .andExpect(jsonPath("$.gatewayConfigured").value("true"));
    }

    private AnalyzeResponse buildResp(String tier) {
        return new AnalyzeResponse("Jane Doe", "Backend Engineer", tier,
                "Jane is highly qualified.", List.of(new RequirementMatch("Java", "met", "5y")),
                List.of("Java", "Spring Boot"), Base64.getEncoder().encodeToString("%PDF".getBytes()),
                "FC-ABC123");
    }
}
