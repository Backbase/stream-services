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
import com.backbase.stream.investment.saga.InvestmentContentSaga;
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
import com.backbase.stream.investment.service.resttemplate.InvestmentRestDocumentContentService;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestModelPortfolioService;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestNewsContentService;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestProductPortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@Import({
    DbsApiClientsAutoConfiguration.class,
    InvestmentClientConfig.class,
    InvestmentWebClientConfiguration.class,
    InvestmentRestServiceApiConfiguration.class
})
@EnableConfigurationProperties({
    InvestmentIngestionConfigurationProperties.class,
    IngestConfigProperties.class
})
@RequiredArgsConstructor
@Configuration
@ConditionalOnProperty(name = "backbase.bootstrap.ingestions.investment.enabled")
public class InvestmentServiceConfiguration {

    @Bean
    @Primary
    public InvestmentClientService investmentClientService(@Qualifier("clientApi") ClientApi clientApi) {
        return new InvestmentClientService(clientApi);
    }

    @Bean
    @Primary
    public InvestmentModelPortfolioService investmentModelPortfolioService(
        @Qualifier("financialAdviceApi") FinancialAdviceApi financialAdviceApi,
        InvestmentRestModelPortfolioService investmentRestModelPortfolioService,
        IngestConfigProperties portfolioProperties) {
        return new InvestmentModelPortfolioService(financialAdviceApi, investmentRestModelPortfolioService,
            portfolioProperties);
    }

    @Bean
    @Primary
    public InvestmentPortfolioProductService investmentPortfolioProductService(
        @Qualifier("investmentProductsApi") InvestmentProductsApi investmentProductsApi,
        IngestConfigProperties portfolioProperties,
        InvestmentModelPortfolioService investmentModelPortfolioService,
        InvestmentRestProductPortfolioService investmentRestProductPortfolioService) {
        return new InvestmentPortfolioProductService(investmentProductsApi, portfolioProperties,
            investmentModelPortfolioService, investmentRestProductPortfolioService);
    }

    @Bean
    @Primary
    public InvestmentPortfolioService investmentPortfolioService(
        @Qualifier("portfolioApi") PortfolioApi portfolioApi,
        @Qualifier("paymentsApi") PaymentsApi paymentsApi,
        @Qualifier("portfolioTradingAccountsApi") PortfolioTradingAccountsApi portfolioTradingAccountsApi,
        IngestConfigProperties portfolioProperties) {
        return new InvestmentPortfolioService(portfolioApi, paymentsApi,
            portfolioTradingAccountsApi, portfolioProperties);
    }

    @Bean
    @Primary
    public InvestmentAssetUniverseService investmentAssetUniverseService(
        @Qualifier("assetUniverseApi") AssetUniverseApi assetUniverseApi,
        InvestmentRestAssetUniverseService investmentRestAssetUniverseService,
        IngestConfigProperties portfolioProperties) {
        return new InvestmentAssetUniverseService(assetUniverseApi, investmentRestAssetUniverseService,
            portfolioProperties);
    }

    @Bean
    @Primary
    public AsyncTaskService asyncTaskService(
        @Qualifier("asyncBulkGroupsApi") AsyncBulkGroupsApi asyncBulkGroupsApi) {
        return new AsyncTaskService(asyncBulkGroupsApi);
    }

    @Bean
    @Primary
    public InvestmentAssetPriceService investmentAssetPriceService(
        @Qualifier("assetUniverseApi") AssetUniverseApi assetUniverseApi,
        IngestConfigProperties portfolioProperties) {
        return new InvestmentAssetPriceService(assetUniverseApi, portfolioProperties);
    }

    @Bean
    @Primary
    public InvestmentIntradayAssetPriceService investmentIntradayAssetPriceService(
        @Qualifier("assetUniverseApi") AssetUniverseApi assetUniverseApi,
        IngestConfigProperties portfolioProperties) {
        return new InvestmentIntradayAssetPriceService(assetUniverseApi, portfolioProperties);
    }

    @Bean
    @Primary
    public InvestmentPortfolioAllocationService investmentPortfolioAllocationService(
        @Qualifier("allocationsApi") AllocationsApi allocationsApi,
        @Qualifier("assetUniverseApi") AssetUniverseApi assetUniverseApi,
        @Qualifier("investmentApi") InvestmentApi investmentApi,
        IngestConfigProperties portfolioProperties) {
        return new InvestmentPortfolioAllocationService(allocationsApi, assetUniverseApi, investmentApi,
            portfolioProperties);
    }

    @Bean
    @Primary
    public InvestmentCurrencyService investmentCurrencyService(@Qualifier("currencyApi") CurrencyApi currencyApi) {
        return new InvestmentCurrencyService(currencyApi);
    }

    @Bean
    @Primary
    public InvestmentRiskAssessmentService investmentRiskAssessmentService(
        @Qualifier("riskAssessmentApi") RiskAssessmentApi riskAssessmentApi) {
        return new InvestmentRiskAssessmentService(riskAssessmentApi);
    }

    @Bean
    @Primary
    public InvestmentRiskQuestionaryService investmentRiskQuestionaryService(
        @Qualifier("riskAssessmentApi") RiskAssessmentApi riskAssessmentApi,
        IngestConfigProperties portfolioProperties) {
        return new InvestmentRiskQuestionaryService(riskAssessmentApi, portfolioProperties);
    }

    @Bean
    @Primary
    public InvestmentSaga investmentSaga(InvestmentClientService investmentClientService,
        InvestmentRiskAssessmentService investmentRiskAssessmentService,
        InvestmentRiskQuestionaryService investmentRiskQuestionaryService,
        InvestmentPortfolioService investmentPortfolioService,
        InvestmentModelPortfolioService investmentModelPortfolioService,
        InvestmentPortfolioProductService investmentPortfolioProductService,
        InvestmentPortfolioAllocationService investmentPortfolioAllocationService, AsyncTaskService asyncTaskService,
        InvestmentIngestionConfigurationProperties coreConfigurationProperties,
        @Qualifier("assetUniverseApi") AssetUniverseApi assetUniverseApi) {
        return new InvestmentSaga(investmentClientService, investmentRiskAssessmentService,
            investmentRiskQuestionaryService, investmentPortfolioService, investmentPortfolioAllocationService,
            investmentModelPortfolioService, investmentPortfolioProductService, asyncTaskService,
            coreConfigurationProperties, assetUniverseApi);
    }

    @Bean
    @Primary
    public InvestmentAssetUniverseSaga investmentStaticDataSaga(
        InvestmentAssetUniverseService investmentAssetUniverseService,
        InvestmentAssetPriceService investmentAssetPriceService,
        InvestmentIntradayAssetPriceService investmentIntradayAssetPriceService,
        InvestmentCurrencyService investmentCurrencyService,
        AsyncTaskService asyncTaskService,
        InvestmentIngestionConfigurationProperties coreConfigurationProperties,
        IngestConfigProperties portfolioProperties) {
        return new InvestmentAssetUniverseSaga(investmentAssetUniverseService, investmentAssetPriceService,
            investmentIntradayAssetPriceService, investmentCurrencyService, asyncTaskService,
            coreConfigurationProperties, portfolioProperties);
    }

    @Bean
    @Primary
    public InvestmentContentSaga investmentContentSaga(
        InvestmentRestNewsContentService investmentRestNewsContentService,
        InvestmentRestDocumentContentService investmentRestDocumentContentService,
        InvestmentIngestionConfigurationProperties coreConfigurationProperties) {
        return new InvestmentContentSaga(investmentRestNewsContentService, investmentRestDocumentContentService,
            coreConfigurationProperties);
    }

}
