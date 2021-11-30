package com.backbase.stream.compositions.legalentity.configuration;

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
    private String legalEntityIngestionUrl = "http://legal-entity-ingestion-integration:8080";

    /**
     * Location of product-integration service.
     */
    private String productIngestionUrl = "http://product-ingestion-integration:8080";

    /**
     * Location of transaction-integration service.
     */
    private String transactionIngestionUrl = "http://transaction-ingestion-integration:8080";
}
