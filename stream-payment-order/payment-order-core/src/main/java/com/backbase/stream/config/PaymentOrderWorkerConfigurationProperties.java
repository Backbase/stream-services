package com.backbase.stream.config;

import com.backbase.stream.worker.configuration.StreamWorkerConfiguration;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "backbase.stream.paymentorder.worker")
@Slf4j
@Data
public class PaymentOrderWorkerConfigurationProperties extends StreamWorkerConfiguration {

    private boolean groupPerArrangementId;

    private boolean continueOnError;

    private boolean deletePaymentOrder = false;
}
