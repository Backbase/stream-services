package com.backbase.stream.clients.config;

import com.backbase.dbs.limit.api.service.ApiClient;
import com.backbase.dbs.limit.api.service.v2.LimitsServiceApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("backbase.communication.services.limit")
public class LimitsClientConfig extends CompositeApiClientConfig {

    public static final String LIMITS_SERVICE_ID = "limit";

    public LimitsClientConfig() {
        super(LIMITS_SERVICE_ID);
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiClient limitApiClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat).setBasePath(createBasePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public LimitsServiceApi limitsServiceApi(ApiClient limitApiClient) {
        return new LimitsServiceApi(limitApiClient);
    }
}
