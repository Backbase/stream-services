package com.backbase.stream.compositions.legalentity.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.text.DateFormat;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class LegalEntityConfigurationTest {
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
        assertTrue(properties.getLegalEntityIntegrationUrl().contains("legal-entity"), "Correct config spotted");
    }
}
