package com.backbase.stream.compositions.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@EnableDiscoveryClient
@SpringBootApplication
@ComponentScan("com.backbase.stream")
public class ProductCompositionApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductCompositionApplication.class, args);
    }
}
