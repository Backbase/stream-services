package com.backbase.stream.clients.config;

import com.backbase.portfolio.instrument.integration.api.service.ApiClient;
import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentAssetClassManagementApi;
import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentCountryManagementApi;
import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentManagementApi;
import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentPriceManagementApi;
import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentRegionManagementApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("backbase.communication.services.instrument")
public class InstrumentApiConfiguration extends CompositeApiClientConfig {

    public static final String PORTFOLIO_SERVICE_ID = "portfolio";

    public InstrumentApiConfiguration() {
        super(PORTFOLIO_SERVICE_ID);
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiClient instrumentApiClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat)
            .setBasePath(createBasePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public InstrumentManagementApi instrumentManagementApi(ApiClient instrumentApiClient) {
        return new InstrumentManagementApi(instrumentApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public InstrumentPriceManagementApi instrumentPriceManagementApi(ApiClient instrumentApiClient) {
        return new InstrumentPriceManagementApi(instrumentApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public InstrumentRegionManagementApi instrumentRegionManagementApi(ApiClient instrumentApiClient) {
        return new InstrumentRegionManagementApi(instrumentApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public InstrumentCountryManagementApi instrumentCountryManagementApi(ApiClient instrumentApiClient) {
        return new InstrumentCountryManagementApi(instrumentApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public InstrumentAssetClassManagementApi instrumentAssetClassManagementApi(ApiClient instrumentApiClient) {
        return new InstrumentAssetClassManagementApi(instrumentApiClient);
    }

}
