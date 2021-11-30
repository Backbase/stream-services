package com.backbase.stream.compositions.legalentity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.backbase")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
