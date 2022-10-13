package com.backbase.stream.clients.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.backbase.buildingblocks.webclient.InterServiceWebClientConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

public class DbsApiClientConfigTest {

    ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    public void dbsApiClientConfigWithDefaultPortTest() {
        contextRunner
            .withPropertyValues("backbase.communication.http.default-service-port=8080")
            .withBean(WebClientAutoConfiguration.class)
            .withBean(InterServiceWebClientConfiguration.class)
            .withBean(DbsApiClientConfig.class, "banking-service")
            .run(context -> {
                var config = context.getBean(DbsApiClientConfig.class);
                assertEquals("http://banking-service:8080", config.createBasePath());
            });
    }

    @Test
    public void dbsApiClientConfigWithoutDefaultPortTest() {
        contextRunner
            .withBean(WebClientAutoConfiguration.class)
            .withBean(InterServiceWebClientConfiguration.class)
            .withBean(DbsApiClientConfig.class, "banking-service")
            .run(context -> {
                var config = context.getBean(DbsApiClientConfig.class);
                assertEquals("http://banking-service", config.createBasePath());
            });
    }

}
