package com.backbase.stream.configuration;

import com.backbase.customerprofile.api.integration.v1.PartyManagementIntegrationApi;
import com.backbase.stream.clients.config.CustomerProfileClientConfig;
import com.backbase.stream.service.CustomerProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@EnableConfigurationProperties(CustomerProfileClientConfig.class)
public class CustomerProfileConfiguration {

    @Bean
    public CustomerProfileService createCustomerProfileService(
        PartyManagementIntegrationApi partyManagementIntegrationApi) {
        return new CustomerProfileService(partyManagementIntegrationApi);
    }
}
