package com.backbase.stream.productcatalog;

import com.backbase.stream.productcatalog.model.ProductCatalog;
import com.backbase.stream.productcatalog.model.ProductType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Blocking version of the {@link ProductCatalogService}.
 */
@Slf4j
@AllArgsConstructor
public class ProductCatalogService {

    private final ReactiveProductCatalogService reactiveProductCatalogService;

    /**
     * Get Product Catalog from DBS.
     *
     * @return Product Catalog
     */
    public ProductCatalog getProductCatalog() {
        return reactiveProductCatalogService.getProductCatalog().block();
    }

    /**
     * Setup Product Catalog.
     *
     * @param productCatalog Product Catalog is setup.
     * @return Configured Product Catalog.
     */
    public ProductCatalog setupProductCatalog(ProductCatalog productCatalog) {
        return reactiveProductCatalogService.setupProductCatalog(productCatalog).block();
    }

    public ProductType getProductTypeByExternalId(String productTypeExternalId) {
        return reactiveProductCatalogService.getProductTypeByExternalId(productTypeExternalId).block();
    }
}
