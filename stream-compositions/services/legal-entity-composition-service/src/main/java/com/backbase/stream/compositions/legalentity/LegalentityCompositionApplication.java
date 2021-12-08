package com.backbase.stream.compositions.legalentity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@EnableDiscoveryClient
@SpringBootApplication
@ComponentScan("com.backbase.stream")
public class LegalentityCompositionApplication {
    public static void main(String[] args) {
        SpringApplication.run(LegalentityCompositionApplication.class, args);
    }
}
