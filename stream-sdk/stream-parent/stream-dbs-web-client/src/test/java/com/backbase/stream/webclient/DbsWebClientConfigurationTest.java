package com.backbase.stream.webclient;

import com.backbase.buildingblocks.webclient.InterServiceWebClientConfiguration;
import com.backbase.stream.webclient.filter.HeadersForwardingClientFilter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;


@Slf4j
@Disabled
@ExtendWith(SpringExtension.class)
public class DbsWebClientConfigurationTest {

    ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    public void webClientCustomizerTest() {
        contextRunner
            .withBean(WebClientAutoConfiguration.class)
            .withBean(InterServiceWebClientConfiguration.class)
            .withUserConfiguration(DbsWebClientConfiguration.class)
            .run(context -> {
                WebClient.Builder builder = context.getBean(WebClient.Builder.class);
                builder.filters(
                    filters -> {
                        var value = filters.stream()
                            .anyMatch(f -> f.getClass().isAssignableFrom(HeadersForwardingClientFilter.class));
                        Assertions.assertTrue(value);
                    });
            });
    }

}
