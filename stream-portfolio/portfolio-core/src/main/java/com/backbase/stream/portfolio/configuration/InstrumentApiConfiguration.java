package com.backbase.stream.portfolio.configuration;

import com.backbase.buildingblocks.webclient.client.ApiClientConfig;
import com.backbase.portfolio.instrument.integration.api.service.ApiClient;
import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentAssetClassManagementApi;
import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentCountryManagementApi;
import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentManagementApi;
import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentPriceManagementApi;
import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentRegionManagementApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@ConfigurationProperties("backbase.communication.services.instrument")
public class InstrumentApiConfiguration extends ApiClientConfig {

    public static final String PORTFOLIO_SERVICE_ID = "portfolio";

    public InstrumentApiConfiguration() {
        super(PORTFOLIO_SERVICE_ID);
    }

    @Bean
    public ApiClient instrumentApiClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat)
            .setBasePath(createBasePath());
    }

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

}
