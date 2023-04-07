package com.backbase.stream.productcatalog.configuration;

import com.backbase.stream.productcatalog.model.ProductCatalog;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@Data
@Validated
@ConfigurationProperties(prefix = "bootstrap")
public class ProductCatalogConfigurationProperties {

    @NotNull private ProductCatalog productCatalog;
}
