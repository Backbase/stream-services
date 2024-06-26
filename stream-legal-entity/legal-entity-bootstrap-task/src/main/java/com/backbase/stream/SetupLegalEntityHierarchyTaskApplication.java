package com.backbase.stream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import reactor.core.publisher.Hooks;


@Slf4j
@SpringBootApplication
public class SetupLegalEntityHierarchyTaskApplication {

    public static void main(String[] args) {

        if (log.isDebugEnabled()) {
          Hooks.onOperatorDebug();
        }

        SpringApplication springApplication = new SpringApplication(SetupLegalEntityHierarchyTaskApplication.class);
        springApplication.setWebApplicationType(WebApplicationType.NONE);
        springApplication.run(args);
    }

}