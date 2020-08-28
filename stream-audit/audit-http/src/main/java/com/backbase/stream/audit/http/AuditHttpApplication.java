package com.backbase.stream.audit.http;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class AuditHttpApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditHttpApplication.class, args);
    }
}
