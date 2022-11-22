package com.backbase.stream.compositions.product.core.config;

import com.backbase.buildingblocks.webclient.WebClientConstants;
import com.backbase.stream.compositions.integration.product.ApiClient;
import com.backbase.stream.compositions.integration.product.api.ProductIntegrationApi;
import com.backbase.stream.compositions.paymentorder.client.PaymentOrderCompositionApi;
import com.backbase.stream.compositions.transaction.client.TransactionCompositionApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.WebClient;

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
    @Primary
    public TransactionCompositionApi transactionCompositionApi(
            com.backbase.stream.compositions.transaction.ApiClient transactionClient) {
        return new TransactionCompositionApi(transactionClient);
    }

    @Bean
    @Primary
    public PaymentOrderCompositionApi paymentOrderCompositionApi(
            com.backbase.stream.compositions.paymentorder.ApiClient paymentOrderApiClient) {
        return new PaymentOrderCompositionApi(paymentOrderApiClient);
    }

    @Bean
    public com.backbase.stream.compositions.transaction.ApiClient transactionClient(
            @Qualifier(WebClientConstants.INTER_SERVICE_WEB_CLIENT_NAME) WebClient dbsWebClient,
            ObjectMapper objectMapper,
            DateFormat dateFormat) {
        com.backbase.stream.compositions.transaction.ApiClient apiClient =
                new com.backbase.stream.compositions.transaction.ApiClient(dbsWebClient, objectMapper, dateFormat);
        apiClient.setBasePath(productConfigurationProperties.getChains().getTransactionComposition().getBaseUrl());

        return apiClient;
    }

    @Bean
    public com.backbase.stream.compositions.paymentorder.ApiClient paymentOrderApiClient(
            @Qualifier(WebClientConstants.INTER_SERVICE_WEB_CLIENT_NAME) WebClient dbsWebClient,
            ObjectMapper objectMapper,
            DateFormat dateFormat) {
        com.backbase.stream.compositions.paymentorder.ApiClient apiClient =
                new com.backbase.stream.compositions.paymentorder.ApiClient(dbsWebClient, objectMapper, dateFormat);
        apiClient.setBasePath(productConfigurationProperties.getChains().getPaymentOrderComposition().getBaseUrl());

        return apiClient;
    }

    @Bean
    public ApiClient productClient(
            @Qualifier(WebClientConstants.INTER_SERVICE_WEB_CLIENT_NAME) WebClient dbsWebClient,
            ObjectMapper objectMapper,
            DateFormat dateFormat) {
        ApiClient apiClient = new ApiClient(dbsWebClient, objectMapper, dateFormat);
        apiClient.setBasePath(productConfigurationProperties.getIntegrationBaseUrl());

        return apiClient;
    }
}
