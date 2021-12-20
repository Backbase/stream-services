package com.backbase.stream.compositions.product.core.config;

import com.backbase.stream.legalentity.model.ProductGroup;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@NoArgsConstructor
@ConfigurationProperties(prefix = "bootstrap", ignoreInvalidFields = true)
public class BootstrapConfigurationProperties {
    private ProductGroup productCatalog;
}
