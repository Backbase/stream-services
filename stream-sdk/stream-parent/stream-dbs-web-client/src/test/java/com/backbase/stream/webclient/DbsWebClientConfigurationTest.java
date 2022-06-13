package com.backbase.stream.webclient;

import static org.assertj.core.api.Assertions.assertThat;

import com.backbase.buildingblocks.webclient.InterServiceWebClientConfiguration;
import com.backbase.buildingblocks.webclient.WebClientConstants;
import com.backbase.stream.webclient.filter.HeadersForwardingClientFilter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.reactive.function.client.WebClient;


@Slf4j
@SpringJUnitConfig
public class DbsWebClientConfigurationTest {

    ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    public void webClientCustomizerTest() {
        contextRunner
            .withBean(WebClientAutoConfiguration.class)
            .withBean(InterServiceWebClientConfiguration.class)
            .withUserConfiguration(DbsWebClientConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(WebClient.class);
                assertThat(context).getBean(WebClientConstants.INTER_SERVICE_WEB_CLIENT_NAME)
                    .extracting("builder.filters")
                    .asList()
                    .anyMatch(HeadersForwardingClientFilter.class::isInstance);
            });
    }

}
