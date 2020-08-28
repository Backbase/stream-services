package com.backbase.stream.transaction;

import com.backbase.dbs.transaction.presentation.service.model.TransactionItemPost;
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

    public TransactionUnitOfWorkExecutor(UnitOfWorkRepository<TransactionTask, String> repository,
        StreamTaskExecutor<TransactionTask> streamTaskExecutor, TransactionWorkerConfigurationProperties properties) {

        super(repository, streamTaskExecutor, properties);
    }

    public Flux<UnitOfWork<TransactionTask>> prepareUnitOfWork(List<TransactionItemPost> items) {
        Stream<UnitOfWork<TransactionTask>> unitOfWorkStream;
        if (((TransactionWorkerConfigurationProperties) streamWorkerConfiguration).isGroupPerArrangementId()) {
            Map<String, List<TransactionItemPost>> transactionsGroupedByArrangement = items.stream()
                .collect(Collectors.groupingBy(TransactionItemPost::getExternalArrangementId));

            unitOfWorkStream = transactionsGroupedByArrangement.entrySet().stream()
                .map(entry -> {
                    String unitOfOWorkId = "transactions-grouped-" + entry.getKey() + "-" + System.currentTimeMillis();
                    return UnitOfWork
                        .from(unitOfOWorkId,
                            new TransactionTask(unitOfOWorkId, entry.getValue()));
                });
        } else {
            String unitOfOWorkId = "transactions-mixed-" + System.currentTimeMillis();
            TransactionTask task = new TransactionTask(unitOfOWorkId, items);
            unitOfWorkStream = Stream.of(UnitOfWork.from(unitOfOWorkId, task));
        }
        return Flux.fromStream(unitOfWorkStream);
    }

    public Flux<UnitOfWork<TransactionTask>> prepareUnitOfWork(Flux<TransactionItemPost> items) {

        return items
            .bufferTimeout(streamWorkerConfiguration.getBufferSize(), streamWorkerConfiguration.getBufferMaxTime())
            .flatMap(this::prepareUnitOfWork);
    }

    public TransactionWorkerConfigurationProperties getTransactionWorkerConfigurationProperties() {
        return (TransactionWorkerConfigurationProperties) super.getStreamWorkerConfiguration();
    }
}
