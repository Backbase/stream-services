package com.backbase.stream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Hooks;


@SpringBootApplication
public class SetupLegalEntityHierarchyTaskApplication {

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(SetupLegalEntityHierarchyTaskApplication.class);
        springApplication.setWebApplicationType(WebApplicationType.NONE);
        springApplication.run(args);
    }

}
