package com.backbase.stream.compositions.transaction.core.service.config;

import com.backbase.stream.compositions.integration.transaction.ApiClient;
import com.backbase.stream.compositions.transaction.core.config.TransactionCompositionConfiguration;
import com.backbase.stream.compositions.transaction.core.config.TransactionConfigurationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.text.DateFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class TransactionConfigurationTest {
    @Mock
    ApiClient apiClient;

    @Mock
    WebClient webClient;

    @Mock
    ObjectMapper objectMapper;

    @Mock
    DateFormat dateFormat;

    @Test
    void test() {
        TransactionConfigurationProperties properties = new TransactionConfigurationProperties();
        properties.setTransactionIntegrationUrl("http://transaction");

        TransactionCompositionConfiguration configuration = new TransactionCompositionConfiguration(properties);
        assertNotNull(configuration.productIntegrationApi(apiClient));

        ApiClient apiClient = configuration.transactionClient(webClient, objectMapper, dateFormat);
        assertEquals("http://transaction", apiClient.getBasePath());
    }
}
