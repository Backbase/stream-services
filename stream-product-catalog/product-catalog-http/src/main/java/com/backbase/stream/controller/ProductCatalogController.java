package com.backbase.stream.controller;

import com.backbase.stream.productcatalog.ReactiveProductCatalogService;
import com.backbase.stream.productcatalog.ProductCatalogApi;
import com.backbase.stream.productcatalog.model.ProductCatalog;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Rest interface exposing Product Catalog Service.
 */
@RestController
@AllArgsConstructor
public class ProductCatalogController implements ProductCatalogApi {

    private final ReactiveProductCatalogService productService;

    /**
     * Get Product Catalog from DBS.
     *
     * @param exchange Current Request Exchange
     * @return Product Catalog
     */
    @Override
    public Mono<ResponseEntity<ProductCatalog>> getProductCatalog(ServerWebExchange exchange) {
        return productService.getProductCatalog()
            .map(ResponseEntity::ok);
    }

    /**
     * Setup Product Catalog in DBS.
     *
     * @param productCatalog Product Catalog to create
     * @param exchange       Current Request Exchange
     * @return Created and updated Product Catalog
     */
    @Override
    public Mono<ResponseEntity<ProductCatalog>> setupProductCatalog(Mono<ProductCatalog> productCatalog,
        ServerWebExchange exchange) {
        return productCatalog.flatMap(productService::setupProductCatalog)
            .map(ResponseEntity::ok);

    }

}
