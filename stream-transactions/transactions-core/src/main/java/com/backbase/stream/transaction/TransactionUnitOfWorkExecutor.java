package com.backbase.stream.transaction;

import com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostRequestBody;
import com.backbase.stream.configuration.TransactionWorkerConfigurationProperties;
import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.UnitOfWorkExecutor;
import com.backbase.stream.worker.model.UnitOfWork;
import com.backbase.stream.worker.repository.UnitOfWorkRepository;

import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TransactionUnitOfWorkExecutor extends UnitOfWorkExecutor<TransactionTask> {

    public TransactionUnitOfWorkExecutor(
            UnitOfWorkRepository<TransactionTask, String> repository,
            StreamTaskExecutor<TransactionTask> streamTaskExecutor,
            TransactionWorkerConfigurationProperties properties) {

        super(repository, streamTaskExecutor, properties);
    }

    public Flux<UnitOfWork<TransactionTask>> prepareUnitOfWork(
            List<TransactionsPostRequestBody> items) {
        Stream<UnitOfWork<TransactionTask>> unitOfWorkStream;
        if (((TransactionWorkerConfigurationProperties) streamWorkerConfiguration)
                .isGroupPerArrangementId()) {
            Map<String, List<TransactionsPostRequestBody>> transactionsGroupedByArrangement =
                    items.stream()
                            .collect(
                                    Collectors.groupingBy(
                                            TransactionsPostRequestBody::getExternalArrangementId));

            unitOfWorkStream =
                    transactionsGroupedByArrangement.entrySet().stream()
                            .map(
                                    entry -> {
                                        String unitOfOWorkId =
                                                "transactions-grouped-"
                                                        + entry.getKey()
                                                        + "-"
                                                        + System.currentTimeMillis();
                                        return UnitOfWork.from(
                                                unitOfOWorkId,
                                                new TransactionTask(
                                                        unitOfOWorkId, entry.getValue()));
                                    });
        } else {
            String unitOfOWorkId = "transactions-mixed-" + System.currentTimeMillis();
            TransactionTask task = new TransactionTask(unitOfOWorkId, items);
            unitOfWorkStream = Stream.of(UnitOfWork.from(unitOfOWorkId, task));
        }
        return Flux.fromStream(unitOfWorkStream);
    }

    public Flux<UnitOfWork<TransactionTask>> prepareUnitOfWork(
            Flux<TransactionsPostRequestBody> items) {

        return items.bufferTimeout(
                        streamWorkerConfiguration.getBufferSize(),
                        streamWorkerConfiguration.getBufferMaxTime())
                .flatMap(this::prepareUnitOfWork);
    }

    public TransactionWorkerConfigurationProperties getTransactionWorkerConfigurationProperties() {
        return (TransactionWorkerConfigurationProperties) super.getStreamWorkerConfiguration();
    }
}
