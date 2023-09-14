package com.backbase.stream.compositions.product.core.config;

import com.backbase.stream.clients.config.CompositeApiClientConfig;
import com.backbase.stream.compositions.integration.product.ApiClient;
import com.backbase.stream.compositions.integration.product.api.ArrangementIntegrationApi;
import com.backbase.stream.compositions.integration.product.api.ProductIntegrationApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConfigurationProperties("backbase.communication.services.stream.product.integration")
public class ProductIntegrationClientConfiguration extends CompositeApiClientConfig {

    public static final String SERVICE_ID = "product-integration";

    public ProductIntegrationClientConfiguration() {
        super(SERVICE_ID);
    }

    @Bean
    public ApiClient productClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat)
            .setBasePath(createBasePath());
    }

    @Bean
    @Primary
    public ProductIntegrationApi productIntegrationApi(ApiClient productClient) {
        return new ProductIntegrationApi(productClient);
    }

    @Bean
    @Primary
    public ArrangementIntegrationApi arrangementIntegrationApi(ApiClient productClient) {
        return new ArrangementIntegrationApi(productClient);
    }

}
