package com.backbase.stream.compositions.legalentity.core.config;

import com.backbase.stream.legalentity.model.LegalEntity;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@NoArgsConstructor
@ConfigurationProperties(prefix = "bootstrap", ignoreInvalidFields = true)
public class BootstrapConfigurationProperties {
    private Boolean enabled;
    private LegalEntity legalEntity;
}
