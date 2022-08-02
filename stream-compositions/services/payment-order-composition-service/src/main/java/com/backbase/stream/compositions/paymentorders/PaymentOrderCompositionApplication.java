package com.backbase.stream.compositions.paymentorders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@EnableDiscoveryClient
@SpringBootApplication
@ComponentScan("com.backbase.stream")
public class PaymentOrderCompositionApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentOrderCompositionApplication.class, args);
    }
}
