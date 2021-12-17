package com.backbase.stream.compositions.legalentity.core.config;

import com.backbase.stream.compositions.integration.legalentity.ApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.text.DateFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class LegalEntityConfigurationTest {
    @Mock
    ApiClient apiClient;

    @Mock
    WebClient webClient;

    @Mock
    ObjectMapper objectMapper;

    @Mock
    DateFormat dateFormat;

    @Test
    void test() {
        LegalEntityConfigurationProperties properties = new LegalEntityConfigurationProperties();
        properties.setLegalEntityIntegrationUrl("http://legal-entity");

        LegalEntityConfiguration configuration = new LegalEntityConfiguration(properties);
        assertNotNull(configuration.legalEntityIntegrationApi(apiClient));

        ApiClient apiClient = configuration.legalEntityClient(webClient, objectMapper, dateFormat);
        assertEquals("http://legal-entity", apiClient.getBasePath());
    }
}
