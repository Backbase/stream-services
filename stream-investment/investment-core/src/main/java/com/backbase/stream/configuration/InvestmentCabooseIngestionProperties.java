package com.backbase.stream.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Feature-flag properties for investment-caboose ingestion.
 *
 * <p>Controls whether the Bootstrap Job writes investment data to investment-caboose and which
 * stages are active for the caboose path. All flags are independent of their primary-service
 * counterparts in {@link InvestmentIngestionConfigurationProperties} — disabling a stage on
 * the primary path does <strong>not</strong> automatically disable it on the caboose path.
 *
 * <p>Example:
 * <pre>
 * backbase:
 *   bootstrap:
 *     ingestions:
 *       investment:
 *         caboose:
 *           enabled: true
 *           asset-universe-enabled: true
 *           wealth-enabled: true
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "backbase.bootstrap.ingestions.investment.caboose")
public class InvestmentCabooseIngestionProperties {

    /**
     * When {@code true}, investment data is also written to investment-caboose.
     * If caboose is unavailable a warning is logged but the bootstrap job does not fail.
     *
     * <p>Default: {@code false}.
     */
    private boolean enabled = false;

    /**
     * Controls whether the caboose asset-universe stage (currencies, markets, assets, prices)
     * is executed. Independent of the primary {@code assetUniverseEnabled} flag.
     *
     * <p>Default: {@code true}.
     */
    private boolean assetUniverseEnabled = true;

    /**
     * Controls whether the caboose wealth / portfolio stage (clients, portfolios, trading accounts)
     * is executed. Independent of the primary {@code wealthEnabled} flag.
     *
     * <p>Default: {@code true}.
     */
    private boolean wealthEnabled = true;
}
