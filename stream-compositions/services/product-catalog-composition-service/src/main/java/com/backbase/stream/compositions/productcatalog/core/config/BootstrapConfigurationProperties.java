package com.backbase.stream.compositions.productcatalog.core.config;

import com.backbase.stream.productcatalog.model.ProductCatalog;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@NoArgsConstructor
@ConfigurationProperties(prefix = "bootstrap", ignoreInvalidFields = true)
public class BootstrapConfigurationProperties {
    private ProductCatalog productCatalog;
}
