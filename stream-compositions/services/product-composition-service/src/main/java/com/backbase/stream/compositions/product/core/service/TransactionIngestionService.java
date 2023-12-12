package com.backbase.stream.compositions.product.core.service;

import com.backbase.stream.compositions.transaction.client.model.TransactionPullIngestionRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionIngestionService {

    Mono<?> ingestTransactions(TransactionPullIngestionRequest res);

    Flux<?> ingestTransactions(Flux<TransactionPullIngestionRequest> res);

    Mono<?> ingestTransactionsAsync(TransactionPullIngestionRequest res);

    Flux<?> ingestTransactionsAsync(Flux<TransactionPullIngestionRequest> res);
}
