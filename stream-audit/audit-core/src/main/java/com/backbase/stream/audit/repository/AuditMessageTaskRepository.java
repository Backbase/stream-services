package com.backbase.stream.audit.repository;

import com.backbase.stream.audit.AuditMessagesTask;
import com.backbase.stream.worker.repository.UnitOfWorkRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditMessageTaskRepository extends UnitOfWorkRepository<AuditMessagesTask, String> {

}
