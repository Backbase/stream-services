package com.backbase.stream.compositions.product.core.config;

import com.backbase.stream.legalentity.model.ProductGroup;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@NoArgsConstructor
@ConfigurationProperties(prefix = "bootstrap", ignoreInvalidFields = true)
public class BootstrapConfigurationProperties {
    private ProductGroup productCatalog;
}
