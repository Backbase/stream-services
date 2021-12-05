package com.backbase.stream.compositions.legalentity.core.config;

import com.backbase.stream.compositions.integration.product.ApiClient;
import com.backbase.stream.compositions.integration.product.api.ProductIntegrationApi;
import com.backbase.stream.compositions.legalentity.core.mapper.ProductGroupMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

import java.text.DateFormat;

@Configuration
@AllArgsConstructor
@EnableConfigurationProperties(ProductConfigurationProperties.class)
public class ProductCompositionConfiguration {
    private final ProductConfigurationProperties productConfigurationProperties;

    @Bean
    public ProductGroupMapper mapper() {
        return Mappers.getMapper(ProductGroupMapper.class);
    }

    @Bean
    @Primary
    public ProductIntegrationApi productIntegrationApi(ApiClient legalEntityClient) {
        return new ProductIntegrationApi(legalEntityClient);
    }

    @Bean
    public ApiClient legalEntityClient(
            WebClient dbsWebClient,
            ObjectMapper objectMapper,
            DateFormat dateFormat) {
        ApiClient apiClient = new ApiClient(dbsWebClient, objectMapper, dateFormat);
        apiClient.setBasePath(productConfigurationProperties.getProductIntegrationUrl());

        return apiClient;
    }
}
