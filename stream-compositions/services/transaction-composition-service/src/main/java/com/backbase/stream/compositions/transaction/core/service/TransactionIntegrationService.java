package com.backbase.stream.compositions.transaction.core.service;

import com.backbase.stream.compositions.integration.transaction.model.TransactionsPostRequestBody;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestPullRequest;
import reactor.core.publisher.Flux;

public interface TransactionIntegrationService {
    /**
     * Pulls transactions from external integration service.
     *
     * @param ingestPullRequest TransactionIngestPullRequest
     * @return Mono<TransactionsPostRequestBody>
     */
    Flux<TransactionsPostRequestBody> pullTransactions(TransactionIngestPullRequest ingestPullRequest);
}
