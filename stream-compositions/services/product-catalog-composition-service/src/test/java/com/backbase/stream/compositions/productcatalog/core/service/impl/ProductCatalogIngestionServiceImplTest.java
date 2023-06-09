package com.backbase.stream.compositions.productcatalog.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.backbase.stream.compositions.integration.productcatalog.model.ProductCatalog;
import com.backbase.stream.compositions.productcatalog.core.model.ProductCatalogIngestPullRequest;
import com.backbase.stream.compositions.productcatalog.core.model.ProductCatalogIngestPushRequest;
import com.backbase.stream.compositions.productcatalog.core.model.ProductCatalogIngestResponse;
import com.backbase.stream.compositions.productcatalog.core.service.ProductCatalogIngestionService;
import com.backbase.stream.compositions.productcatalog.core.service.ProductCatalogIntegrationService;
import com.backbase.stream.compositions.productcatalog.mapper.ProductCatalogMapper;
import com.backbase.stream.productcatalog.ReactiveProductCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ProductCatalogIngestionServiceImplTest {

  @Mock ReactiveProductCatalogService reactiveProductCatalogService;
  @Mock ProductCatalogMapper mapper;
  private ProductCatalogIngestionService productCatalogIngestionService;
  @Mock private ProductCatalogIntegrationService productCatalogIntegrationService;

  @BeforeEach
  void setUp() {
    productCatalogIngestionService =
        new ProductCatalogIngestionServiceImpl(
            mapper, reactiveProductCatalogService, productCatalogIntegrationService);
  }

  @Test
  void ingestionInPullMode_Success() {
    when(mapper.mapIntegrationToStream(any()))
        .thenReturn(new com.backbase.stream.productcatalog.model.ProductCatalog());

    when(productCatalogIntegrationService.pullProductCatalog(any()))
        .thenReturn(Mono.just(new ProductCatalog()));

    // when(reactiveProductCatalogService.setupProductCatalog(any())).thenReturn(Mono.just(new
    // com.backbase.stream.productcatalog.model.ProductCatalog()));
    when(reactiveProductCatalogService.upsertProductCatalog(any()))
        .thenReturn(Mono.just(new com.backbase.stream.productcatalog.model.ProductCatalog()));

    ProductCatalogIngestPullRequest request = ProductCatalogIngestPullRequest.builder().build();
    ProductCatalogIngestResponse productCatalogIngestResponse =
        productCatalogIngestionService.ingestPull(Mono.just(request)).block();
    assertNotNull(productCatalogIngestResponse.getProductCatalog());
  }

  @Test
  void ingestionInPushMode_Unsupported() {
    Mono<ProductCatalogIngestPushRequest> request =
        Mono.just(ProductCatalogIngestPushRequest.builder().build());
    assertThrows(
        UnsupportedOperationException.class,
        () -> {
          productCatalogIngestionService.ingestPush(request);
        });
  }
}
