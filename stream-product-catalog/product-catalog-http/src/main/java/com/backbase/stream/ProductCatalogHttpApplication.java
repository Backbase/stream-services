package com.backbase.stream;

import com.backbase.stream.productcatalog.configuration.ProductCatalogServiceConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/** Spring Boot Application offering a REST interface for Product Catalog Ingestion Service. */
@SpringBootApplication
@ImportAutoConfiguration({ProductCatalogServiceConfiguration.class})
public class ProductCatalogHttpApplication {

  public static void main(String[] args) {
    SpringApplication.run(ProductCatalogHttpApplication.class, args);
  }
}

@Configuration
class ProductCatalogHttpApplicationConfiguration {

  @Bean
  public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
    return http.authorizeExchange().anyExchange().permitAll().and().csrf().disable().build();
  }
}
