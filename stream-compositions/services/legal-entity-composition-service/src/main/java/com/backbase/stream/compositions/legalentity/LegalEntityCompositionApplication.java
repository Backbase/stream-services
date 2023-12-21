package com.backbase.stream.compositions.legalentity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import reactor.core.publisher.Hooks;

@EnableDiscoveryClient
@SpringBootApplication
//@ComponentScan(
//    value = "com.backbase.stream",
//    excludeFilters = @Filter(type = FilterType.REGEX,
//        pattern = "com\\.backbase\\.stream\\.compositions\\.events\\.egress\\.event\\.spec\\.v1..*")
//)
public class LegalEntityCompositionApplication {

    public static void main(String[] args) {
        SpringApplication.run(LegalEntityCompositionApplication.class, args);
        Hooks.onOperatorDebug();
    }
}
