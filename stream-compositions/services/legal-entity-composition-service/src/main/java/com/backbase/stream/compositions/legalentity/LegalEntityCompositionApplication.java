package com.backbase.stream.compositions.legalentity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import reactor.core.publisher.Hooks;

@EnableDiscoveryClient
@SpringBootApplication
@ComponentScan("com.backbase.stream")
public class LegalEntityCompositionApplication {
    public static void main(String[] args) {
        SpringApplication.run(LegalEntityCompositionApplication.class, args);
        Hooks.onOperatorDebug();
    }
}
