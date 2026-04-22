package com.backbase.stream.compositions.transaction.cursor.core.config;

import com.backbase.stream.compositions.transaction.cursor.core.mapper.TransactionCursorMapper;
import lombok.AllArgsConstructor;
import org.mapstruct.factory.Mappers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * The Configuration for disabling Security filters
 */
@Configuration
@AllArgsConstructor
public class TransactionCursorConfiguration {

    @Bean
    public TransactionCursorMapper transactionCursorMapper() {
        return Mappers.getMapper(TransactionCursorMapper.class);
    }

    @Bean
    @Order(1)
    public SecurityWebFilterChain transactionCursorSecurityFilterChain(ServerHttpSecurity http) {
        return http.authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .build();
    }
}
