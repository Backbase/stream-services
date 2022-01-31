package com.backbase.stream.compositions.product.core.service;

import com.backbase.stream.compositions.product.core.model.ProductIngestPullRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestPushRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestResponse;
import reactor.core.publisher.Mono;

public interface ProductIngestionService {

    /**
     * Ingests legal Entities in pull mode.
     *
     * @param ingestPullRequest Ingest pull request
     * @return LegalEntityIngestResponse
     */
    Mono<ProductIngestResponse> ingestPull(Mono<ProductIngestPullRequest> ingestPullRequest);

    /**
     * Ingests product group in push mode.
     *
     * @param ingestPushRequest Ingest push request
     * @return ProductIngestResponse
     */
    Mono<ProductIngestResponse> ingestPush(Mono<ProductIngestPushRequest> ingestPushRequest);
}
