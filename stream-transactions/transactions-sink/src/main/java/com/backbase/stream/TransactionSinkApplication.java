package com.backbase.stream;

import com.backbase.stream.config.BackbaseStreamConfigurationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Slf4j
@SpringBootApplication
@EnableConfigurationProperties({BackbaseStreamConfigurationProperties.class})
public class TransactionSinkApplication {

    public static void main(String[] args) {
    SpringApplication.run(TransactionSinkApplication.class, args);
}

}
