package com.backbase.stream.configuration;

import com.backbase.investment.api.service.ApiClient;
import com.backbase.investment.api.service.v1.*;
import com.backbase.stream.clients.autoconfigure.DbsApiClientsAutoConfiguration;
import com.backbase.stream.investment.saga.InvestmentAssetUniversSaga;
import com.backbase.stream.investment.saga.InvestmentSaga;
import com.backbase.stream.investment.service.*;
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
        CustomIntegrationApiService customIntegrationApiService) {
        return new InvestmentAssetUniverseService(assetUniverseApi, customIntegrationApiService);
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
    public InvestmentPortfolioAllocationService investmentPortfolioAllocationService(AllocationsApi allocationsApi,
        AssetUniverseApi assetUniverseApi, InvestmentApi investmentApi,
        CustomIntegrationApiService customIntegrationApiService) {
        return new InvestmentPortfolioAllocationService(allocationsApi, assetUniverseApi, investmentApi,
            customIntegrationApiService);
    }

    @Bean
    public InvestmentNewsContentService investmentNewsContentService(ContentApi contentApi, ApiClient apiClient) {
        return new InvestmentNewsContentService(contentApi, apiClient);
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
    public InvestmentAssetUniversSaga investmentStaticDataSaga(
        InvestmentAssetUniverseService investmentAssetUniverseService,
        InvestmentAssetPriceService investmentAssetPriceService,
        InvestmentNewsContentService investmentNewsContentService,
        InvestmentIngestionConfigurationProperties coreConfigurationProperties) {
        return new InvestmentAssetUniversSaga(investmentAssetUniverseService, investmentAssetPriceService,
            investmentNewsContentService, coreConfigurationProperties);
    }

}
