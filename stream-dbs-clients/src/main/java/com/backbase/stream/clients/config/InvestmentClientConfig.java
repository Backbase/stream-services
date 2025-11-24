package com.backbase.stream.clients.config;

import com.backbase.investment.api.service.ApiClient;
import com.backbase.investment.api.service.v1.AssetUniverseApi;
import com.backbase.investment.api.service.v1.ClientApi;
import com.backbase.investment.api.service.v1.FinancialAdviceApi;
import com.backbase.investment.api.service.v1.InvestmentProductsApi;
import com.backbase.investment.api.service.v1.PortfolioApi;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

    /**
     * Configuration for Investment service REST client (ClientApi).
     */
    @Bean
    @ConditionalOnMissingBean
    public ApiClient investmentApiClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        ObjectMapper mapper = objectMapper.copy();
        objectMapper.setSerializationInclusion(Include.NON_EMPTY);
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new ApiClient(getWebClient(), mapper, dateFormat)
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

    @Bean
    @ConditionalOnMissingBean
    public AssetUniverseApi assetUniverseApi(ApiClient investmentApiClient) {
        return new AssetUniverseApi(investmentApiClient);
    }

}
