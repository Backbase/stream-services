package com.backbase.stream.webclient.configuration;

import com.backbase.buildingblocks.webclient.InterServiceWebClientCustomizer;
import com.backbase.stream.webclient.filter.HeaderForwardingClientFilter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.logging.LogLevel;
import java.text.DateFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;


/**
 * DBS Web Client Configuration to be used by Stream Services that communicate with DBS. We are overriding here the
 * default token converter port in the URI to be compliant with most clustered environments.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(DbsWebClientConfigurationProperties.class)
public class DbsWebClientConfiguration {

    /**
     * Add customizer to the SSDK's Web Client to include extra headers.
     *
     * @param properties .
     * @return .
     */
    @Bean
    public InterServiceWebClientCustomizer webClientCustomizer(DbsWebClientConfigurationProperties properties) {
        return webClientBuilder -> webClientBuilder.filter(new HeaderForwardingClientFilter(properties));
    }

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper legacyObjectMapper(DateFormat dateFormat) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setDateFormat(dateFormat);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    /**
     * Enhance logging for http requests on debug level.
     *
     * @return .
     */
    @Bean
    public InterServiceWebClientCustomizer loggingWebClientCustomizer() {
        return webClientBuilder -> webClientBuilder.clientConnector(
            new ReactorClientHttpConnector(
                HttpClient.create().wiretap(HttpClient.class.getCanonicalName(), LogLevel.DEBUG,
                    AdvancedByteBufFormat.TEXTUAL)));
    }

}
