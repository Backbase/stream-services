package com.backbase.stream.product.generator.configuration;

import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.iban4j.CountryCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("backbase.stream.generator.product")
@NoArgsConstructor
@Data
public class ProductGeneratorConfigurationProperties {

    /**
     * Default Currency.
     */
    private String defaultCurrency = "EUR";

    /**
     * Country of IBAN Code
     */
    private CountryCode countryCode = CountryCode.NL;


    /**
     * Account Generator Options per Product Type
     */
    private Map<String, ProductKindGeneratorProperties> kinds;

}
