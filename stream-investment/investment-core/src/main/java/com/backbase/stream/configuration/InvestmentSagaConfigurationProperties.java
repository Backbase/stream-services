package com.backbase.stream.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties governing investment client ingestion behavior.
 */
@Data
@ConfigurationProperties(prefix = "stream.investment.ingestion")
public class InvestmentSagaConfigurationProperties {

    /**
     * If true, saga will attempt a GET lookup before creating a client (extra roundtrip) to achieve idempotent behavior.
     */
    private boolean preExistenceCheck = true;
}

