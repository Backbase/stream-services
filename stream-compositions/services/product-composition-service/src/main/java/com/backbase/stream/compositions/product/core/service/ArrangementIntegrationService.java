package com.backbase.stream.compositions.product.core.service;

import com.backbase.stream.compositions.product.core.model.ArrangementIngestPullRequest;
import com.backbase.stream.compositions.product.core.model.ArrangementIngestResponse;
import reactor.core.publisher.Mono;

public interface ArrangementIntegrationService {
    /**
     * Ingests arrangement in pull mode.
     *
     * @param ingestionRequest Ingest pull request
     * @return ArrangementIngestionResponse
     */
    Mono<ArrangementIngestResponse> pullArrangement(ArrangementIngestPullRequest ingestionRequest);
}
