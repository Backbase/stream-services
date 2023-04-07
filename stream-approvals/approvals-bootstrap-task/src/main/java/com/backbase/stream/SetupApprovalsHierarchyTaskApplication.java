package com.backbase.stream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SetupApprovalsHierarchyTaskApplication {

  public static void main(String[] args) {
    SpringApplication springApplication =
        new SpringApplication(SetupApprovalsHierarchyTaskApplication.class);
    springApplication.setWebApplicationType(WebApplicationType.NONE);
    springApplication.run(args);
  }
}
