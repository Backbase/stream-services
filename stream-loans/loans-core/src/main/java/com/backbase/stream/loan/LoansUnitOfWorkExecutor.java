package com.backbase.stream.loan;

import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.UnitOfWorkExecutor;
import com.backbase.stream.worker.configuration.StreamWorkerConfiguration;
import com.backbase.stream.worker.repository.UnitOfWorkRepository;

public class LoansUnitOfWorkExecutor extends UnitOfWorkExecutor<LoansTask> {

    public LoansUnitOfWorkExecutor(UnitOfWorkRepository<LoansTask, String> repository,
        StreamTaskExecutor<LoansTask> streamTaskExecutor, StreamWorkerConfiguration streamWorkerConfiguration) {
        super(repository, streamTaskExecutor, streamWorkerConfiguration);
    }

}
