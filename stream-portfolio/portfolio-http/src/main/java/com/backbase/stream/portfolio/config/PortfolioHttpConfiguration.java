package com.backbase.stream.portfolio.config;

import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.validation.annotation.Validated;

/**
 * Portfolio Http Configuration.
 *
 * @author Vladimir Kirchev
 */
@Configuration
@Validated
public class PortfolioHttpConfiguration {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http.authorizeExchange().anyExchange().permitAll().and().csrf().disable().build();
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
