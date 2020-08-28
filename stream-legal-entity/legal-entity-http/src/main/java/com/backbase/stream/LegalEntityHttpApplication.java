package com.backbase.stream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@SpringBootApplication
public class LegalEntityHttpApplication {

    public static void main(String[] args) {
        SpringApplication.run(LegalEntityHttpApplication.class, args);
    }
}

@Configuration
class LegalEntityHttpConfiguration {
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http.authorizeExchange()
            .anyExchange()
            .permitAll()
            .and()
            .csrf()
            .disable()
            .build();
    }

}
