package com.backbase.stream.clients.config;

import com.backbase.investment.api.service.ApiClient;
import com.backbase.investment.api.service.v1.ClientApi;
import com.backbase.investment.api.service.v1.FinancialAdviceApi;
import com.backbase.investment.api.service.v1.InvestmentProductsApi;
import com.backbase.investment.api.service.v1.PortfolioApi;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Investment service REST client (ClientApi).
 */
@Configuration
@ConfigurationProperties("backbase.communication.services.investment")
public class InvestmentClientConfig extends CompositeApiClientConfig {

    public static final String INVESTMENT_SERVICE_ID = "investment";

    public InvestmentClientConfig() {
        super(INVESTMENT_SERVICE_ID);
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiClient investmentApiClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        objectMapper.setSerializationInclusion(Include.NON_EMPTY);
        return new ApiClient(getWebClient(), objectMapper, dateFormat)
            .setBasePath(createBasePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public ClientApi clientApi(ApiClient investmentApiClient) {
        return new ClientApi(investmentApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public InvestmentProductsApi investmentProductsApi(ApiClient investmentApiClient) {
        return new InvestmentProductsApi(investmentApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public PortfolioApi portfolioApi(ApiClient investmentApiClient) {
        return new PortfolioApi(investmentApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public FinancialAdviceApi financialAdviceApi(ApiClient investmentApiClient) {
        return new FinancialAdviceApi(investmentApiClient);
    }

}
