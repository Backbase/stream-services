package com.backbase.stream.config;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "backbase.stream.paymentorder")
@Validated
@Slf4j
@Data
public class PaymentOrderTypeConfiguration {

    @NotNull
    private List<String> types;
}
