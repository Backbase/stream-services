package com.backbase.stream;

import com.backbase.stream.productcatalog.configuration.ProductCatalogServiceConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot Application offering a REST interface for Product Catalog Ingestion Service.
 */
@SpringBootApplication
@ImportAutoConfiguration({ProductCatalogServiceConfiguration.class})
public class ProductCatalogHttpApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductCatalogHttpApplication.class, args);
    }

}


