package com.backbase.stream.legalentity.sink;

import com.backbase.stream.configuration.LegalEntitySagaConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Slf4j
@SpringBootApplication
@EnableConfigurationProperties({LegalEntitySagaConfiguration.class})
public class LegalEntitySinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(LegalEntitySinkApplication.class, args);
    }

}
