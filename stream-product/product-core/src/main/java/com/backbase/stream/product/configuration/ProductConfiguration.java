package com.backbase.stream.product.configuration;


import com.backbase.buildingblocks.webclient.WebClientConstants;
import com.backbase.dbs.arrangement.api.service.ApiClient;
import com.backbase.dbs.arrangement.api.service.v2.ArrangementsApi;
import com.backbase.stream.config.BackbaseStreamConfigurationProperties;
import com.backbase.stream.product.service.ArrangementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Product  Configuration.
 */
@Configuration
@EnableConfigurationProperties(BackbaseStreamConfigurationProperties.class)
@AllArgsConstructor
public class ProductConfiguration {

    private final BackbaseStreamConfigurationProperties backbaseStreamConfigurationProperties;

    @Bean
    public ArrangementService arrangementService(ApiClient accountsApiClient) {
        return new ArrangementService(new ArrangementsApi(accountsApiClient));
    }

    @Bean
    public com.backbase.dbs.arrangement.api.service.ApiClient accountsApiClient(
        @Qualifier(WebClientConstants.INTER_SERVICE_WEB_CLIENT_NAME) WebClient dbsWebClient,
        ObjectMapper objectMapper,
        DateFormat dateFormat) {
        com.backbase.dbs.arrangement.api.service.ApiClient apiClient =
            new com.backbase.dbs.arrangement.api.service.ApiClient(
                dbsWebClient, objectMapper, dateFormat);
        apiClient.setBasePath(backbaseStreamConfigurationProperties.getDbs().getArrangementManagerBaseUrl());
        return apiClient;
    }


}
