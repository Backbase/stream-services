package com.backbase.stream.compositions.productcatalog.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.backbase.stream.compositions.integration.productcatalog.api.ProductCatalogIntegrationApi;
import com.backbase.stream.compositions.integration.productcatalog.model.ProductCatalog;
import com.backbase.stream.compositions.integration.productcatalog.model.PullProductCatalogResponse;
import com.backbase.stream.compositions.productcatalog.core.model.ProductCatalogIngestPullRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ProductCatalogIntegrationServiceImplTest {

    @Mock
    private ProductCatalogIntegrationApi productCatalogIntegrationApi;

    private ProductCatalogIntegrationServiceImpl productCatalogIntegrationService;

    @BeforeEach
    void setUp() {
        productCatalogIntegrationService =
            new ProductCatalogIntegrationServiceImpl(productCatalogIntegrationApi);
    }

    @Test
    void callIntegrationService_Success() throws UnsupportedOperationException {
        ProductCatalog productCatalog = new ProductCatalog();

        PullProductCatalogResponse pullProductGroupResponse =
            new PullProductCatalogResponse().productCatalog(productCatalog);
        when(productCatalogIntegrationApi.pullProductCatalog(any()))
            .thenReturn(Mono.just(pullProductGroupResponse));

        ProductCatalogIngestPullRequest ingestPullRequest =
            ProductCatalogIngestPullRequest.builder().build();
        Mono<ProductCatalog> productCatalogMono =
            productCatalogIntegrationService.pullProductCatalog(ingestPullRequest);
        assertEquals(productCatalog, productCatalogMono.block());
    }

    @Test
    void callIntegrationService_EmptyLegalEntityList() throws UnsupportedOperationException {
        when(productCatalogIntegrationApi.pullProductCatalog(any())).thenReturn(Mono.empty());

        ProductCatalogIngestPullRequest ingestPullRequest =
            ProductCatalogIngestPullRequest.builder().build();
        Mono<ProductCatalog> legalEntities =
            productCatalogIntegrationService.pullProductCatalog(ingestPullRequest);

        assertEquals(null, legalEntities.block());
    }
}
