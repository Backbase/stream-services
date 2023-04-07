package com.backbase.stream.compositions.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@EnableDiscoveryClient
@SpringBootApplication
@ComponentScan("com.backbase.stream")
public class TransactionCompositionApplication {
  public static void main(String[] args) {
    SpringApplication.run(TransactionCompositionApplication.class, args);
  }
}
