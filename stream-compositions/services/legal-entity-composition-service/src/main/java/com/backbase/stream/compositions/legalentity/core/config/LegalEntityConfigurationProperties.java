package com.backbase.stream.compositions.legalentity.core.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@NoArgsConstructor
@ConfigurationProperties("backbase.stream.compositions.legal-entity")
public class LegalEntityConfigurationProperties {
    private Boolean enableCompletedEvent = false;
    private Boolean enableFailedEvent = false;
    private Boolean chainProductEvent = false;
    private String legalEntityIntegrationUrl = "http://legal-entity-ingestion-integration:9000";
    private String productCompositionUrl = "http://localhost:8083/product-composition-service";
}
