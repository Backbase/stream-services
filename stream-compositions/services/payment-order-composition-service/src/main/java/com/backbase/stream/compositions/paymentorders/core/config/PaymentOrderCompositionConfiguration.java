package com.backbase.stream.compositions.paymentorders.core.config;

import com.backbase.buildingblocks.webclient.WebClientConstants;
import com.backbase.stream.compositions.paymentorder.integration.ApiClient;
import com.backbase.stream.compositions.paymentorder.integration.client.PaymentOrderIntegrationApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
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
@EnableConfigurationProperties(PaymentOrderConfigurationProperties.class)
public class PaymentOrderCompositionConfiguration {
    private final PaymentOrderConfigurationProperties paymentOrderConfigurationProperties;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http.csrf().disable().build();
    }

    @Bean
    @Primary
    public PaymentOrderIntegrationApi paymentOrderIntegrationApi(ApiClient paymentOrderIntegrationClient) {
        return new PaymentOrderIntegrationApi(paymentOrderIntegrationClient);
    }

    @Bean
    public ApiClient paymentOrderIntegrationClient(
            @Qualifier(WebClientConstants.INTER_SERVICE_WEB_CLIENT_NAME) WebClient dbsWebClient,
            ObjectMapper objectMapper,
            DateFormat dateFormat) {
        ApiClient apiClient = new ApiClient(dbsWebClient, objectMapper, dateFormat);
        apiClient.setBasePath(paymentOrderConfigurationProperties.getIntegrationBaseUrl());

        return apiClient;
    }
}
