package com.backbase.stream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * Spring Boot Application which loads Job Profile Templates from a CSV file found in the classpath.
 */
@SpringBootApplication
public class ExportBusinessFunctionsApplication {

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(ExportBusinessFunctionsApplication.class);
        springApplication.setWebApplicationType(WebApplicationType.NONE);
        springApplication.run(args);
    }

}