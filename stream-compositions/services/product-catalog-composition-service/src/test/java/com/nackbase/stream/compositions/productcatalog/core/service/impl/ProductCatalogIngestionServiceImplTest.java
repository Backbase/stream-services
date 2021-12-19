package com.nackbase.stream.compositions.productcatalog.core.service.impl;

import com.backbase.stream.compositions.integration.productcatalog.model.ProductCatalog;
import com.backbase.stream.compositions.productcatalog.core.model.ProductCatalogIngestPushRequest;
import com.backbase.stream.compositions.productcatalog.core.model.ProductCatalogIngestResponse;
import com.backbase.stream.compositions.productcatalog.core.service.ProductCatalogIngestionService;
import com.backbase.stream.compositions.productcatalog.core.service.ProductCatalogIntegrationService;
import com.backbase.stream.compositions.productcatalog.core.service.impl.ProductCatalogIngestionServiceImpl;
import com.backbase.stream.compositions.productcatalog.mapper.ProductCatalogMapper;
import com.backbase.stream.compositions.productcatalog.mapper.ProductCatalogMapperImpl;
import com.backbase.stream.productcatalog.ReactiveProductCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProductCatalogIngestionServiceImplTest {
    private ProductCatalogIngestionService productCatalogIngestionService;

    @Mock
    ReactiveProductCatalogService reactiveProductCatalogService;

    @Mock
    private ProductCatalogIntegrationService productCatalogIntegrationService;

    @Mock
    ProductCatalogMapper mapper;

    @BeforeEach
    void setUp() {
        productCatalogIngestionService = new ProductCatalogIngestionServiceImpl(
                mapper,
                reactiveProductCatalogService,
                productCatalogIntegrationService);
    }

    @Test
    void ingestionInPullMode_Success() {
        when(mapper.mapIntegrationToStream(any()))
                .thenReturn(new com.backbase.stream.productcatalog.model.ProductCatalog());

        when(productCatalogIntegrationService.retrieveProductCatalog())
                .thenReturn(Mono.just(new ProductCatalog()));

        when(reactiveProductCatalogService.setupProductCatalog(any())).thenReturn(Mono.just(new com.backbase.stream.productcatalog.model.ProductCatalog()));

        ProductCatalogIngestResponse productCatalogIngestResponse = productCatalogIngestionService.ingestPull().block();
        assertNotNull(productCatalogIngestResponse.getProductCatalog());
    }

    @Test
    void ingestionInPushMode_Unsupported() {
        Mono<ProductCatalogIngestPushRequest> request = Mono.just(ProductCatalogIngestPushRequest.builder().build());
        assertThrows(UnsupportedOperationException.class, () -> {
            productCatalogIngestionService.ingestPush(request);
        });
    }
}
