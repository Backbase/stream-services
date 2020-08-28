package com.backbase.stream;

import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.UnitOfWorkExecutor;
import com.backbase.stream.worker.configuration.StreamWorkerConfiguration;
import com.backbase.stream.worker.repository.UnitOfWorkRepository;

public class LegalEntityUnitOfWorkExecutor extends UnitOfWorkExecutor<LegalEntityTask> {

    public LegalEntityUnitOfWorkExecutor(UnitOfWorkRepository<LegalEntityTask, String> repository, StreamTaskExecutor<LegalEntityTask> streamTaskExecutor, StreamWorkerConfiguration properties) {
        super(repository, streamTaskExecutor, properties);
    }
}
