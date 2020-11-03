package com.backbase.stream.account.integration.orchestrator.configuration;

import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties(prefix = "backbase.stream.account-integration")
@Validated
public class AccountIntegrationOrchestratorConfigurationProperties {

    @NotNull
    private String defaultAccountIntegrationBaseUrl;

    @NotNull
    private Map<String, String> productTypeAccountIntegrationBaseUrl;
}
