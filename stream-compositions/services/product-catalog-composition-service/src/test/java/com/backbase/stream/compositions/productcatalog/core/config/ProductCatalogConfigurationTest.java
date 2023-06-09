package com.backbase.stream.compositions.productcatalog.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.backbase.stream.compositions.integration.productcatalog.ApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

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

        ApiClient apiClient =
            configuration.productCatalogClient(
                WebClient.builder().build(), new ObjectMapper(), DateFormat.getDateInstance());
        assertEquals("http://product-catalog", apiClient.getBasePath());
    }
}
