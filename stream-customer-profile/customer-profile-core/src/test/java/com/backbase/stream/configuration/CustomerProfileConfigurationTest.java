package com.backbase.stream.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.backbase.buildingblocks.webclient.InterServiceWebClientConfiguration;
import com.backbase.customerprofile.api.integration.v1.PartyManagementIntegrationApi;
import com.backbase.stream.clients.config.CustomerProfileClientConfig;
import com.backbase.stream.mapper.PartyMapper;
import com.backbase.stream.service.CustomerProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.reactive.function.client.WebClient;

@SpringJUnitConfig
class CustomerProfileConfigurationTest {

    ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    void configurationTest() {
        contextRunner
            .withBean(WebClientAutoConfiguration.class)
            .withBean(InterServiceWebClientConfiguration.class)
            .withBean(PartyManagementIntegrationApi.class, () -> mock(PartyManagementIntegrationApi.class))
            .withBean(PartyMapper.class, () -> mock(PartyMapper.class))
            .withUserConfiguration(CustomerProfileConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(CustomerProfileService.class);
                assertThat(context).hasSingleBean(PartyManagementIntegrationApi.class);
                assertThat(context).hasSingleBean(WebClient.class);
                assertThat(context).hasSingleBean(CustomerProfileClientConfig.class);
                assertThat(context).hasSingleBean(PartyMapper.class);
            });
    }
}