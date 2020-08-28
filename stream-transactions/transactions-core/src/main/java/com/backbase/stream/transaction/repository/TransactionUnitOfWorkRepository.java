package com.backbase.stream.transaction.repository;

import com.backbase.stream.transaction.TransactionTask;
import com.backbase.stream.worker.repository.UnitOfWorkRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionUnitOfWorkRepository extends UnitOfWorkRepository<TransactionTask, String> {

}
