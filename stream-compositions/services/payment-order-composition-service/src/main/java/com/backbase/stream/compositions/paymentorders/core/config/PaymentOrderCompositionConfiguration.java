package com.backbase.stream.compositions.paymentorders.core.config;

import com.backbase.stream.clients.config.CompositeApiClientConfig;
import com.backbase.stream.compositions.paymentorder.integration.ApiClient;
import com.backbase.stream.compositions.paymentorder.integration.client.PaymentOrderIntegrationApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@ConfigurationProperties("backbase.communication.services.stream.payment-order.integration")
public class PaymentOrderCompositionConfiguration extends CompositeApiClientConfig {

    private static final String SERVICE_ID = "payment-order-integration";

    public PaymentOrderCompositionConfiguration() {
        super(SERVICE_ID);
    }

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
    public ApiClient paymentOrderIntegrationClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat)
            .setBasePath(createBasePath());
    }
}
