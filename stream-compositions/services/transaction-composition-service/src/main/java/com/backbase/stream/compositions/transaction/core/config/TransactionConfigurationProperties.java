package com.backbase.stream.compositions.transaction.core.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@NoArgsConstructor
@ConfigurationProperties("backbase.stream.compositions.transaction")
public class TransactionConfigurationProperties {
    private Boolean enableCompletedEvents = true;
    private Boolean enableFailedEvents = true;
    private String transactionIntegrationUrl = "http://transaction-ingestion-integration:8080";
}
