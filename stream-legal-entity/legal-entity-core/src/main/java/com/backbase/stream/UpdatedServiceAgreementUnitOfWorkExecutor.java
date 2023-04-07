package com.backbase.stream;

import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.UnitOfWorkExecutor;
import com.backbase.stream.worker.configuration.StreamWorkerConfiguration;
import com.backbase.stream.worker.repository.UnitOfWorkRepository;

public class UpdatedServiceAgreementUnitOfWorkExecutor
        extends UnitOfWorkExecutor<UpdatedServiceAgreementTask> {

    public UpdatedServiceAgreementUnitOfWorkExecutor(
            UnitOfWorkRepository<UpdatedServiceAgreementTask, String> repository,
            StreamTaskExecutor<UpdatedServiceAgreementTask> streamTaskExecutor,
            StreamWorkerConfiguration streamWorkerConfiguration) {
        super(repository, streamTaskExecutor, streamWorkerConfiguration);
    }
}
