package com.backbase.stream.compositions.transaction.cursor.core.config;

import com.backbase.stream.compositions.transaction.cursor.core.mapper.TransactionCursorMapper;

import lombok.AllArgsConstructor;

import org.mapstruct.factory.Mappers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/** The Configuration for disabling Security filters */
@Configuration
@AllArgsConstructor
public class TransactionCursorConfiguration {

    @Bean
    public TransactionCursorMapper mapper() {
        return Mappers.getMapper(TransactionCursorMapper.class);
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http.csrf().disable().build();
    }
}
