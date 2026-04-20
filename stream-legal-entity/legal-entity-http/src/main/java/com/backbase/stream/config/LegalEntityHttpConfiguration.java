package com.backbase.stream.config;

import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.validation.annotation.Validated;

@Configuration
@Validated
@EnableWebFluxSecurity
public class LegalEntityHttpConfiguration {

    @Bean
    @Order(1)
    public SecurityWebFilterChain legalEntitySecurityFilterChain(ServerHttpSecurity http) {
        return http
            .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .build();
    }

    /**
     * To support tracing requests to the services.
     *
     * @return In memory HttpTraceRepository.
     */
    @Bean
    @ConditionalOnExpression("${management.endpoints.enabled-by-default:false} or ${management.trace.http.enabled:false}")
    public InMemoryHttpExchangeRepository httpTraceRepository() {
        return new InMemoryHttpExchangeRepository();
    }

}
