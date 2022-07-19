package com.backbase.stream.productcatalog.configuration;

import com.backbase.stream.productcatalog.model.ProductCatalog;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "bootstrap")
public class ProductCatalogConfigurationProperties {

    @NotNull
    private ProductCatalog productCatalog;
}
