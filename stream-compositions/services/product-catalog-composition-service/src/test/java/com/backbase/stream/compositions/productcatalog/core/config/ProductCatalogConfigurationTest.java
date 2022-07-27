package com.backbase.stream.compositions.productcatalog.core.config;

import com.backbase.stream.compositions.integration.productcatalog.ApiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class ProductCatalogConfigurationTest {
    @Mock
    ApiClient apiClient;

    @Test
    void test() {
        ProductCatalogConfigurationProperties properties = new ProductCatalogConfigurationProperties();
        properties.setProductCatalogIntegrationUrl("http://product-catalog");

        ProductCatalogConfiguration configuration = new ProductCatalogConfiguration(properties);
        assertNotNull(configuration.productCatalogIntegrationApi(apiClient));

        ApiClient apiClient = configuration.productCatalogClient();
        assertEquals("http://product-catalog", apiClient.getBasePath());
    }
}
