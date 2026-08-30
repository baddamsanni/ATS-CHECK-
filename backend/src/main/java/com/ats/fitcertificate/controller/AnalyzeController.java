package com.ats.fitcertificate.controller;

import com.ats.fitcertificate.dto.AnalyzeResponse;
import com.ats.fitcertificate.exception.BadRequestException;
import com.ats.fitcertificate.service.AnalyzeService;
import com.ats.fitcertificate.service.MergeGatewayNarrator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AnalyzeController {

    private static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
    private static final int MAX_JD_CHARS = 20000;

    private final AnalyzeService analyzeService;
    private final MergeGatewayNarrator mergeGatewayNarrator;

    public AnalyzeController(AnalyzeService analyzeService,
                             MergeGatewayNarrator mergeGatewayNarrator) {
        this.analyzeService = analyzeService;
        this.mergeGatewayNarrator = mergeGatewayNarrator;
    }

    @PostMapping(value = "/analyze", consumes = "multipart/form-data", produces = "application/json")
    public AnalyzeResponse analyze(
            @RequestParam("resume") MultipartFile resume,
            @RequestParam("jobDescription") String jobDescription,
            @RequestParam(value = "model", required = false) String model) throws IOException {

        if (resume == null || resume.isEmpty()) {
            throw new BadRequestException("A resume file is required.");
        }
        if (resume.getSize() > MAX_FILE_BYTES) {
            throw new BadRequestException("Resume file exceeds the 5MB limit.");
        }
        if (jobDescription == null || jobDescription.isBlank()) {
            throw new BadRequestException("A job description is required.");
        }
        if (jobDescription.length() > MAX_JD_CHARS) {
            throw new BadRequestException("Job description exceeds the 20000 character limit.");
        }

        return analyzeService.analyze(
                new BufferedInputStream(resume.getInputStream()),
                resume.getOriginalFilename(),
                jobDescription.trim(),
                model
        );
    }

    /**
     * Returns the list of LLM models the frontend can offer in its model
     * selector, plus the active backend and default model.
     */
    @GetMapping(value = "/models", produces = "application/json")
    public Map<String, Object> models() {
        List<Map<String, String>> models = MergeGatewayNarrator.SUPPORTED_MODELS.stream()
                .map(m -> Map.of(
                        "id", m.id(),
                        "displayName", m.displayName(),
                        "description", m.description()))
                .toList();
        return Map.of(
                "models", models,
                "defaultModel", mergeGatewayNarrator.getDefaultModel(),
                "gatewayConfigured", String.valueOf(mergeGatewayNarrator.isConfigured())
        );
    }
}
