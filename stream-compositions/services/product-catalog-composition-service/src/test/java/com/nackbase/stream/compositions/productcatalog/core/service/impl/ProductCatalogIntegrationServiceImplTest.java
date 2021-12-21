package com.nackbase.stream.compositions.productcatalog.core.service.impl;

import com.backbase.stream.compositions.integration.productcatalog.api.ProductCatalogIntegrationApi;
import com.backbase.stream.compositions.integration.productcatalog.model.ProductCatalog;
import com.backbase.stream.compositions.integration.productcatalog.model.PullProductCatalogResponse;
import com.backbase.stream.compositions.productcatalog.core.service.impl.ProductCatalogIntegrationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductCatalogIntegrationServiceImplTest {
    @Mock
    private ProductCatalogIntegrationApi productCatalogIntegrationApi;

    private ProductCatalogIntegrationServiceImpl productCatalogIntegrationService;

    @BeforeEach
    void setUp() {
        productCatalogIntegrationService = new ProductCatalogIntegrationServiceImpl(productCatalogIntegrationApi);
    }

    @Test
    void callIntegrationService_Success() throws UnsupportedOperationException {
        ProductCatalog productCatalog = new ProductCatalog();

        PullProductCatalogResponse pullProductGroupResponse = new PullProductCatalogResponse().productCatalog(productCatalog);
        when(productCatalogIntegrationApi.pullProductCatalog())
                .thenReturn(Mono.just(pullProductGroupResponse));

        Mono<ProductCatalog> productCatalogMono = productCatalogIntegrationService.pullProductCatalog();
        assertEquals(productCatalog, productCatalogMono.block());
    }

    @Test
    void callIntegrationService_EmptyLegalEntityList() throws UnsupportedOperationException {
        when(productCatalogIntegrationApi.pullProductCatalog())
                .thenReturn(Mono.empty());

        Mono<ProductCatalog> legalEntities = productCatalogIntegrationService.pullProductCatalog();

        assertEquals(null, legalEntities.block());
    }
}
