package com.backbase.stream.productcatalog.configuration;

import com.backbase.dbs.arrangement.api.service.v2.ProductKindsApi;
import com.backbase.dbs.arrangement.api.service.v2.ProductsApi;
import com.backbase.stream.productcatalog.ProductCatalogService;
import com.backbase.stream.productcatalog.ReactiveProductCatalogService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Spring Application Configuration for Product Catalog Template. */
@Configuration
public class ProductCatalogServiceConfiguration {

  @Bean
  public ReactiveProductCatalogService reactiveProductCatalogService(
      ProductsApi productsApi, ProductKindsApi productKindsApi) {
    return new ReactiveProductCatalogService(productsApi, productKindsApi);
  }

  @Bean
  public ProductCatalogService productCatalogService(
      ReactiveProductCatalogService reactiveProductCatalogService) {
    return new ProductCatalogService(reactiveProductCatalogService);
  }
}
