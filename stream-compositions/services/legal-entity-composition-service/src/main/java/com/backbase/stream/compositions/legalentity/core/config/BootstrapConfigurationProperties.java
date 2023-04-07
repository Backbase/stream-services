package com.backbase.stream.compositions.legalentity.core.config;

import com.backbase.stream.legalentity.model.LegalEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@NoArgsConstructor
@ConfigurationProperties(prefix = "bootstrap", ignoreInvalidFields = true)
public class BootstrapConfigurationProperties {

    private Boolean enabled;
    private LegalEntity legalEntity;
}
