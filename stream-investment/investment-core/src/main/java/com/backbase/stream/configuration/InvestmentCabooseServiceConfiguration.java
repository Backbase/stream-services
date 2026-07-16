package com.backbase.stream.configuration;

import com.backbase.investment.api.service.v1.AllocationsApi;
import com.backbase.investment.api.service.v1.AssetUniverseApi;
import com.backbase.investment.api.service.v1.AsyncBulkGroupsApi;
import com.backbase.investment.api.service.v1.ClientApi;
import com.backbase.investment.api.service.v1.CurrencyApi;
import com.backbase.investment.api.service.v1.FinancialAdviceApi;
import com.backbase.investment.api.service.v1.InvestmentApi;
import com.backbase.investment.api.service.v1.InvestmentProductsApi;
import com.backbase.investment.api.service.v1.PaymentsApi;
import com.backbase.investment.api.service.v1.PortfolioApi;
import com.backbase.investment.api.service.v1.PortfolioTradingAccountsApi;
import com.backbase.investment.api.service.v1.RiskAssessmentApi;
import com.backbase.stream.clients.autoconfigure.DbsApiClientsAutoConfiguration;
import com.backbase.stream.investment.saga.InvestmentAssetUniverseSaga;
import com.backbase.stream.investment.saga.InvestmentSaga;
import com.backbase.stream.investment.service.AsyncTaskService;
import com.backbase.stream.investment.service.InvestmentAssetPriceService;
import com.backbase.stream.investment.service.InvestmentAssetUniverseService;
import com.backbase.stream.investment.service.InvestmentClientService;
import com.backbase.stream.investment.service.InvestmentCurrencyService;
import com.backbase.stream.investment.service.InvestmentIntradayAssetPriceService;
import com.backbase.stream.investment.service.InvestmentModelPortfolioService;
import com.backbase.stream.investment.service.InvestmentPortfolioAllocationService;
import com.backbase.stream.investment.service.InvestmentPortfolioProductService;
import com.backbase.stream.investment.service.InvestmentPortfolioService;
import com.backbase.stream.investment.service.InvestmentRiskAssessmentService;
import com.backbase.stream.investment.service.InvestmentRiskQuestionaryService;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestAssetUniverseService;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestModelPortfolioService;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestProductPortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Spring configuration for investment-caboose ingestion — mirrors {@link InvestmentServiceConfiguration}
 * in structure but targets investment-caboose via a dedicated API client.
 *
 * <p>Active only when {@code backbase.bootstrap.ingestions.investment.caboose.enabled=true}.
 *
 * <p>Can operate as a <strong>standalone path</strong> independently of
 * {@link InvestmentServiceConfiguration}. When both are active the BJS dual-writes asset-universe and
 * portfolio data to the primary Investment Service and investment-caboose. Content ingestion remains
 * primary-only.
 *
 * <p>All beans are prefixed with {@code caboose} so they coexist alongside primary beans
 * in dual-write mode without ambiguity.
 *
 * <p>Sync REST-template services are provided by {@link InvestmentCabooseRestServiceApiConfiguration}.
 */
@RequiredArgsConstructor
@Configuration
@Import({
    DbsApiClientsAutoConfiguration.class,
    InvestmentCabooseClientConfig.class,
    InvestmentCabooseWebClientConfiguration.class,
    InvestmentCabooseRestServiceApiConfiguration.class
})
@ConditionalOnProperty(
    name = "backbase.bootstrap.ingestions.investment.caboose.enabled",
    havingValue = "true"
)
@EnableConfigurationProperties({
    InvestmentCabooseIngestionProperties.class,
    InvestmentIngestionConfigurationProperties.class,
    IngestConfigProperties.class
})
public class InvestmentCabooseServiceConfiguration {


    @Bean("cabooseInvestmentClientService")
    public InvestmentClientService cabooseInvestmentClientService(
        @Qualifier("cabooseClientApi") ClientApi cabooseClientApi) {
        return new InvestmentClientService(cabooseClientApi);
    }

    @Bean("cabooseInvestmentModelPortfolioService")
    public InvestmentModelPortfolioService cabooseInvestmentModelPortfolioService(
        @Qualifier("cabooseFinancialAdviceApi") FinancialAdviceApi cabooseFinancialAdviceApi,
        @Qualifier("cabooseInvestmentRestModelPortfolioService")
        InvestmentRestModelPortfolioService cabooseInvestmentRestModelPortfolioService,
        IngestConfigProperties portfolioProperties) {
        return new InvestmentModelPortfolioService(cabooseFinancialAdviceApi,
            cabooseInvestmentRestModelPortfolioService, portfolioProperties);
    }

    @Bean("cabooseInvestmentPortfolioProductService")
    public InvestmentPortfolioProductService cabooseInvestmentPortfolioProductService(
        @Qualifier("cabooseInvestmentProductsApi") InvestmentProductsApi cabooseInvestmentProductsApi,
        IngestConfigProperties portfolioProperties,
        @Qualifier("cabooseInvestmentModelPortfolioService")
        InvestmentModelPortfolioService cabooseInvestmentModelPortfolioService,
        @Qualifier("cabooseInvestmentRestProductPortfolioService")
        InvestmentRestProductPortfolioService cabooseInvestmentRestProductPortfolioService) {
        return new InvestmentPortfolioProductService(cabooseInvestmentProductsApi, portfolioProperties,
            cabooseInvestmentModelPortfolioService, cabooseInvestmentRestProductPortfolioService);
    }

    @Bean("cabooseInvestmentPortfolioService")
    public InvestmentPortfolioService cabooseInvestmentPortfolioService(
        @Qualifier("caboosePortfolioApi") PortfolioApi caboosePortfolioApi,
        @Qualifier("caboosePaymentsApi") PaymentsApi caboosePaymentsApi,
        @Qualifier("caboosePortfolioTradingAccountsApi")
        PortfolioTradingAccountsApi caboosePortfolioTradingAccountsApi,
        IngestConfigProperties portfolioProperties) {
        return new InvestmentPortfolioService(caboosePortfolioApi, caboosePaymentsApi,
            caboosePortfolioTradingAccountsApi, portfolioProperties);
    }

    @Bean("cabooseInvestmentAssetUniverseService")
    public InvestmentAssetUniverseService cabooseInvestmentAssetUniverseService(
        @Qualifier("cabooseAssetUniverseApi") AssetUniverseApi cabooseAssetUniverseApi,
        @Qualifier("cabooseInvestmentRestAssetUniverseService")
        InvestmentRestAssetUniverseService cabooseInvestmentRestAssetUniverseService,
        IngestConfigProperties portfolioProperties) {
        return new InvestmentAssetUniverseService(cabooseAssetUniverseApi,
            cabooseInvestmentRestAssetUniverseService, portfolioProperties);
    }

    @Bean("cabooseAsyncTaskService")
    public AsyncTaskService cabooseAsyncTaskService(
        @Qualifier("cabooseAsyncBulkGroupsApi") AsyncBulkGroupsApi cabooseAsyncBulkGroupsApi) {
        return new AsyncTaskService(cabooseAsyncBulkGroupsApi);
    }

    @Bean("cabooseInvestmentAssetPriceService")
    public InvestmentAssetPriceService cabooseInvestmentAssetPriceService(
        @Qualifier("cabooseAssetUniverseApi") AssetUniverseApi cabooseAssetUniverseApi,
        IngestConfigProperties portfolioProperties) {
        return new InvestmentAssetPriceService(cabooseAssetUniverseApi, portfolioProperties);
    }

    @Bean("cabooseInvestmentIntradayAssetPriceService")
    public InvestmentIntradayAssetPriceService cabooseInvestmentIntradayAssetPriceService(
        @Qualifier("cabooseAssetUniverseApi") AssetUniverseApi cabooseAssetUniverseApi,
        IngestConfigProperties portfolioProperties) {
        return new InvestmentIntradayAssetPriceService(cabooseAssetUniverseApi, portfolioProperties);
    }

    @Bean("cabooseInvestmentPortfolioAllocationService")
    public InvestmentPortfolioAllocationService cabooseInvestmentPortfolioAllocationService(
        @Qualifier("cabooseAllocationsApi") AllocationsApi cabooseAllocationsApi,
        @Qualifier("cabooseAssetUniverseApi") AssetUniverseApi cabooseAssetUniverseApi,
        @Qualifier("cabooseInvestmentApi") InvestmentApi cabooseInvestmentApi,
        IngestConfigProperties portfolioProperties) {
        return new InvestmentPortfolioAllocationService(cabooseAllocationsApi, cabooseAssetUniverseApi,
            cabooseInvestmentApi, portfolioProperties);
    }

    @Bean("cabooseInvestmentCurrencyService")
    public InvestmentCurrencyService cabooseInvestmentCurrencyService(
        @Qualifier("cabooseCurrencyApi") CurrencyApi cabooseCurrencyApi) {
        return new InvestmentCurrencyService(cabooseCurrencyApi);
    }

    @Bean("cabooseInvestmentRiskAssessmentService")
    public InvestmentRiskAssessmentService cabooseInvestmentRiskAssessmentService(
        @Qualifier("cabooseRiskAssessmentApi") RiskAssessmentApi cabooseRiskAssessmentApi) {
        return new InvestmentRiskAssessmentService(cabooseRiskAssessmentApi);
    }

    @Bean("cabooseInvestmentRiskQuestionaryService")
    public InvestmentRiskQuestionaryService cabooseInvestmentRiskQuestionaryService(
        @Qualifier("cabooseRiskAssessmentApi") RiskAssessmentApi cabooseRiskAssessmentApi,
        IngestConfigProperties portfolioProperties) {
        return new InvestmentRiskQuestionaryService(cabooseRiskAssessmentApi, portfolioProperties);
    }

    @Bean("cabooseInvestmentSaga")
    public InvestmentSaga cabooseInvestmentSaga(
        @Qualifier("cabooseInvestmentClientService") InvestmentClientService cabooseInvestmentClientService,
        @Qualifier("cabooseInvestmentRiskAssessmentService")
        InvestmentRiskAssessmentService cabooseInvestmentRiskAssessmentService,
        @Qualifier("cabooseInvestmentRiskQuestionaryService")
        InvestmentRiskQuestionaryService cabooseInvestmentRiskQuestionaryService,
        @Qualifier("cabooseInvestmentPortfolioService")
        InvestmentPortfolioService cabooseInvestmentPortfolioService,
        @Qualifier("cabooseInvestmentModelPortfolioService")
        InvestmentModelPortfolioService cabooseInvestmentModelPortfolioService,
        @Qualifier("cabooseInvestmentPortfolioProductService")
        InvestmentPortfolioProductService cabooseInvestmentPortfolioProductService,
        @Qualifier("cabooseInvestmentPortfolioAllocationService")
        InvestmentPortfolioAllocationService cabooseInvestmentPortfolioAllocationService,
        @Qualifier("cabooseAsyncTaskService") AsyncTaskService cabooseAsyncTaskService,
        InvestmentCabooseIngestionProperties cabooseIngestionProperties,
        @Qualifier("cabooseAssetUniverseApi") AssetUniverseApi cabooseAssetUniverseApi) {
        return new InvestmentSaga(cabooseInvestmentClientService, cabooseInvestmentRiskAssessmentService,
            cabooseInvestmentRiskQuestionaryService, cabooseInvestmentPortfolioService,
            cabooseInvestmentPortfolioAllocationService, cabooseInvestmentModelPortfolioService,
            cabooseInvestmentPortfolioProductService, cabooseAsyncTaskService,
            buildCabooseIngestionConfigurationProperties(cabooseIngestionProperties), cabooseAssetUniverseApi);
    }

    @Bean("cabooseInvestmentAssetUniverseSaga")
    public InvestmentAssetUniverseSaga cabooseInvestmentAssetUniverseSaga(
        @Qualifier("cabooseInvestmentAssetUniverseService")
        InvestmentAssetUniverseService cabooseInvestmentAssetUniverseService,
        @Qualifier("cabooseInvestmentAssetPriceService")
        InvestmentAssetPriceService cabooseInvestmentAssetPriceService,
        @Qualifier("cabooseInvestmentIntradayAssetPriceService")
        InvestmentIntradayAssetPriceService cabooseInvestmentIntradayAssetPriceService,
        @Qualifier("cabooseInvestmentCurrencyService")
        InvestmentCurrencyService cabooseInvestmentCurrencyService,
        @Qualifier("cabooseAsyncTaskService") AsyncTaskService cabooseAsyncTaskService,
        InvestmentCabooseIngestionProperties cabooseIngestionProperties,
        IngestConfigProperties portfolioProperties) {
        return new InvestmentAssetUniverseSaga(cabooseInvestmentAssetUniverseService,
            cabooseInvestmentAssetPriceService, cabooseInvestmentIntradayAssetPriceService,
            cabooseInvestmentCurrencyService, cabooseAsyncTaskService,
            buildCabooseIngestionConfigurationProperties(cabooseIngestionProperties), portfolioProperties);
    }

    /**
     * Builds a plain {@link InvestmentIngestionConfigurationProperties} instance populated
     * from the caboose-specific feature flags.
     *
     * <p>This object is <strong>not</strong> registered as a Spring bean — it is created
     * inline inside each saga factory method. This intentionally avoids having two beans of
     * type {@link InvestmentIngestionConfigurationProperties} in the Spring context (which
     * would cause autowiring ambiguity in dual-write mode).
     *
     * <p>The saga's internal guard ({@code coreConfigurationProperties.isAssetUniverseEnabled()},
     * etc.) will therefore read from
     * {@code backbase.bootstrap.ingestions.investment.caboose.*} — completely independent of
     * the primary {@code backbase.bootstrap.ingestions.investment.*} flags.
     *
     * @param cabooseIngestionProperties bound caboose properties
     * @return a fresh properties instance carrying only the caboose flags
     */
    private static InvestmentIngestionConfigurationProperties buildCabooseIngestionConfigurationProperties(
        InvestmentCabooseIngestionProperties cabooseIngestionProperties) {
        InvestmentIngestionConfigurationProperties props = new InvestmentIngestionConfigurationProperties();
        props.setAssetUniverseEnabled(cabooseIngestionProperties.isAssetUniverseEnabled());
        props.setWealthEnabled(cabooseIngestionProperties.isWealthEnabled());
        return props;
    }
}
