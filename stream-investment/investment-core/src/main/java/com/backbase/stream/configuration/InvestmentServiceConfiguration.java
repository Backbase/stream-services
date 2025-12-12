package com.backbase.stream.configuration;

import com.backbase.investment.api.service.ApiClient;
import com.backbase.investment.api.service.v1.AllocationsApi;
import com.backbase.investment.api.service.v1.AssetUniverseApi;
import com.backbase.investment.api.service.v1.AsyncBulkGroupsApi;
import com.backbase.investment.api.service.v1.ClientApi;
import com.backbase.investment.api.service.v1.FinancialAdviceApi;
import com.backbase.investment.api.service.v1.InvestmentProductsApi;
import com.backbase.investment.api.service.v1.PortfolioApi;
import com.backbase.stream.clients.autoconfigure.DbsApiClientsAutoConfiguration;
import com.backbase.stream.investment.saga.InvestmentAssetUniversSaga;
import com.backbase.stream.investment.saga.InvestmentSaga;
import com.backbase.stream.investment.service.CustomIntegrationApiService;
import com.backbase.stream.investment.service.InvestmentAssetPriceService;
import com.backbase.stream.investment.service.InvestmentAssetUniverseService;
import com.backbase.stream.investment.service.InvestmentClientService;
import com.backbase.stream.investment.service.InvestmentModelPortfolioService;
import com.backbase.stream.investment.service.InvestmentPortfolioAllocationService;
import com.backbase.stream.investment.service.InvestmentPortfolioService;
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
        InvestmentProductsApi investmentProductsApi,
        InvestmentIngestionConfigurationProperties configurationProperties) {
        return new InvestmentPortfolioService(investmentProductsApi, portfolioApi, configurationProperties);
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
    public InvestmentAssetPriceService investmentAssetPriceService(AssetUniverseApi assetUniverseApi,
        AsyncBulkGroupsApi asyncBulkGroupsApi) {
        return new InvestmentAssetPriceService(assetUniverseApi, asyncBulkGroupsApi);
    }

    @Bean
    public InvestmentPortfolioAllocationService investmentPortfolioAllocationService(AllocationsApi allocationsApi,
        AssetUniverseApi assetUniverseApi, CustomIntegrationApiService customIntegrationApiService) {
        return new InvestmentPortfolioAllocationService(allocationsApi, assetUniverseApi,
            customIntegrationApiService);
    }

    @Bean
    public InvestmentSaga investmentSaga(InvestmentClientService investmentClientService,
        InvestmentPortfolioService investmentPortfolioService,
        InvestmentModelPortfolioService investmentModelPortfolioService,
        InvestmentPortfolioAllocationService investmentPortfolioAllocationService,
        InvestmentIngestionConfigurationProperties coreConfigurationProperties) {
        return new InvestmentSaga(investmentClientService, investmentPortfolioService,
            investmentPortfolioAllocationService, investmentModelPortfolioService, coreConfigurationProperties);
    }

    @Bean
    public InvestmentAssetUniversSaga investmentStaticDataSaga(
        InvestmentAssetUniverseService investmentAssetUniverseService,
        InvestmentAssetPriceService investmentAssetPriceService,
        InvestmentIngestionConfigurationProperties coreConfigurationProperties) {
        return new InvestmentAssetUniversSaga(investmentAssetUniverseService, investmentAssetPriceService,
            coreConfigurationProperties);
    }

}
