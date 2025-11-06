package com.backbase.stream;

import com.backbase.dbs.transaction.api.service.v3.model.*;
import com.backbase.stream.transaction.TransactionTask;
import com.backbase.stream.transaction.TransactionsQuery;
import com.backbase.stream.worker.model.UnitOfWork;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.stream.Stream;

public interface TransactionService {
    Flux<UnitOfWork<TransactionTask>> processTransactions(Flux<TransactionsPostRequestBody> transactions);

    default Flux<TransactionsPostResponseBody> getTransactionIdsFlux(UnitOfWork<TransactionTask> unitOfWork) {
        Stream<TransactionsPostResponseBody> transactionIdsStream = unitOfWork.getStreamTasks().stream()
            .map(TransactionTask::getResponse)
            .flatMap(Collection::stream);
        return Flux.fromStream(transactionIdsStream);
    }

    Flux<TransactionItem> getLatestTransactions(String arrangementId, int size, String bookingDateGreaterThan, String bookingDateLessThan);

    @SuppressWarnings("WeakerAccess")
    Mono<Void> deleteTransactions(Flux<TransactionsDeleteRequestBody> transactionItemDelete);

    Flux<TransactionItem> getTransactions(TransactionsQuery transactionsQuery);

    Mono<Void> patchTransactions(Flux<TransactionsPatchRequestBody> transactionItems);

    Mono<Void> postRefresh(Flux<ArrangementItem> arrangementItems);
}
