package com.backbase.stream.portfolio.configuration;

import com.backbase.buildingblocks.webclient.WebClientConstants;
import com.backbase.portfolio.instrument.integration.api.service.ApiClient;
import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentAssetClassManagementApi;
import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentCountryManagementApi;
import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentManagementApi;
import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentPriceManagementApi;
import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentRegionManagementApi;
import com.backbase.stream.config.BackbaseStreamConfigurationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(BackbaseStreamConfigurationProperties.class)
@RequiredArgsConstructor
@Slf4j
public class InstrumentApiConfiguration {

    private final BackbaseStreamConfigurationProperties backbaseStreamConfigurationProperties;

    @Bean
    public InstrumentManagementApi instrumentManagementApi(ApiClient instrumentApiClient) {
        return new InstrumentManagementApi(instrumentApiClient);
    }

    @Bean
    public InstrumentPriceManagementApi instrumentPriceManagementApi(ApiClient instrumentApiClient) {
        return new InstrumentPriceManagementApi(instrumentApiClient);
    }

    @Bean
    public InstrumentRegionManagementApi instrumentRegionManagementApi(ApiClient instrumentApiClient) {
        return new InstrumentRegionManagementApi(instrumentApiClient);
    }

    @Bean
    public InstrumentCountryManagementApi instrumentCountryManagementApi(ApiClient instrumentApiClient) {
        return new InstrumentCountryManagementApi(instrumentApiClient);
    }

    @Bean
    public InstrumentAssetClassManagementApi instrumentAssetClassManagementApi(ApiClient instrumentApiClient) {
        return new InstrumentAssetClassManagementApi(instrumentApiClient);
    }

    @Bean
    ApiClient instrumentApiClient(@Qualifier(WebClientConstants.INTER_SERVICE_WEB_CLIENT_NAME) WebClient dbsWebClient,
        ObjectMapper objectMapper, DateFormat dateFormat) {

        ApiClient apiClient = new ApiClient(dbsWebClient, objectMapper, dateFormat);
        log.debug("Instrument Api Client Stream Configuration Properties: {}", backbaseStreamConfigurationProperties);
        apiClient.setBasePath(backbaseStreamConfigurationProperties.getDbs().getPortfolioBaseUrl());
        return apiClient;
    }

}
