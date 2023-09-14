package com.backbase.stream.compositions.transaction.core.config;

import com.backbase.stream.clients.config.CompositeApiClientConfig;
import com.backbase.stream.compositions.transaction.cursor.ApiClient;
import com.backbase.stream.compositions.transaction.cursor.client.TransactionCursorApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConfigurationProperties("backbase.communication.services.stream.transaction.cursor")
public class TransactionCursorClientConfiguration extends CompositeApiClientConfig {

    public static final String SERVICE_ID = "transaction-cursor";

    public TransactionCursorClientConfiguration() {
        super(SERVICE_ID);
    }

    @Bean
    public ApiClient transactionCursorClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat)
            .setBasePath(createBasePath());
    }

    @Bean
    @Primary
    public TransactionCursorApi transactionCursorApi(ApiClient transactionCursorClient) {
        return new TransactionCursorApi(transactionCursorClient);
    }

}
