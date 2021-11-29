package com.backbase.stream.compositions.legalentity.core;

import com.backbase.compositions.integration.legalentity.api.service.ApiClient;
import com.backbase.compositions.integration.legalentity.api.service.v1.LegalEntityIntegrationApi;
import com.backbase.stream.compositions.configuration.IntegrationConfigurationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.text.DateFormat;

@Configuration
@AllArgsConstructor
@EnableConfigurationProperties(IntegrationConfigurationProperties.class)
public class LegalEntityIntegrationConfiguration {
    private final IntegrationConfigurationProperties integrationConfigurationProperties;

    @Bean
    public LegalEntityIntegrationApi arrangementService(ApiClient legalEntityClient) {
        return new LegalEntityIntegrationApi(legalEntityClient);
    }

    @Bean
    public ApiClient legalEntityClient(
            WebClient dbsWebClient,
            ObjectMapper objectMapper,
            DateFormat dateFormat) {
        ApiClient apiClient = new ApiClient(dbsWebClient, objectMapper, dateFormat);
        apiClient.setBasePath(integrationConfigurationProperties.getLegalEntityIngestionUrl());

        return apiClient;
    }
}
