package com.backbase.stream.legalentity.generator.configuration;

import java.time.temporal.ChronoUnit;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Legal Entity Generator Options.
 */
@ConfigurationProperties("backbase.stream.generator.legal-entity.source")
@Data
public class LegalEntityGeneratorSourceConfigurationProperties {

    /**
     * Time Unit for triggering generating Legal Entities.
     */
    private ChronoUnit timeUnit = ChronoUnit.SECONDS;

    /**
     * Delay period between creating Legal Entities.
     */
    private long delay = 1;

    /**
     * Number of Legal Entities to generate per trigger.
     */
    private int numberOfLegalEntitiesToGeneratePerTrigger = 1;

}
