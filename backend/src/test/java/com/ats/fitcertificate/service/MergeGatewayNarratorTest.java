package com.ats.fitcertificate.service;

import com.ats.fitcertificate.dto.RequirementMatchResult;
import com.ats.fitcertificate.exception.UpstreamException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

class MergeGatewayNarratorTest {

    private final MergeGatewayNarrator narrator = new MergeGatewayNarrator(
            RestClient.builder().baseUrl("http://localhost:1"),
            "fake-key", "https://api-gateway.merge.dev/v1/openai",
            "google/gemini-3-flash-preview", 1500);

    @Test
    void isConfiguredTrueWhenKeyPresent() { assertTrue(narrator.isConfigured()); }

    @Test
    void isConfiguredFalseWhenKeyBlank() {
        assertFalse(new MergeGatewayNarrator(RestClient.builder(), "", "u", "m", 1).isConfigured());
    }

    @Test
    void narrateThrowsWhenKeyMissing() {
        assertThrows(UpstreamException.class, () ->
                new MergeGatewayNarrator(RestClient.builder(), "", "u", "m", 1).narrate("r", "j"));
    }

    @Test
    void parsesV3Schema() {
        String response = """
                {"choices":[{"message":{"content":"{\\"candidateName\\":\\"Jane Doe\\",\\"roleTitle\\":\\"Backend Engineer\\",\\"tier\\":\\"Excellent Fit\\",\\"summary\\":\\"Jane is highly qualified.\\",\\"requirements\\":[{\\"requirement\\":\\"Java\\",\\"status\\":\\"met\\",\\"evidence\\":\\"5 years\\"}],\\"matchedSkills\\":[\\"Java\\",\\"Spring\\"]}"}}]}
                """;
        RequirementMatchResult r = narrator.parse(response);
        assertNotNull(r);
        assertEquals("Jane Doe", r.getCandidateName());
        assertEquals("Backend Engineer", r.getRoleTitle());
        assertEquals("Excellent Fit", r.getTier());
        assertEquals("Jane is highly qualified.", r.getSummary());
        assertEquals(1, r.getRequirements().size());
        assertEquals(2, r.getMatchedSkills().size());
        assertEquals("Java", r.getMatchedSkills().get(0));
    }

    @Test
    void parsesFencedJson() {
        String response = """
                {"choices":[{"message":{"content":"```json\\n{\\"candidateName\\":\\"A\\",\\"roleTitle\\":\\"B\\",\\"tier\\":\\"Good Fit\\",\\"summary\\":\\"s\\",\\"requirements\\":[{\\"requirement\\":\\"x\\",\\"status\\":\\"met\\",\\"evidence\\":\\"y\\"}],\\"matchedSkills\\":[\\"z\\"]}\\n```"}}]}
                """;
        RequirementMatchResult r = narrator.parse(response);
        assertNotNull(r);
        assertEquals("Good Fit", r.getTier());
    }

    @Test
    void returnsNullForMalformed() {
        assertNull(narrator.parse("not json"));
        assertNull(narrator.parse(""));
        assertNull(narrator.parse(null));
    }

    @Test
    void returnsNullForMissingFields() {
        assertNull(narrator.parse("{\"choices\":[{\"message\":{\"content\":\"{\\\"foo\\\":1}\"}}]}"));
    }

    @Test
    void supportedModelsHasFour() {
        assertEquals(4, MergeGatewayNarrator.SUPPORTED_MODELS.size());
    }
}
