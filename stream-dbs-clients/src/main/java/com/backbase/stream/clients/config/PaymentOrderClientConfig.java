package com.backbase.stream.clients.config;

import com.backbase.dbs.paymentorder.api.service.ApiClient;
import com.backbase.dbs.paymentorder.api.service.v2.PaymentOrdersApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("backbase.communication.services.payment.order")
public class PaymentOrderClientConfig extends CompositeApiClientConfig {

    public static final String PAYMENT_ORDER_SERVICE_ID = "payment-order-service";

    public PaymentOrderClientConfig() {
        super(PAYMENT_ORDER_SERVICE_ID);
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiClient paymentOrderClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat).setBasePath(createBasePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public PaymentOrdersApi paymentOrdersApi(ApiClient paymentOrderClient) {
        return new PaymentOrdersApi(paymentOrderClient);
    }
}
