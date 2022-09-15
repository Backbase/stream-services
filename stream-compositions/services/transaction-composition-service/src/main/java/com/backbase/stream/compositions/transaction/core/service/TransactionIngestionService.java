package com.backbase.stream.compositions.transaction.core.service;

import com.backbase.stream.compositions.transaction.core.model.TransactionIngestPullRequest;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestPushRequest;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestResponse;
import reactor.core.publisher.Mono;

public interface TransactionIngestionService {

    /**
     * Ingests legal Entities in pull mode.
     *
     * @param ingestPullRequest Ingest pull request
     * @return LegalEntityIngestResponse
     */
    Mono<TransactionIngestResponse> ingestPull(TransactionIngestPullRequest ingestPullRequest);

    /**
     * Ingests product group in push mode.
     *
     * @param ingestPushRequest Ingest push request
     * @return ProductIngestResponse
     */
    Mono<TransactionIngestResponse> ingestPush(TransactionIngestPushRequest ingestPushRequest);
}
