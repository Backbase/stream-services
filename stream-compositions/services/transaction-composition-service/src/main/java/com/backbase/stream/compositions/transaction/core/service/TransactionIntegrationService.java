package com.backbase.stream.compositions.transaction.core.service;

import com.backbase.stream.compositions.transaction.core.model.TransactionIngestPullRequest;
import com.backbase.stream.compositions.transaction.integration.client.model.TransactionsPostRequestBody;

import reactor.core.publisher.Flux;

public interface TransactionIntegrationService {
    /**
     * Pulls transactions from external integration service.
     *
     * @param ingestPullRequest TransactionIngestPullRequest
     * @return Mono<TransactionsPostRequestBody>
     */
    Flux<TransactionsPostRequestBody> pullTransactions(
            TransactionIngestPullRequest ingestPullRequest);
}
