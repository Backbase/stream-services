package com.backbase.stream.transaction;

import com.backbase.dbs.transaction.presentation.service.api.TransactionsApi;
import com.backbase.dbs.transaction.presentation.service.model.TransactionIds;
import com.backbase.dbs.transaction.presentation.service.model.TransactionItemPost;
import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.exception.StreamTaskException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Slf4j
public class TransactionTaskExecutor implements StreamTaskExecutor<TransactionTask> {

    private final TransactionsApi transactionsApi;

    public TransactionTaskExecutor(TransactionsApi transactionsApi) {
        this.transactionsApi = transactionsApi;
    }

    @Override
    public Mono<TransactionTask> executeTask(TransactionTask streamTask) {
        List<TransactionItemPost> data = streamTask.getData();
        String externalIds = streamTask.getData().stream().map(TransactionItemPost::getExternalId).collect(Collectors.joining(","));
        log.info("Post {} transactions: ", data.size());
        return transactionsApi.postTransactions(data)
            .onErrorResume(WebClientResponseException.class, throwable -> {
                streamTask.error("transactions", "post", "failed", externalIds, null, throwable, throwable.getResponseBodyAsString(), "Failed to ingest transactions");
                return Mono.error(new StreamTaskException(streamTask, throwable, "Failed to Ingest Transactions: " + throwable.getResponseBodyAsString()));
            })
            .collectList()
            .map(transactionIds -> {
                streamTask.error("transactions", "post", "success", externalIds, transactionIds.stream().map(TransactionIds::getId).collect(Collectors.joining(",")), "Ingested Transactions");
                streamTask.setResponse(transactionIds);
                return streamTask;
            });
    }

    @Override
    public Mono<TransactionTask> rollBack(TransactionTask streamTask) {
        return Mono.just(streamTask);
    }

}
