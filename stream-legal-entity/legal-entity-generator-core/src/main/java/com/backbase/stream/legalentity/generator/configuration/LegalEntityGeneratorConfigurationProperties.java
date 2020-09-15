package com.backbase.stream.legalentity.generator.configuration;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "backbase.stream.generator.legal-entity")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LegalEntityGeneratorConfigurationProperties {

    /**
     * Generate Retail Customers.
     */
    private boolean generateRetailCustomers;

    /**
     * Generate Legal Entity using Locale.
     */
    private Locale locale = Locale.ENGLISH;

    /**
     * Parent Legal Entity ID.
     */
    private String parentLegalEntityId = "bank";

    /**
     * Number of product groups (data groups) to be created to each legal entity
     */
    private Map<Integer, Double> distribution = Collections.singletonMap(1, 1d);

    /**
     * Generate Products for Legal Entity.
     */
    private boolean generateProducts = true;

    /**
     * Percentage of legal entites created with deliberate errors
     */
    private Double errorRate = 0d;

}
