package com.backbase.stream.compositions.product.core.config;

import com.backbase.stream.clients.config.CompositeApiClientConfig;
import com.backbase.stream.compositions.transaction.ApiClient;
import com.backbase.stream.compositions.transaction.client.TransactionCompositionApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConfigurationProperties("backbase.communication.services.stream.transaction.composition")
public class TransactionCompositionClientConfiguration extends CompositeApiClientConfig {

    public static final String SERVICE_ID = "transaction-composition";

    public TransactionCompositionClientConfiguration() {
        super(SERVICE_ID);
    }

    @Bean
    public ApiClient transactionClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat)
            .setBasePath(createBasePath());
    }

    @Bean
    @Primary
    public TransactionCompositionApi transactionCompositionApi(ApiClient transactionClient) {
        return new TransactionCompositionApi(transactionClient);
    }

}
