package com.backbase.stream.configuration;

import com.backbase.investment.api.service.v1.ClientApi;
import com.backbase.investment.api.service.v1.FinancialAdviceApi;
import com.backbase.investment.api.service.v1.InvestmentProductsApi;
import com.backbase.investment.api.service.v1.PortfolioApi;
import com.backbase.stream.clients.autoconfigure.DbsApiClientsAutoConfiguration;
import com.backbase.stream.investment.saga.InvestmentSaga;
import com.backbase.stream.investment.service.InvestmentClientService;
import com.backbase.stream.investment.service.InvestmentPortfolioService;
import lombok.RequiredArgsConstructor;
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
public class InvestmentServiceConfiguration {

    @Bean
    public InvestmentSaga investmentSaga(ClientApi clientApi, PortfolioApi portfolioApi,
        InvestmentProductsApi investmentProductsApi, FinancialAdviceApi financialAdviceApi,
        InvestmentSagaConfigurationProperties properties) {
        return new InvestmentSaga(
            new InvestmentClientService(clientApi),
            new InvestmentPortfolioService(investmentProductsApi, portfolioApi, financialAdviceApi));
    }

}
