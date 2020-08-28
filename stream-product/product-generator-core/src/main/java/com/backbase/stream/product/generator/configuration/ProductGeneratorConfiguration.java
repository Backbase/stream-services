package com.backbase.stream.product.generator.configuration;

import com.backbase.stream.product.generator.ProductGenerator;
import com.backbase.stream.productcatalog.configuration.ProductCatalogServiceConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(ProductCatalogServiceConfiguration.class)
@EnableConfigurationProperties({ProductGeneratorConfigurationProperties.class})
public class ProductGeneratorConfiguration {

    @Bean
    public ProductGenerator productGenerator(ProductGeneratorConfigurationProperties configurationProperties) {
        return new ProductGenerator(configurationProperties);
    }
}
