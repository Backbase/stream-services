package com.backbase.stream.worker.configuration;

import brave.http.HttpResponseParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.instrument.web.HttpServerResponseParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpResponse;

@Slf4j
@Configuration
public class TraceConfiguration {

    @Bean
    @HttpServerResponseParser
    public HttpResponseParser httpResponseParser() {
        return (response, context, span) -> {
            Object unwrap = response.unwrap();
            if (unwrap instanceof ServerHttpResponse) {
                log.debug("Creating sleuth headers in response.");
                ServerHttpResponse resp = (ServerHttpResponse) unwrap;
                resp.getHeaders().add("X-B3-TraceId", context.traceIdString());
            } else {
                log.warn("Skipping sleuth headers creation due to unsupported response type.");
            }
        };
    }

}
