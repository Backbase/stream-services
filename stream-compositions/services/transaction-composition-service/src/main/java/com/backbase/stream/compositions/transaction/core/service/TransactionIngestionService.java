package com.backbase.stream.compositions.transaction.core.service;

import com.backbase.stream.compositions.transaction.core.model.TransactionIngestPullRequest;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestPushRequest;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestResponse;

import reactor.core.publisher.Mono;

public interface TransactionIngestionService {

    /**
     * Ingests Transactions in pull mode.
     *
     * @param ingestPullRequest Ingest pull request
     * @return TransactionIngestResponse
     */
    Mono<TransactionIngestResponse> ingestPull(TransactionIngestPullRequest ingestPullRequest);

    /**
     * Ingests Transactions in push mode.
     *
     * @param ingestPushRequest Ingest push request
     * @return TransactionIngestResponse
     */
    Mono<TransactionIngestResponse> ingestPush(TransactionIngestPushRequest ingestPushRequest);
}
