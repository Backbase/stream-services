package com.backbase.stream.compositions.legalentity.core.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@NoArgsConstructor
@ConfigurationProperties("backbase.stream.compositions.legal-entity")
public class LegalEntityConfigurationProperties {
    private Boolean enableCompletedEvents;
    private Boolean enableFailedEvents;
    private String legalEntityIntegrationUrl = "http://legal-entity-ingestion-integration:8080";
}
