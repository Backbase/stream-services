package com.backbase.stream.compositions.paymentorders.core.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@NoArgsConstructor
@ConfigurationProperties("backbase.stream.compositions.paymentorder")
public class PaymentOrderConfigurationProperties {

    private String integrationBaseUrl = "http://payment-orders-ingestion-integration:8080";
}
