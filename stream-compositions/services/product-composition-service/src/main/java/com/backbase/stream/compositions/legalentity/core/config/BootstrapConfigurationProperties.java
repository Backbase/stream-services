package com.backbase.stream.compositions.legalentity.core.config;

import com.backbase.stream.productcatalog.model.ProductCatalog;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@NoArgsConstructor
@ConfigurationProperties(prefix = "bootstrap", ignoreInvalidFields = true)
public class BootstrapConfigurationProperties {
    private ProductCatalog productCatalog;
}
