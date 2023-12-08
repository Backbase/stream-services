package com.backbase.stream.compositions.product.core.service;

import com.backbase.stream.compositions.transaction.client.model.TransactionPullIngestionRequest;
import reactor.core.publisher.Mono;

public interface TransactionIngestionService {

    Mono<?> ingestTransactions(TransactionPullIngestionRequest res);

    Mono<?> ingestTransactionsAsync(TransactionPullIngestionRequest res);
}
