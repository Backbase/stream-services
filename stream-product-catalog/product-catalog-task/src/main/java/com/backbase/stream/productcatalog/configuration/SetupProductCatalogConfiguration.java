package com.backbase.stream.productcatalog.configuration;

import com.backbase.stream.productcatalog.ProductCatalogService;
import com.backbase.stream.productcatalog.model.ProductCatalog;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Example Task that setup Product Catalog in DBS. Please change and adapt for your own project. Or
 * change to read product from a CSV file.
 */
@EnableTask
@Configuration
@AllArgsConstructor
@Slf4j
@EnableConfigurationProperties(ProductCatalogConfigurationProperties.class)
public class SetupProductCatalogConfiguration {

  private final ProductCatalogService productCatalogService;
  private final ProductCatalogConfigurationProperties productCatalogConfigurationProperties;

  /**
   * Command Line Runner which terminates the Spring Boot Application on Completion.
   *
   * @return Statistics of how many items are created so it can be tracked by Spring Cloud Data Flow
   */
  @Bean
  public CommandLineRunner commandLineRunner() {
    return args -> {
      ProductCatalog productCatalog = productCatalogConfigurationProperties.getProductCatalog();
      log.info("Setting up Product Catalog");
      productCatalogService.setupProductCatalog(productCatalog);
      log.info("Finished setting up Product Catalog");
    };
  }
}
