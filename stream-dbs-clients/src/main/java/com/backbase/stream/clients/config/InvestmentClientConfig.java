package com.backbase.stream.clients.config;

import com.backbase.investment.api.service.ApiClient;
import com.backbase.investment.api.service.v1.ClientApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.openapitools.jackson.nullable.JsonNullableModule;
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
        JsonNullableModule jnm = new JsonNullableModule();
        objectMapper.registerModule(jnm);
        return new ApiClient(getWebClient(), objectMapper, dateFormat)
            .setBasePath(createBasePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public ClientApi clientApi(ApiClient investmentApiClient) {
        return new ClientApi(investmentApiClient);
    }
}

