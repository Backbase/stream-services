package com.backbase.stream.product;

import com.backbase.stream.product.configuration.ProductConfiguration;
import com.backbase.stream.product.configuration.ProductIngestionSagaConfigurationProperties;
import com.backbase.stream.product.service.ArrangementService;
import com.backbase.stream.service.AccessGroupService;
import com.backbase.stream.service.UserService;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(ProductConfiguration.class)
@EnableConfigurationProperties(ProductIngestionSagaConfigurationProperties.class)
public class ProductIngestionSagaConfiguration {

    @Bean
    public ProductIngestionSaga productIngestionSaga(
            ArrangementService arrangementService,
            AccessGroupService accessGroupService,
            UserService userService,
            ProductIngestionSagaConfigurationProperties configurationProperties) {
        return new ProductIngestionSaga(
                arrangementService, accessGroupService, userService, configurationProperties);
    }

    @Bean
    public BatchProductIngestionSaga batchProductIngestionSaga(
            ArrangementService arrangementService,
            AccessGroupService accessGroupService,
            UserService userService,
            ProductIngestionSagaConfigurationProperties configurationProperties) {
        return new BatchProductIngestionSaga(
                arrangementService, accessGroupService, userService, configurationProperties);
    }
}
