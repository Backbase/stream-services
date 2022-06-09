package com.backbase.stream.compositions.product.core.service.config;

import com.backbase.stream.compositions.integration.product.api.ProductIntegrationApi;
import com.backbase.stream.compositions.product.core.config.ProductCompositionConfiguration;
import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.backbase.stream.compositions.integration.product.ApiClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class ProductConfigurationTest {

    @Mock
    ApiClient apiClient;

    @Test
    void test() {
        ProductConfigurationProperties properties = new ProductConfigurationProperties();
        properties.setIntegrationBaseUrl("http://product");

        ProductCompositionConfiguration configuration = new ProductCompositionConfiguration(properties);
        assertNotNull(configuration.productIntegrationApi(apiClient));

        ProductIntegrationApi productIntegrationApi = configuration.productIntegrationApi(apiClient);
        // TODO
        //assertEquals("http://product", productIntegrationApi.getApiClient().getBasePath());
    }
}
