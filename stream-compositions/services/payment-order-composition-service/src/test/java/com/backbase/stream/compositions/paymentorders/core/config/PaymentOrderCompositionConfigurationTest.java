package com.backbase.stream.compositions.paymentorders.core.config;

import com.backbase.stream.compositions.paymentorder.integration.ApiClient;
import com.backbase.stream.compositions.paymentorder.integration.client.PaymentOrderIntegrationApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.text.DateFormat;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PaymentOrderCompositionConfigurationTest {

    @Mock
    private WebClient webClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private DateFormat dateFormat;

    @Mock
    private PaymentOrderConfigurationProperties properties;

    private ApiClient paymentOrderIntegrationClient;

    private PaymentOrderIntegrationApi paymentOrderIntegrationApi;

    @BeforeEach
    void init() {

        Mockito.when(properties.getIntegrationBaseUrl()).thenReturn("https://payment-order-integration");

        paymentOrderIntegrationClient = new ApiClient(webClient, objectMapper, dateFormat);
        paymentOrderIntegrationClient.setBasePath(properties.getIntegrationBaseUrl());

        paymentOrderIntegrationApi = new PaymentOrderIntegrationApi(paymentOrderIntegrationClient);

    }

    @Test
    void testCompositionConfig() {
        PaymentOrderCompositionConfiguration config = new PaymentOrderCompositionConfiguration(properties);

        assertNotNull(config.paymentOrderIntegrationApi(paymentOrderIntegrationClient));
        assertNotNull(config.paymentOrderIntegrationApi(paymentOrderIntegrationClient).getApiClient());
        assertNotNull(config.paymentOrderIntegrationApi(paymentOrderIntegrationClient).getApiClient().getBasePath());
        assertEquals("https://payment-order-integration",
                config.paymentOrderIntegrationApi(paymentOrderIntegrationClient).getApiClient().getBasePath());

    }

}