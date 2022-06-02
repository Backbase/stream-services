package com.backbase.stream.compositions.product.core.service.config;

import com.backbase.stream.compositions.integration.product.api.ProductIntegrationApi;
import com.backbase.stream.compositions.product.core.config.ProductCompositionConfiguration;
import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class ProductConfigurationTest {
    @Test
    void test() {
        ProductConfigurationProperties properties = new ProductConfigurationProperties();
        properties.setIntegrationBaseUrl("http://product");

        ProductCompositionConfiguration configuration = new ProductCompositionConfiguration(properties);
        assertNotNull(configuration.productIntegrationApi());

        ProductIntegrationApi productIntegrationApi = configuration.productIntegrationApi();
        assertEquals("http://product", productIntegrationApi.getApiClient().getBasePath());
    }
}
