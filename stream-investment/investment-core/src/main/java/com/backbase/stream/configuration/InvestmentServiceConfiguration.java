package com.backbase.stream.configuration;

import com.backbase.investment.api.service.ApiClient;
import com.backbase.investment.api.service.v1.AssetUniverseApi;
import com.backbase.investment.api.service.v1.ClientApi;
import com.backbase.investment.api.service.v1.FinancialAdviceApi;
import com.backbase.investment.api.service.v1.InvestmentProductsApi;
import com.backbase.investment.api.service.v1.PortfolioApi;
import com.backbase.stream.clients.autoconfigure.DbsApiClientsAutoConfiguration;
import com.backbase.stream.investment.saga.InvestmentSaga;
import com.backbase.stream.investment.service.InvestmentAssetUniverseService;
import com.backbase.stream.investment.service.InvestmentClientService;
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
    InvestmentSagaConfigurationProperties.class
})
@RequiredArgsConstructor
@Configuration
@ConditionalOnProperty(name = "backbase.bootstrap.ingestions.wealth.enabled")
public class InvestmentServiceConfiguration {

    @Bean
    public InvestmentSaga investmentSaga(ClientApi clientApi, PortfolioApi portfolioApi,
        InvestmentProductsApi investmentProductsApi, FinancialAdviceApi financialAdviceApi,
        AssetUniverseApi assetUniverseApi, InvestmentSagaConfigurationProperties properties,
        ApiClient apiClient) {
        return new InvestmentSaga(
            new InvestmentClientService(clientApi),
            new InvestmentPortfolioService(investmentProductsApi, portfolioApi, financialAdviceApi),
            new InvestmentAssetUniverseService(assetUniverseApi, apiClient));
    }

}
