
package com.backbase.stream.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties governing investment client ingestion behavior.
 */
@Data
@ConfigurationProperties(prefix = "backbase.bootstrap.ingestions.investment")
public class InvestmentIngestionConfigurationProperties {

    private boolean contentEnabled = true;
    private boolean assetUniverseEnabled = true;
    private boolean wealthEnabled = true;
    private int portfolioActivationPastMonths = 1;


}

