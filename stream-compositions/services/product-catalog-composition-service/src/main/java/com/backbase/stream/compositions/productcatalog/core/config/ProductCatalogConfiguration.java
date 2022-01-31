package com.backbase.stream.compositions.productcatalog.core.config;

import com.backbase.stream.compositions.integration.productcatalog.ApiClient;
import com.backbase.stream.compositions.integration.productcatalog.api.ProductCatalogIntegrationApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.WebClient;

import java.text.DateFormat;

@Configuration
@AllArgsConstructor
@EnableWebFluxSecurity
@EnableConfigurationProperties(ProductCatalogConfigurationProperties.class)
public class ProductCatalogConfiguration {
    private final ProductCatalogConfigurationProperties properties;

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
    public ApiClient productCatalogClient(
            WebClient dbsWebClient,
            ObjectMapper objectMapper,
            DateFormat dateFormat) {
        ApiClient apiClient = new ApiClient(dbsWebClient, objectMapper, dateFormat);
        apiClient.setBasePath(properties.getProductCatalogIntegrationUrl());

        return apiClient;
    }
}
