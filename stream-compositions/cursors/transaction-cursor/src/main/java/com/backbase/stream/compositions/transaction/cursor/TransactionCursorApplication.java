package com.backbase.stream.compositions.transaction.cursor;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableDiscoveryClient
@SpringBootApplication
@ComponentScan(basePackages = {"com.backbase.stream"})
@EntityScan({"com.backbase.stream.compositions.transaction.cursor.core.domain"})
@EnableJpaRepositories({"com.backbase.stream.compositions.transaction.cursor.core.repository"})
public class TransactionCursorApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionCursorApplication.class, args);
    }
}
