package com.backbase.stream.configuration;

import com.backbase.investment.api.service.ApiClient;
import com.backbase.investment.api.service.v1.AllocationsApi;
import com.backbase.investment.api.service.v1.AssetUniverseApi;
import com.backbase.investment.api.service.v1.AsyncBulkGroupsApi;
import com.backbase.investment.api.service.v1.ClientApi;
import com.backbase.investment.api.service.v1.FinancialAdviceApi;
import com.backbase.investment.api.service.v1.InvestmentApi;
import com.backbase.investment.api.service.v1.InvestmentProductsApi;
import com.backbase.investment.api.service.v1.PaymentsApi;
import com.backbase.investment.api.service.v1.PortfolioApi;
import com.backbase.stream.clients.autoconfigure.DbsApiClientsAutoConfiguration;
import com.backbase.stream.investment.saga.InvestmentAssetUniverseSaga;
import com.backbase.stream.investment.saga.InvestmentContentSaga;
import com.backbase.stream.investment.saga.InvestmentSaga;
import com.backbase.stream.investment.service.AsyncTaskService;
import com.backbase.stream.investment.service.CustomIntegrationApiService;
import com.backbase.stream.investment.service.InvestmentAssetPriceService;
import com.backbase.stream.investment.service.InvestmentAssetUniverseService;
import com.backbase.stream.investment.service.InvestmentClientService;
import com.backbase.stream.investment.service.InvestmentIntradayAssetPriceService;
import com.backbase.stream.investment.service.InvestmentModelPortfolioService;
import com.backbase.stream.investment.service.InvestmentPortfolioAllocationService;
import com.backbase.stream.investment.service.InvestmentPortfolioService;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestAssetUniverseService;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestNewsContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Import({
    DbsApiClientsAutoConfiguration.class,
})
@EnableConfigurationProperties({
    InvestmentIngestionConfigurationProperties.class
})
@RequiredArgsConstructor
@Configuration
@ConditionalOnProperty(name = "backbase.bootstrap.ingestions.investment.enabled")
public class InvestmentServiceConfiguration {

    @Bean
    public InvestmentClientService investmentClientService(ClientApi clientApi) {
        return new InvestmentClientService(clientApi);
    }

    @Bean
    public CustomIntegrationApiService customIntegrationApiService(ApiClient apiClient) {
        return new CustomIntegrationApiService(apiClient);
    }

    @Bean
    public InvestmentPortfolioService investmentPortfolioService(PortfolioApi portfolioApi,
        InvestmentProductsApi investmentProductsApi, PaymentsApi paymentsApi,
        InvestmentIngestionConfigurationProperties configurationProperties) {
        return new InvestmentPortfolioService(investmentProductsApi, portfolioApi, paymentsApi,
            configurationProperties);
    }

    @Bean
    public InvestmentAssetUniverseService investmentAssetUniverseService(AssetUniverseApi assetUniverseApi,
        InvestmentRestAssetUniverseService investmentRestAssetUniverseService,
        CustomIntegrationApiService customIntegrationApiService) {
        return new InvestmentAssetUniverseService(assetUniverseApi, investmentRestAssetUniverseService,
            customIntegrationApiService);
    }

    @Bean
    public AsyncTaskService asyncTaskService(AsyncBulkGroupsApi asyncBulkGroupsApi) {
        return new AsyncTaskService(asyncBulkGroupsApi);
    }

    @Bean
    public InvestmentModelPortfolioService investmentModelPortfolioService(FinancialAdviceApi financialAdviceApi,
        CustomIntegrationApiService customIntegrationApiService) {
        return new InvestmentModelPortfolioService(financialAdviceApi, customIntegrationApiService);
    }

    @Bean
    public InvestmentAssetPriceService investmentAssetPriceService(AssetUniverseApi assetUniverseApi) {
        return new InvestmentAssetPriceService(assetUniverseApi);
    }

    @Bean
    public InvestmentIntradayAssetPriceService investmentIntradayAssetPriceService(AssetUniverseApi assetUniverseApi) {
        return new InvestmentIntradayAssetPriceService(assetUniverseApi);
    }

    @Bean
    public InvestmentPortfolioAllocationService investmentPortfolioAllocationService(AllocationsApi allocationsApi,
        AssetUniverseApi assetUniverseApi, InvestmentApi investmentApi,
        CustomIntegrationApiService customIntegrationApiService) {
        return new InvestmentPortfolioAllocationService(allocationsApi, assetUniverseApi, investmentApi,
            customIntegrationApiService);
    }

    @Bean
    public InvestmentSaga investmentSaga(InvestmentClientService investmentClientService,
        InvestmentPortfolioService investmentPortfolioService,
        InvestmentModelPortfolioService investmentModelPortfolioService,
        InvestmentPortfolioAllocationService investmentPortfolioAllocationService, AsyncTaskService asyncTaskService,
        InvestmentIngestionConfigurationProperties coreConfigurationProperties) {
        return new InvestmentSaga(investmentClientService, investmentPortfolioService,
            investmentPortfolioAllocationService, investmentModelPortfolioService, asyncTaskService,
            coreConfigurationProperties);
    }

    @Bean
    public InvestmentAssetUniverseSaga investmentStaticDataSaga(
        InvestmentAssetUniverseService investmentAssetUniverseService,
        InvestmentAssetPriceService investmentAssetPriceService,
        InvestmentIntradayAssetPriceService investmentIntradayAssetPriceService,
        InvestmentIngestionConfigurationProperties coreConfigurationProperties) {
        return new InvestmentAssetUniverseSaga(investmentAssetUniverseService, investmentAssetPriceService,
            investmentIntradayAssetPriceService, coreConfigurationProperties);
    }

    @Bean
    public InvestmentContentSaga investmentContentSaga(
        InvestmentRestNewsContentService investmentRestNewsContentService,
        InvestmentIngestionConfigurationProperties coreConfigurationProperties) {
        return new InvestmentContentSaga(investmentRestNewsContentService, coreConfigurationProperties);
    }

}
