package com.backbase.stream.compositions.productcatalog.core.service.impl;

import com.backbase.stream.compositions.integration.productcatalog.api.ProductCatalogIntegrationApi;
import com.backbase.stream.compositions.integration.productcatalog.model.ProductCatalog;
import com.backbase.stream.compositions.productcatalog.core.service.ProductCatalogIntegrationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@AllArgsConstructor
public class ProductCatalogIntegrationServiceImpl implements ProductCatalogIntegrationService {
    private final ProductCatalogIntegrationApi productCatalogIntegrationApi;

    @Override
    public Mono<ProductCatalog> retrieveProductCatalog() {
        return productCatalogIntegrationApi.getProductCatalog().map(item -> item.getProductCatalog());
    }
}
