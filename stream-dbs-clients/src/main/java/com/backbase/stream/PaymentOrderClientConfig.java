package com.backbase.stream;

import com.backbase.buildingblocks.webclient.client.ApiClientConfig;
import com.backbase.dbs.paymentorder.api.service.ApiClient;
import com.backbase.dbs.paymentorder.api.service.v2.PaymentOrdersApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("backbase.communication.services.payment.order")
public class PaymentOrderClientConfig extends ApiClientConfig {

    public static final String PAYMENT_ORDER_SERVICE_ID = "payment-order-service";

    public PaymentOrderClientConfig() {
        super(PAYMENT_ORDER_SERVICE_ID);
    }

    @Bean
    public ApiClient paymentOrderClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat)
            .setBasePath(createBasePath());
    }

    @Bean
    public PaymentOrdersApi paymentOrdersApi(ApiClient paymentOrderClient) {
        return new PaymentOrdersApi(paymentOrderClient);
    }

}
