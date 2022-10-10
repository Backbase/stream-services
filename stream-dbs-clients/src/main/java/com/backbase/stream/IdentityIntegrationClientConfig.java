package com.backbase.stream;

import com.backbase.buildingblocks.webclient.client.ApiClientConfig;
import com.backbase.identity.integration.api.service.ApiClient;
import com.backbase.identity.integration.api.service.v1.IdentityIntegrationServiceApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("backbase.communication.services.identity.integration")
@ConditionalOnProperty(value = "backbase.stream.legalentity.sink.use-identity-integration")
public class IdentityIntegrationClientConfig extends ApiClientConfig {

    public static final String IDENTITY_INTEGRATION_SERVICE_ID = "identity-integration-service";

    public IdentityIntegrationClientConfig() {
        super(IDENTITY_INTEGRATION_SERVICE_ID);
    }

    @Bean
    public ApiClient identityApiClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat)
            .setBasePath(createBasePath());
    }

    @Bean
    public IdentityIntegrationServiceApi identityIntegrationServiceApi(ApiClient identityApiClient) {
        return new IdentityIntegrationServiceApi(identityApiClient);
    }

}
