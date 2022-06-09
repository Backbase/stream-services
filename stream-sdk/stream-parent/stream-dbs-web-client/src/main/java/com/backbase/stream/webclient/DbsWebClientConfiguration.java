package com.backbase.stream.webclient;

import com.backbase.buildingblocks.webclient.InterServiceWebClientCustomizer;
import com.backbase.stream.webclient.configuration.DbsWebClientConfigurationProperties;
import com.backbase.stream.webclient.filter.HeadersForwardingClientFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * DBS Web Client Configuration to be used by Stream Services that communicate with DBS.
 */
@Configuration
@EnableConfigurationProperties(DbsWebClientConfigurationProperties.class)
@Slf4j
public class DbsWebClientConfiguration {

    public static final String CONTEXT_KEY_FORWARDED_HEADERS = "headers";


    /**
     * Default Reactive Web Client to be used when interacting with DBS Services. Requires OAuth2 client credentials set
     * in application.yml
     *
     * @return Preconfigured Web Client
     */
    @Bean
    public InterServiceWebClientCustomizer webClientCustomizer(
        DbsWebClientConfigurationProperties dbsWebClientConfigurationProperties) {
        return webClientBuilder -> webClientBuilder.filter(
            new HeadersForwardingClientFilter(dbsWebClientConfigurationProperties));
    }


}
