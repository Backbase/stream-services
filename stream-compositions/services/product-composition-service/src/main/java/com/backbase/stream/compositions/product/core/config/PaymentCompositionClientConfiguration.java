package com.backbase.stream.compositions.product.core.config;

import com.backbase.stream.clients.config.CompositeApiClientConfig;
import com.backbase.stream.compositions.paymentorder.ApiClient;
import com.backbase.stream.compositions.paymentorder.client.PaymentOrderCompositionApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConfigurationProperties("backbase.communication.services.stream.payment-order.composition")
public class PaymentCompositionClientConfiguration extends CompositeApiClientConfig {

    public static final String SERVICE_ID = "payment-order-composition";

    public PaymentCompositionClientConfiguration() {
        super(SERVICE_ID);
    }

    @Bean
    public ApiClient paymentOrderApiClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat)
            .setBasePath(createBasePath());
    }

    @Bean
    @Primary
    public PaymentOrderCompositionApi paymentOrderCompositionApi(ApiClient paymentOrderApiClient) {
        return new PaymentOrderCompositionApi(paymentOrderApiClient);
    }

}
