package com.backbase.stream.config;

import com.backbase.stream.approval.model.Approval;
import com.backbase.stream.legalentity.model.LegalEntity;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bootstrap", ignoreInvalidFields = true)
@Data
@NoArgsConstructor
public class BootstrapConfigurationProperties {

    private Approval approval;

}
