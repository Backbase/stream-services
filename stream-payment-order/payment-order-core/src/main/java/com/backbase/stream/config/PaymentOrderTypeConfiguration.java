package com.backbase.stream.config;

import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "backbase.stream.paymentorder")
@Slf4j
@Data
public class PaymentOrderTypeConfiguration {

    private List<String> types;
}
