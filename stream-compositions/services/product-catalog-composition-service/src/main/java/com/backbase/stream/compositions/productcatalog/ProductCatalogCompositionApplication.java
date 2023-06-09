package com.backbase.stream.compositions.productcatalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@EnableDiscoveryClient
@SpringBootApplication
@ComponentScan("com.backbase.stream")
public class ProductCatalogCompositionApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductCatalogCompositionApplication.class, args);
    }
}
