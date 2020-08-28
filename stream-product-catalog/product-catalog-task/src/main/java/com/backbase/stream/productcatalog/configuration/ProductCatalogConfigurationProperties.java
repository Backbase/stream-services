package com.backbase.stream.productcatalog.configuration;

import com.backbase.stream.productcatalog.model.ProductCatalog;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bootstrap")
@Data
public class ProductCatalogConfigurationProperties {

    private ProductCatalog productCatalog;
}
