
package com.backbase.stream.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties governing investment client ingestion behavior.
 */
@Data
@ConfigurationProperties(prefix = "backbase.bootstrap.ingestions.investment")
public class InvestmentIngestionConfigurationProperties {

    private boolean assetUniversEnabled = true;
    private boolean wealthEnabled = true;


}

