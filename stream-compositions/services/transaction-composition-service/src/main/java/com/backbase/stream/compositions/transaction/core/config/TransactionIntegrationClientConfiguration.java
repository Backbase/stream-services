package com.backbase.stream.compositions.transaction.core.config;

import com.backbase.stream.clients.config.CompositeApiClientConfig;
import com.backbase.stream.compositions.transaction.integration.ApiClient;
import com.backbase.stream.compositions.transaction.integration.client.TransactionIntegrationApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConfigurationProperties("backbase.communication.services.stream.transaction.integration")
public class TransactionIntegrationClientConfiguration extends CompositeApiClientConfig {

    public static final String SERVICE_ID = "transaction-integration";

    public TransactionIntegrationClientConfiguration() {
        super(SERVICE_ID);
    }

    @Bean
    public ApiClient transactionIntegrationClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat)
            .setBasePath(createBasePath());
    }

    @Bean
    @Primary
    public TransactionIntegrationApi transactionIntegrationApi(ApiClient transactionIntegrationClient) {
        return new TransactionIntegrationApi(transactionIntegrationClient);
    }

}
