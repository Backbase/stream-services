package com.backbase.stream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SetupApprovalHierarchyTaskApplication {

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(SetupApprovalHierarchyTaskApplication.class);
        springApplication.setWebApplicationType(WebApplicationType.NONE);
        springApplication.run(args);
    }

}