package com.backbase.stream.transaction.sink;

import com.backbase.dbs.transaction.presentation.service.model.TransactionItemPost;
import com.backbase.stream.TransactionService;
import com.backbase.stream.transaction.TransactionTask;
import com.backbase.stream.worker.UnitOfWorkExecutor;
import com.backbase.stream.worker.model.UnitOfWork;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;

/**
 * Input chanel for ingesting transactions.
 */
@EnableBinding(Sink.class)
@Slf4j
@AllArgsConstructor
public class TransactionSinkBinding {

    private final UnitOfWorkExecutor<TransactionTask> transactionTaskUnitOfWorkExecutor;


    /**
     * List of transactions to ingest.
     *
     * @param transactionItems List of transactions
     */
    @StreamListener(Sink.INPUT)
    public void accept(List<TransactionItemPost> transactionItems) {

        String unitOfOWorkId = "transaction-sink-" + System.currentTimeMillis();
        UnitOfWork<TransactionTask> transactionTaskUnitOfWork = UnitOfWork.from(
            unitOfOWorkId, new TransactionTask(unitOfOWorkId, transactionItems));

        UnitOfWork<TransactionTask> unitOfWork = transactionTaskUnitOfWorkExecutor.executeUnitOfWork(transactionTaskUnitOfWork)
            .block();

                log.info("Processed Unit of Work: {}", unitOfWork);

    }
}
