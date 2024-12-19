package com.backbase.stream.compositions.productcatalog.core.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@NoArgsConstructor
@ConfigurationProperties("backbase.stream.compositions.product-catalog")
public class ProductCatalogConfigurationProperties {

    private Boolean enableCompletedEvents = true;
    private Boolean enableFailedEvents = true;
}
