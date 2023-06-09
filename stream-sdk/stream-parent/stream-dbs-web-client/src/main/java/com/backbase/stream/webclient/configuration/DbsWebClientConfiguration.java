package com.backbase.stream.webclient.configuration;

import com.backbase.buildingblocks.webclient.InterServiceWebClientCustomizer;
import com.backbase.stream.webclient.filter.HeadersForwardingClientFilter;
import com.backbase.stream.webclient.filter.HeadersForwardingServerFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DBS Web Client Configuration to be used by Stream Services that communicate with DBS.
 *
 * <p>We are overriding here the default token converter port in the URI to be compliant with most
 * clustered environments.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(DbsWebClientConfigurationProperties.class)
public class DbsWebClientConfiguration {

    public static final String CONTEXT_KEY_FORWARDED_HEADERS = "headers";

    /**
     * Adds reactive server filter to chain.
     *
     * @param properties Dbs webclient properties
     * @return HeadersForwardingServerFilter
     */
    @Bean
    public HeadersForwardingServerFilter headersForwardingServerFilter(
        DbsWebClientConfigurationProperties properties) {
        return new HeadersForwardingServerFilter(properties);
    }

    /**
     * Add customizer to the SSDK's Web Client to include extra headers.
     *
     * @param properties Dbs webclient properties
     * @return InterServiceWebClientCustomizer
     */
    @Bean
    public InterServiceWebClientCustomizer webClientCustomizer(
        DbsWebClientConfigurationProperties properties) {
        return webClientBuilder ->
            webClientBuilder.filter(new HeadersForwardingClientFilter(properties));
    }
}
