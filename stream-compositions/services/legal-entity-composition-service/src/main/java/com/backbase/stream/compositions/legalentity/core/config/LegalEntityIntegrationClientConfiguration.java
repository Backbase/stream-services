package com.backbase.stream.compositions.legalentity.core.config;

import com.backbase.stream.clients.config.CompositeApiClientConfig;
import com.backbase.stream.compositions.legalentity.integration.ApiClient;
import com.backbase.stream.compositions.legalentity.integration.client.LegalEntityIntegrationApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConfigurationProperties("backbase.communication.services.stream.legal-entity.integration")
public class LegalEntityIntegrationClientConfiguration extends CompositeApiClientConfig {

    public static final String SERVICE_ID = "legal-entity-integration";

    public LegalEntityIntegrationClientConfiguration() {
        super(SERVICE_ID);
    }

    @Bean
    public ApiClient legalEntityClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat)
            .setBasePath(createBasePath());
    }

    @Bean
    @Primary
    public LegalEntityIntegrationApi legalEntityIntegrationApi(ApiClient legalEntityClient) {
        return new LegalEntityIntegrationApi(legalEntityClient);
    }

}
