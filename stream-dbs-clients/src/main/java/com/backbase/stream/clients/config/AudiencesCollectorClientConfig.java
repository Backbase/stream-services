package com.backbase.stream.clients.config;

import com.backbase.audiences.collector.api.service.ApiClient;
import com.backbase.audiences.collector.api.service.v1.HandlersServiceApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("backbase.communication.services.user-segments-collector")
public class AudiencesCollectorClientConfig extends CompositeApiClientConfig {

    public static final String USER_SEGMENT_COLLECTOR_SERVICE_ID = "user-segments-collector";

    public AudiencesCollectorClientConfig() {
        super(USER_SEGMENT_COLLECTOR_SERVICE_ID);
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiClient userSegmentCollectorApiClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat)
            .setBasePath(createBasePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public HandlersServiceApi userSegmentCollectorServiceApi(ApiClient apiClient) {
        return new HandlersServiceApi(apiClient);
    }

}
