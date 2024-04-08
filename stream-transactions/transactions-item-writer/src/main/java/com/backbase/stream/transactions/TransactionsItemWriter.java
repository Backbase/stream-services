package com.backbase.stream.transactions;

import com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostRequestBody;
import com.backbase.stream.transaction.TransactionTask;
import com.backbase.stream.transaction.TransactionUnitOfWorkExecutor;
import com.backbase.stream.worker.exception.StreamTaskException;
import com.backbase.stream.worker.model.UnitOfWork;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;

@AllArgsConstructor
@Slf4j
public class TransactionsItemWriter implements ItemWriter<TransactionsPostRequestBody> {

    private final TransactionUnitOfWorkExecutor transactionTaskUnitOfWorkExecutor;

    /**
     * Process thousands of transactions by grouping them on arrangement id and register a Unit Of Work per arrangement
     * Executes jobs directly Synchronously, as Item Writers are used synchronously
     */
    @SuppressWarnings("unchecked")
    @Override
    public void write(@NonNull Chunk<? extends TransactionsPostRequestBody> items) throws Exception {

        List<TransactionsPostRequestBody> list = (List<TransactionsPostRequestBody>) items;
        Flux<UnitOfWork<TransactionTask>> unitOfWorkFlux = transactionTaskUnitOfWorkExecutor.prepareUnitOfWork(list);

        try {
            unitOfWorkFlux.flatMap(transactionTaskUnitOfWorkExecutor::executeTasks)
                .doOnNext(UnitOfWork::logSummary)
                .doOnError(throwable -> log
                    .error("Unexpected Exception while processing UnitOfWork: {}", throwable.getMessage()))
                .blockLast();
        } catch (StreamTaskException e) {

            if (!transactionTaskUnitOfWorkExecutor.getTransactionWorkerConfigurationProperties().isContinueOnError()) {
                throw new Exception("Failed to write transactions", e.getCause());
            } else {
                log.error("Failed to write transactions: {}", e.getMessage());
            }
        }
    }
}
