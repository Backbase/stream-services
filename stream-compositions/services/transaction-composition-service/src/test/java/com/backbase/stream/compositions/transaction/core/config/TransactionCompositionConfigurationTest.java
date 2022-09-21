package com.backbase.stream.compositions.transaction.core.config;

import com.backbase.stream.compositions.transaction.cursor.client.TransactionCursorApi;
import com.backbase.stream.compositions.transaction.integration.ApiClient;
import com.backbase.stream.compositions.transaction.integration.client.TransactionIntegrationApi;
import com.backbase.stream.compositions.transaction.core.config.TransactionCompositionConfiguration;
import com.backbase.stream.compositions.transaction.core.config.TransactionConfigurationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.text.DateFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class TransactionCompositionConfigurationTest {

    @Mock
    private WebClient webClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private DateFormat dateFormat;

    @Mock
    private TransactionConfigurationProperties properties;

    private ApiClient transactionIntegrationClient;

    private com.backbase.stream.compositions.transaction.cursor.ApiClient transactionCursorClient;

    private TransactionCursorApi transactionCursorApi;

    private TransactionIntegrationApi transactionIntegrationApi;

    @BeforeEach
    void init() {
        TransactionConfigurationProperties.Cursor cursorConfig = new TransactionConfigurationProperties.Cursor();
        cursorConfig.setBaseUrl("https://transaction-cursor");

        Mockito.when(properties.getIntegrationBaseUrl()).thenReturn("https://transaction-integration");
        Mockito.when(properties.getCursor()).thenReturn(cursorConfig);

        transactionCursorClient = new com.backbase.stream.compositions.transaction.cursor.ApiClient
                (webClient, objectMapper, dateFormat);
        transactionCursorClient.setBasePath(properties.getCursor().getBaseUrl());

        transactionIntegrationClient = new ApiClient(webClient, objectMapper, dateFormat);
        transactionIntegrationClient.setBasePath(properties.getIntegrationBaseUrl());

        transactionCursorApi = new TransactionCursorApi(transactionCursorClient);
        transactionIntegrationApi = new TransactionIntegrationApi(transactionIntegrationClient);

    }

    @Test
    void testCompositionConfig() {
        TransactionCompositionConfiguration config = new TransactionCompositionConfiguration(properties);

        //Mockito.when(config.transactionIntegrationApi(any())).thenReturn(transactionIntegrationApi);
        //Mockito.when(config.transactionCursorApi(any())).thenReturn(transactionCursorApi);

        assertNotNull(config.transactionIntegrationApi(transactionIntegrationClient));
        assertNotNull(config.transactionIntegrationApi(transactionIntegrationClient).getApiClient());
        assertNotNull(config.transactionIntegrationApi(transactionIntegrationClient).getApiClient().getBasePath());
        assertEquals("https://transaction-integration",
                config.transactionIntegrationApi(transactionIntegrationClient).getApiClient().getBasePath());

        assertNotNull(config.transactionCursorApi(transactionCursorClient));
        assertNotNull(config.transactionCursorApi(transactionCursorClient).getApiClient());
        assertNotNull(config.transactionCursorApi(transactionCursorClient).getApiClient().getBasePath());
        assertEquals("https://transaction-cursor",
                config.transactionCursorApi(transactionCursorClient).getApiClient().getBasePath());
    }
}
