package com.backbase.stream.legalentity.repository;

import com.backbase.stream.UpdatedServiceAgreementTask;
import com.backbase.stream.worker.repository.UnitOfWorkRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UpdatedServiceAgreementUnitOfWorkRepository
    extends UnitOfWorkRepository<UpdatedServiceAgreementTask, String> {}
