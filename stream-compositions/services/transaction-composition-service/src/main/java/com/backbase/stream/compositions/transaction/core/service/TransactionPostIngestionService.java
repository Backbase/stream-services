package com.backbase.stream.compositions.transaction.core.service;

import com.backbase.dbs.transaction.api.service.v3.model.TransactionsPostResponseBody;
import reactor.core.publisher.Mono;

import java.util.List;

public interface TransactionPostIngestionService {
    /**
     * Post processing for a completed ingestion process
     * @param response
     */
    void handleSuccess(List<TransactionsPostResponseBody> response);

    /**
     * Post processing for a failed ingestion process
     * @param error
     */
    Mono<List<TransactionsPostResponseBody>> handleFailure(Throwable error);
}
