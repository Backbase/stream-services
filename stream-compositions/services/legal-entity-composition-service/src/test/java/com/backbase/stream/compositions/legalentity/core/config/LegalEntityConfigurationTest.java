package com.backbase.stream.compositions.legalentity.core.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class LegalEntityConfigurationTest {
    @Test
    void test() {
        LegalEntityConfigurationProperties properties = new LegalEntityConfigurationProperties();
        properties.setIntegrationBaseUrl("http://legal-entity");
        assertTrue(properties.getIntegrationBaseUrl().contains("legal-entity"), "Correct config spotted");
    }
}
