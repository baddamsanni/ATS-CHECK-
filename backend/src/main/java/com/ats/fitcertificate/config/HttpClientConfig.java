package com.ats.fitcertificate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * HTTP client configuration.
 *
 * <p>Provides a dedicated {@link RestClient} bean for the
 * {@code ClaudeCertificateNarrator} (Anthropic base URL). The
 * {@code MergeGatewayNarrator} builds its own client from the
 * auto-configured {@link RestClient.Builder} with the Merge Gateway base URL.</p>
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestClient anthropicRestClient(RestTemplateBuilder builder,
                                          @Value("${anthropic.base-url:https://api.anthropic.com}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
