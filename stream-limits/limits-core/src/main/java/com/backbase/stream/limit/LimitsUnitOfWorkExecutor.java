package com.backbase.stream.limit;

import com.backbase.dbs.limit.service.model.CreateLimitRequest;
import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.UnitOfWorkExecutor;
import com.backbase.stream.worker.configuration.StreamWorkerConfiguration;
import com.backbase.stream.worker.model.UnitOfWork;
import com.backbase.stream.worker.repository.UnitOfWorkRepository;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Stream;

public class LimitsUnitOfWorkExecutor extends UnitOfWorkExecutor<LimitsTask> {

    public LimitsUnitOfWorkExecutor(UnitOfWorkRepository<LimitsTask, String> repository, StreamTaskExecutor<LimitsTask> streamTaskExecutor, StreamWorkerConfiguration streamWorkerConfiguration) {
        super(repository, streamTaskExecutor, streamWorkerConfiguration);
    }

    public Flux<UnitOfWork<LimitsTask>> prepareUnitOfWork(List<CreateLimitRequest> items) {
        String unitOfWorkId = "limits-" + System.currentTimeMillis();
        Flux<UnitOfWork<LimitsTask>> toWorkOn = Flux.empty();
        items.forEach(item -> {
            LimitsTask limitsTask = new LimitsTask(unitOfWorkId + "-" + item.getUserBBID(), item);
            Flux<UnitOfWork<LimitsTask>> just = Flux.just(UnitOfWork.from(unitOfWorkId, limitsTask));
            toWorkOn.mergeWith(just);
        });

        return toWorkOn;
    }

    public Flux<UnitOfWork<LimitsTask>> prepareUnitOfWork(Flux<CreateLimitRequest> items) {
        return items.buffer(streamWorkerConfiguration.getBufferSize()).flatMap(this::prepareUnitOfWork);
    }
}
