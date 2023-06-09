package com.backbase.stream.compositions.productcatalog.core.service;

import com.backbase.stream.compositions.integration.productcatalog.model.ProductCatalog;
import com.backbase.stream.compositions.productcatalog.core.model.ProductCatalogIngestPullRequest;
import reactor.core.publisher.Mono;

public interface ProductCatalogIntegrationService {

  /**
   * Retrieve product catalog from integration service.
   *
   * @return Product catalog
   */
  Mono<ProductCatalog> pullProductCatalog(ProductCatalogIngestPullRequest ingestPullRequest);
}
