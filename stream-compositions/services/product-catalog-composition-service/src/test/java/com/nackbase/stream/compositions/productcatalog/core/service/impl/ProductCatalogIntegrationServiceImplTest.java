package com.nackbase.stream.compositions.productcatalog.core.service.impl;

import com.backbase.stream.compositions.integration.productcatalog.api.ProductCatalogIntegrationApi;
import com.backbase.stream.compositions.integration.productcatalog.model.GetProductGroupResponse;
import com.backbase.stream.compositions.integration.productcatalog.model.ProductCatalog;
import com.backbase.stream.compositions.productcatalog.core.service.impl.ProductCatalogIntegrationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProductCatalogIntegrationServiceImplTest {
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

        GetProductGroupResponse getProductGroupResponse = new GetProductGroupResponse().
                productCatalog(productCatalog);
        when(productCatalogIntegrationApi.getProductCatalog())
                .thenReturn(Mono.just(getProductGroupResponse));

        Mono<ProductCatalog> productCatalogMono = productCatalogIntegrationService.retrieveProductCatalog();
        assertEquals(productCatalog, productCatalogMono.block());
    }

    @Test
    void callIntegrationService_EmptyLegalEntityList() throws UnsupportedOperationException {
        when(productCatalogIntegrationApi.getProductCatalog())
                .thenReturn(Mono.empty());

        Mono<ProductCatalog> legalEntities = productCatalogIntegrationService.retrieveProductCatalog();

        assertEquals(null, legalEntities.block());
    }
}
