package com.backbase.stream.config;

import com.backbase.stream.legalentity.model.LegalEntity;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@NoArgsConstructor
@ConfigurationProperties(prefix = "bootstrap", ignoreInvalidFields = true)
public class BootstrapConfigurationProperties {

  @NotNull private LegalEntity legalEntity;
}
