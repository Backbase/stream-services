package com.backbase.stream.compositions.transaction.core.config;

import com.backbase.stream.compositions.integration.transaction.ApiClient;
import com.backbase.stream.compositions.integration.transaction.api.TransactionIntegrationApi;
import com.backbase.stream.compositions.transaction.core.mapper.TransactionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.mapstruct.factory.Mappers;
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
@EnableConfigurationProperties(TransactionConfigurationProperties.class)
public class TransactionCompositionConfiguration {
    private final TransactionConfigurationProperties productConfigurationProperties;

    @Bean
    public TransactionMapper mapper() {
        return Mappers.getMapper(TransactionMapper.class);
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf().disable()
                .build();
    }

    @Bean
    @Primary
    public TransactionIntegrationApi productIntegrationApi(ApiClient productClient) {
        return new TransactionIntegrationApi(productClient);
    }

    @Bean
    public ApiClient transactionClient(
            WebClient dbsWebClient,
            ObjectMapper objectMapper,
            DateFormat dateFormat) {
        ApiClient apiClient = new ApiClient(dbsWebClient, objectMapper, dateFormat);
        apiClient.setBasePath(productConfigurationProperties.getTransactionIntegrationUrl());

        return apiClient;
    }
}
