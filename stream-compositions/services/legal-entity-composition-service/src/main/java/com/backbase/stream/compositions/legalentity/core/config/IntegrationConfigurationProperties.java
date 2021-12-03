package com.backbase.stream.compositions.legalentity.core.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@NoArgsConstructor
@ConfigurationProperties("backbase.stream.compositions.integration")
public class IntegrationConfigurationProperties {
    /**
     * Location of legal-entity-integration service.
     */
    private String legalEntityIntegrationUrl = "http://legal-entity-ingestion-integration:8080";
}
