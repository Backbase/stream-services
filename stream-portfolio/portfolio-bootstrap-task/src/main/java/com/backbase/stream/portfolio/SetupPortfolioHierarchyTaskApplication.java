package com.backbase.stream.portfolio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SetupPortfolioHierarchyTaskApplication {

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(SetupPortfolioHierarchyTaskApplication.class);
        springApplication.setWebApplicationType(WebApplicationType.NONE);
        springApplication.run(args);
    }

}
