package com.backbase.stream.clients.config;


import com.backbase.customerprofile.api.integration.ApiClient;
import com.backbase.customerprofile.api.integration.v1.CustomerLifeCycleManagementIntegrationApi;
import com.backbase.customerprofile.api.integration.v1.CustomerManagementIntegrationApi;
import com.backbase.customerprofile.api.integration.v1.CustomerProfileManagementIntegrationApi;
import com.backbase.customerprofile.api.integration.v1.PartyManagementIntegrationApi;
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
    public ApiClient customerProfileApiIntegrationClient(
        ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat)
            .setBasePath(createBasePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public PartyManagementIntegrationApi partyManagementIntegrationApi(
        ApiClient customerProfileApiIntegrationClient) {
        return new PartyManagementIntegrationApi(customerProfileApiIntegrationClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public CustomerManagementIntegrationApi createCustomerManagementIntegrationApi(
        ApiClient customerProfileClientConfig) {
        return new CustomerManagementIntegrationApi(customerProfileClientConfig);
    }

    @Bean
    @ConditionalOnMissingBean
    public CustomerProfileManagementIntegrationApi createCustomerProfileManagementIntegrationApi(
        ApiClient customerProfileClientConfig) {
        return new CustomerProfileManagementIntegrationApi(customerProfileClientConfig);
    }

    @Bean
    @ConditionalOnMissingBean
    public CustomerLifeCycleManagementIntegrationApi createCustomerLifeCycleManagementIntegrationApi(
        ApiClient customerProfileClientConfig) {
        return new CustomerLifeCycleManagementIntegrationApi(customerProfileClientConfig);
    }

}
