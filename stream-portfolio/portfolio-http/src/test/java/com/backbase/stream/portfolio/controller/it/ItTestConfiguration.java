package com.backbase.stream.portfolio.controller.it;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * IT Test Configuration.
 * 
 * @author Vladimir Kirchev
 *
 */
@TestConfiguration
class ItTestConfiguration {
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
