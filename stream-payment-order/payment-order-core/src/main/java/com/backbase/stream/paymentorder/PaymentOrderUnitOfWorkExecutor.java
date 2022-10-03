package com.backbase.stream.paymentorder;

import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostRequest;
import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.UnitOfWorkExecutor;
import com.backbase.stream.worker.configuration.StreamWorkerConfiguration;
import com.backbase.stream.worker.model.UnitOfWork;
import com.backbase.stream.worker.repository.UnitOfWorkRepository;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Stream;

public class PaymentOrderUnitOfWorkExecutor extends UnitOfWorkExecutor<PaymentOrderTask> {

    public PaymentOrderUnitOfWorkExecutor(UnitOfWorkRepository<PaymentOrderTask, String> repository,
                                          StreamTaskExecutor<PaymentOrderTask> streamTaskExecutor,
                                          StreamWorkerConfiguration streamWorkerConfiguration) {
        super(repository, streamTaskExecutor, streamWorkerConfiguration);
    }

    public Flux<UnitOfWork<PaymentOrderTask>> prepareUnitOfWork(List<PaymentOrderPostRequest> items) {

        Stream<UnitOfWork<PaymentOrderTask>> unitOfWorkStream;
        String unitOfOWorkId = "payment-orders-mixed-" + System.currentTimeMillis();
        PaymentOrderTask task = new PaymentOrderTask(unitOfOWorkId, items);
        unitOfWorkStream = Stream.of(UnitOfWork.from(unitOfOWorkId, task));
        return Flux.fromStream(unitOfWorkStream);
    }

    public Flux<UnitOfWork<PaymentOrderTask>> prepareUnitOfWork(Flux<PaymentOrderPostRequest> items) {
        return items
                .bufferTimeout(streamWorkerConfiguration.getBufferSize(), streamWorkerConfiguration.getBufferMaxTime())
                .flatMap(this::prepareUnitOfWork);
    }
}
