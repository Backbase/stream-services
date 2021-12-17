package com.backbase.stream.compositions.legalentity.core.config;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@NoArgsConstructor
@ConfigurationProperties("backbase.stream.compositions.legal-entity")
public class LegalEntityConfigurationProperties {
    private Boolean enableCompletedEvents = true;
    private Boolean enableFailedEvents = true;
    private String legalEntityIntegrationUrl = "http://legal-entity-ingestion-integration:8080";
}
