package com.backbase.stream.compositions.legalentity.core.config;

import com.backbase.stream.compositions.integration.legalentity.ApiClient;
import com.backbase.stream.compositions.integration.legalentity.api.LegalEntityIntegrationApi;
import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.WebClient;

import java.text.DateFormat;

@Configuration
@AllArgsConstructor
@EnableConfigurationProperties(LegalEntityConfigurationProperties.class)
public class LegalEntityConfiguration {
    private final LegalEntityConfigurationProperties legalEntityConfigurationProperties;

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

    @Bean
    public LegalEntityMapper mapper() {
        return Mappers.getMapper(LegalEntityMapper.class);
    }

    @Bean
    @Primary
    public LegalEntityIntegrationApi legalEntityIntegrationApi(ApiClient legalEntityClient) {
        return new LegalEntityIntegrationApi(legalEntityClient);
    }

    @Bean
    public ApiClient legalEntityClient(
            WebClient dbsWebClient,
            ObjectMapper objectMapper,
            DateFormat dateFormat) {
        ApiClient apiClient = new ApiClient(dbsWebClient, objectMapper, dateFormat);
        apiClient.setBasePath(legalEntityConfigurationProperties.getLegalEntityIntegrationUrl());

        return apiClient;
    }
}
