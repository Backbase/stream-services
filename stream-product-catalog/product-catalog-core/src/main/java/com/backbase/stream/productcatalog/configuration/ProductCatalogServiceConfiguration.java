package com.backbase.stream.productcatalog.configuration;

import com.backbase.dbs.arrangement.api.service.ApiClient;
import com.backbase.stream.config.BackbaseStreamConfigurationProperties;
import com.backbase.stream.productcatalog.ProductCatalogService;
import com.backbase.stream.productcatalog.ReactiveProductCatalogService;
import com.backbase.stream.webclient.DbsWebClientConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Spring Application Configuration for Product Catalog Template.
 */
@Configuration
@EnableConfigurationProperties(BackbaseStreamConfigurationProperties.class)
@Import(DbsWebClientConfiguration.class)
public class ProductCatalogServiceConfiguration {


    private final BackbaseStreamConfigurationProperties backbaseStreamConfigurationProperties;

    public ProductCatalogServiceConfiguration(
        BackbaseStreamConfigurationProperties backbaseStreamConfigurationProperties) {
        this.backbaseStreamConfigurationProperties = backbaseStreamConfigurationProperties;
    }

    @Bean
    public ReactiveProductCatalogService reactiveProductCatalogService(
        ApiClient accountPresentationClient) {
        return new ReactiveProductCatalogService(accountPresentationClient);
    }

    @Bean
    public ProductCatalogService productCatalogService(ReactiveProductCatalogService reactiveProductCatalogService) {
        return new ProductCatalogService(reactiveProductCatalogService);
    }

    @Bean
    @ConditionalOnMissingBean(ApiClient.class)
    public ApiClient accountPresentationClient(WebClient dbsClient,
        ObjectMapper objectMapper,
        DateFormat dateFormat) {
        ApiClient apiClient = new ApiClient(dbsClient, objectMapper, dateFormat);
        apiClient.setBasePath(backbaseStreamConfigurationProperties.getDbs().getArrangementManagerBaseUrl());
        return apiClient;
    }
}
