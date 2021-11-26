package com.backbase.stream.compositions.legalentity;

import com.backbase.stream.compositions.configuration.IntegrationConfigurationProperties;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
@EnableConfigurationProperties(IntegrationConfigurationProperties.class)
public class LegalEntityIntegrationConfiguration {
    private final IntegrationConfigurationProperties integrationConfigurationProperties;
}
