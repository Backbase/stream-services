package com.backbase.stream.compositions.legalentity.core.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@NoArgsConstructor
@ConfigurationProperties("backbase.stream.compositions.product")
public class ProductConfigurationProperties {
    private Boolean enableCompletedEvents = true;
    private Boolean enableFailedEvents = true;
    private String productIntegrationUrl = "http://product-ingestion-integration:8080";
}
