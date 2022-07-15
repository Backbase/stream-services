package com.backbase.stream.paymentorder;

import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.UnitOfWorkExecutor;
import com.backbase.stream.worker.configuration.StreamWorkerConfiguration;
import com.backbase.stream.worker.repository.UnitOfWorkRepository;

public class PaymentOrderUnitOfWorkExecutor extends UnitOfWorkExecutor<PaymentOrderTask> {

    public PaymentOrderUnitOfWorkExecutor(UnitOfWorkRepository<PaymentOrderTask, String> repository,
                                          StreamTaskExecutor<PaymentOrderTask> streamTaskExecutor,
                                          StreamWorkerConfiguration streamWorkerConfiguration) {
        super(repository, streamTaskExecutor, streamWorkerConfiguration);
    }
}
