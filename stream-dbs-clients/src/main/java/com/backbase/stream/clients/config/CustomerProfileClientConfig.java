package com.backbase.stream.clients.config;


import com.backbase.customerprofile.api.integration.v1.PartyManagementIntegrationApi;
import com.backbase.customerprofile.api.service.ApiClient;
import com.backbase.customerprofile.api.service.v1.CustomerManagementServiceApi;
import com.backbase.customerprofile.api.service.v1.PartyManagementServiceApi;
import com.backbase.customerprofile.api.service.v1.PartyRelationshipsManagementServiceApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("backbase.communication.services.customer-profile")
public class CustomerProfileClientConfig extends CompositeApiClientConfig {

    public static final String CUSTOMER_PROFILE_SERVICE_ID = "customer-profile";

    public CustomerProfileClientConfig() {
        super(CUSTOMER_PROFILE_SERVICE_ID);
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiClient customerProfileApiClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat).setBasePath(createBasePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public com.backbase.customerprofile.api.integration.ApiClient customerProfileApiIntegrationClient(
        ObjectMapper objectMapper, DateFormat dateFormat) {
        return new com.backbase.customerprofile.api.integration.ApiClient(getWebClient(), objectMapper, dateFormat)
            .setBasePath(createBasePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public CustomerManagementServiceApi customerManagementServiceApi(ApiClient customerProfileApiClient) {
        return new CustomerManagementServiceApi(customerProfileApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public PartyManagementServiceApi partyManagementServiceApi(ApiClient customerProfileApiClient) {
        return new PartyManagementServiceApi(customerProfileApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public PartyRelationshipsManagementServiceApi partyRelationshipsManagementServiceApi(
        ApiClient customerProfileApiClient) {
        return new PartyRelationshipsManagementServiceApi(customerProfileApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public PartyManagementIntegrationApi partyManagementIntegrationApi(
        com.backbase.customerprofile.api.integration.ApiClient customerProfileApiIntegrationClient) {
        return new PartyManagementIntegrationApi(customerProfileApiIntegrationClient);
    }


}
