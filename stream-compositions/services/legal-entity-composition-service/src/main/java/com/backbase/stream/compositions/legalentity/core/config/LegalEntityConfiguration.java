package com.backbase.stream.compositions.legalentity.core.config;

import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapper;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "backbase.stream.compositions.legal-entity")
public class LegalEntityConfiguration {
    @Bean
    public LegalEntityMapper mapper() {
        return Mappers.getMapper(LegalEntityMapper.class);
    }
}
