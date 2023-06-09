package com.backbase.stream;

import com.backbase.dbs.transaction.api.service.v2.model.ArrangementItem;
import com.backbase.dbs.transaction.api.service.v2.model.TransactionItem;
import com.backbase.dbs.transaction.api.service.v2.model.TransactionsDeleteRequestBody;
import com.backbase.dbs.transaction.api.service.v2.model.TransactionsPatchRequestBody;
import com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostRequestBody;
import com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostResponseBody;
import com.backbase.stream.transaction.TransactionTask;
import com.backbase.stream.transaction.TransactionsQuery;
import com.backbase.stream.worker.model.UnitOfWork;
import java.util.Collection;
import java.util.stream.Stream;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionService {

    Flux<UnitOfWork<TransactionTask>> processTransactions(
        Flux<TransactionsPostRequestBody> transactions);

    default Flux<TransactionsPostResponseBody> getTransactionIdsFlux(
        UnitOfWork<TransactionTask> unitOfWork) {
        Stream<TransactionsPostResponseBody> transactionIdsStream =
            unitOfWork.getStreamTasks().stream()
                .map(TransactionTask::getResponse)
                .flatMap(Collection::stream);
        return Flux.fromStream(transactionIdsStream);
    }

    Flux<TransactionItem> getLatestTransactions(String arrangementId, int size);

    @SuppressWarnings("WeakerAccess")
    Mono<Void> deleteTransactions(Flux<TransactionsDeleteRequestBody> transactionItemDelete);

    Flux<TransactionItem> getTransactions(TransactionsQuery transactionsQuery);

    Mono<Void> patchTransactions(Flux<TransactionsPatchRequestBody> transactionItems);

    Mono<Void> postRefresh(Flux<ArrangementItem> arrangementItems);
}
