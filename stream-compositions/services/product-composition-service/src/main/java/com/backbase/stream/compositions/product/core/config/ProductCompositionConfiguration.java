package com.backbase.stream.compositions.product.core.config;

import com.backbase.stream.compositions.integration.product.ApiClient;
import com.backbase.stream.compositions.integration.product.api.ProductIntegrationApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.WebClient;

import java.text.DateFormat;

@Configuration
@AllArgsConstructor
@EnableConfigurationProperties(ProductConfigurationProperties.class)
public class ProductCompositionConfiguration {
    private final ProductConfigurationProperties productConfigurationProperties;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf().disable()
                .build();
    }

    @Bean
    @Primary
    public ProductIntegrationApi productIntegrationApi(ApiClient productClient) {
        return new ProductIntegrationApi(productClient);
    }

    @Bean
    public ApiClient productClient(
            WebClient dbsWebClient,
            ObjectMapper objectMapper,
            DateFormat dateFormat) {
        ApiClient apiClient = new ApiClient(dbsWebClient, objectMapper, dateFormat);
        apiClient.setBasePath(productConfigurationProperties.getIntegrationBaseUrl());

        return apiClient;
    }
}
