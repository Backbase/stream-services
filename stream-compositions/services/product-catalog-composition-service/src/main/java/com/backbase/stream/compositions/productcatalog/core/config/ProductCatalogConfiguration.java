package com.backbase.stream.compositions.productcatalog.core.config;

import com.backbase.stream.clients.config.CompositeApiClientConfig;
import com.backbase.stream.compositions.integration.productcatalog.ApiClient;
import com.backbase.stream.compositions.integration.productcatalog.api.ProductCatalogIntegrationApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableConfigurationProperties(ProductCatalogConfigurationProperties.class)
@ConfigurationProperties("backbase.communication.services.stream.product-catalog.integration")
public class ProductCatalogConfiguration extends CompositeApiClientConfig {

    private static final String SERVICE_ID = "product-catalog-ingestion-integration";

    public ProductCatalogConfiguration() {
        super(SERVICE_ID);
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http.csrf().disable().build();
    }

    @Bean
    @Primary
    public ProductCatalogIntegrationApi productCatalogIntegrationApi(ApiClient legalEntityClient) {
        return new ProductCatalogIntegrationApi(legalEntityClient);
    }

    @Bean
    public ApiClient productCatalogClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat)
            .setBasePath(createBasePath());
    }
}
