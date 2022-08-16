package com.backbase.stream.compositions.product.core.service;

import com.backbase.stream.compositions.product.core.model.ProductIngestResponse;
import reactor.core.publisher.Mono;

public interface ProductPostIngestionService {

    /**
     * Post processing for a completed ingestion process
     *
     * @param response
     */
    Mono<ProductIngestResponse> handleSuccess(ProductIngestResponse response);

    /**
     * Post processing for a failed ingestion process
     *
     * @param error
     */
    void handleFailure(Throwable error);
}
