package com.backbase.stream.compositions.productcatalog.core.service;

import com.backbase.stream.compositions.productcatalog.core.model.ProductCatalogIngestPushRequest;
import com.backbase.stream.compositions.productcatalog.core.model.ProductCatalogIngestResponse;
import reactor.core.publisher.Mono;

public interface ProductCatalogIngestionService {
    /**
     * Ingests product catalog in pull mode.
     *
     * @return ProductCatalogIngestResponse
     */
    Mono<ProductCatalogIngestResponse> ingestPull();

    /**
     * Ingests product catalog in push mode.
     *
     * @param ingestPullRequest Ingest push request
     * @return ProductCatalogIngestResponse
     */
    Mono<ProductCatalogIngestResponse> ingestPush(Mono<ProductCatalogIngestPushRequest> ingestPullRequest);
}
