package com.backbase.stream.loan.repository;

import com.backbase.stream.loan.LoansTask;
import com.backbase.stream.worker.repository.UnitOfWorkRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoansUnitOfWorkRepository extends UnitOfWorkRepository<LoansTask, String> {

}
