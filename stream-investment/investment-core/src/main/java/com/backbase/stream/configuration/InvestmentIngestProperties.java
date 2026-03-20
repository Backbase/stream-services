package com.backbase.stream.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Fine-grained configuration properties for {@code InvestmentPortfolioService}.
 *
 * <p>Properties are grouped by the model they govern so operators can reason about each
 * concern independently without touching unrelated settings.
 *
 * <p>All values can be overridden via {@code application.yml} / {@code application.properties}
 * using the prefix {@code backbase.bootstrap.ingestions.investment.service}.
 *
 * <p>Ingestion-flow feature flags ({@code contentEnabled}, {@code assetUniverseEnabled}, etc.)
 * live in the sibling class {@link InvestmentIngestionConfigurationProperties}.
 *
 * <p>Example:
 * <pre>
 * backbase:
 *   bootstrap:
 *     ingestions:
 *       investment:
 *         service:
 *           portfolio:
 *             default-currency: USD
 *             activation-past-months: 3
 *           allocation:
 *             model-portfolio-allocation-asset: "model_portfolio.allocation.asset"
 *           deposit:
 *             provider: real-bank
 *             default-amount: 25000.0
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "backbase.bootstrap.ingestions.investment.service")
public class InvestmentIngestProperties {

    private PortfolioConfig portfolio = new PortfolioConfig();
    private AllocationConfig allocation = new AllocationConfig();
    private DepositConfig deposit = new DepositConfig();

    // -------------------------------------------------------------------------
    // Portfolio
    // -------------------------------------------------------------------------

    /**
     * Settings that govern how individual investment portfolios are created and activated.
     */
    @Data
    public static class PortfolioConfig {

        /**
         * ISO 4217 currency code applied to new portfolios when none is specified in the source data.
         */
        private String defaultCurrency = "EUR";

        /**
         * How many months into the past the portfolio's {@code activated} timestamp is set.
         * A value of {@code 1} means the portfolio is considered to have been activated 1 month ago.
         */
        private int activationPastMonths = 1;
    }

    // -------------------------------------------------------------------------
    // Allocation
    // -------------------------------------------------------------------------

    /**
     * Settings that govern portfolio-product allocation behaviour.
     */
    @Data
    public static class AllocationConfig {

        /**
         * The allocation-asset expansion key sent when listing or managing portfolio products.
         * Changing this value allows switching between different allocation-asset model definitions
         * without a code change.
         */
        private String modelPortfolioAllocationAsset = "model_portfolio.allocation.asset";
    }

    // -------------------------------------------------------------------------
    // Deposit
    // -------------------------------------------------------------------------

    /**
     * Settings that govern the automatic seed deposit created for new portfolios.
     */
    @Data
    public static class DepositConfig {

        /**
         * The payment provider identifier sent with every deposit request.
         * Set this to the real provider name for non-mock environments.
         */
        private String provider = null;

        /**
         * The monetary amount used as the initial seed deposit when no previous deposit exists
         * or when the current deposited total is below this threshold.
         */
        private double defaultAmount = 10_000d;
    }
}

