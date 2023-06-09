package com.backbase.stream.legalentity.repository;

import com.backbase.stream.LegalEntityTask;
import com.backbase.stream.worker.repository.UnitOfWorkRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LegalEntityUnitOfWorkRepository
    extends UnitOfWorkRepository<LegalEntityTask, String> {}
