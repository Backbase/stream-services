package com.backbase.stream.compositions.product.core.service.config;

import com.backbase.stream.compositions.integration.product.ApiClient;
import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties;
import com.backbase.stream.compositions.product.core.config.ProductCompositionConfiguration;
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
class ProductConfigurationTest {
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
        ProductConfigurationProperties properties = new ProductConfigurationProperties();
        properties.setProductIntegrationUrl("http://product");

        ProductCompositionConfiguration configuration = new ProductCompositionConfiguration(properties);
        assertNotNull(configuration.productIntegrationApi(apiClient));

        ApiClient apiClient = configuration.productClient(webClient, objectMapper, dateFormat);
        assertEquals("http://product", apiClient.getBasePath());
    }
}
