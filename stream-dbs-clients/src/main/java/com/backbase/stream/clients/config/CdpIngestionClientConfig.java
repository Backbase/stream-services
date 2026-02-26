package com.backbase.stream.clients.config;

import com.backbase.cdp.ingestion.api.service.ApiClient;
import com.backbase.cdp.ingestion.api.service.v1.CdpApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("backbase.communication.services.cdp-ingestion")
public class CdpIngestionClientConfig extends CompositeApiClientConfig {

    public static final String CDP_INGESTION_SERVICE_ID = "cdp-ingestion";

    public CdpIngestionClientConfig() {
        super(CDP_INGESTION_SERVICE_ID);
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiClient cdpIngestionApiClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat)
            .setBasePath(createBasePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public CdpApi cdpIngestionServiceApi(ApiClient apiClient) {
        return new CdpApi(apiClient);
    }

}
