package com.backbase.stream.compositions.legalentity;

import com.backbase.stream.legalentity.model.LegalEntity;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "backbase.stream.compositions.legal-entity")
public class LegalEntityConfiguration {
    private LegalEntity defaultLegalEntity;
}
