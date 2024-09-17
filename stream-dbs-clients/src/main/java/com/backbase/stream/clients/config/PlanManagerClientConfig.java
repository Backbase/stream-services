package com.backbase.stream.clients.config;

import com.backbase.tailoredvalue.planmanager.service.api.ApiClient;
import com.backbase.tailoredvalue.planmanager.service.api.v0.PlansApi;
import com.backbase.tailoredvalue.planmanager.service.api.v0.UserPlansApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.DateFormat;

@Configuration
@ConfigurationProperties("backbase.communication.services.plan.manager")
public class PlanManagerClientConfig extends CompositeApiClientConfig {

    public static final String PLAN_MANAGER_SERVICE_ID = "plan-manager";

    public PlanManagerClientConfig() {
        super(PLAN_MANAGER_SERVICE_ID);
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiClient planManagerApiClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat)
                .setBasePath(createBasePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public PlansApi plansApi(ApiClient apiClient) {
        return new PlansApi(apiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public UserPlansApi userPlansApi(ApiClient apiClient) {
        return new UserPlansApi(apiClient);
    }

}
