
package com.backbase.stream.configuration;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties governing investment client ingestion behavior.
 *
 * <p>Controls which high-level ingestion flows are enabled. Service-level tuning
 * (portfolio currencies, deposit provider, allocation assets, etc.) lives in
 * {@link InvestmentIngestProperties}.
 */
@Data
@ConditionalOnBean(InvestmentServiceConfiguration.class)
@ConfigurationProperties(prefix = "backbase.bootstrap.ingestions.investment")
public class InvestmentIngestionConfigurationProperties {

    private boolean contentEnabled = true;
    private boolean assetUniverseEnabled = true;
    private boolean wealthEnabled = true;


}

