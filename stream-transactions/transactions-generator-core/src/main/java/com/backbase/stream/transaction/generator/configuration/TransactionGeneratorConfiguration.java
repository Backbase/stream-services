package com.backbase.stream.transaction.generator.configuration;

import com.backbase.stream.transaction.generator.TransactionGenerator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TransactionGeneratorOptions.class)
public class TransactionGeneratorConfiguration {

    @Bean
    public TransactionGenerator transactionsDataGenerator(TransactionGeneratorOptions options) {
        return new TransactionGenerator(options);
    }
}
