package com.backbase.stream.audit.http.mapper;

import com.backbase.dbs.audit.api.service.v2.model.AuditMessage;
import com.backbase.stream.audit.AuditMessagesTask;
import com.backbase.stream.audit.http.model.UnitOfWorkResponse;
import com.backbase.stream.worker.model.UnitOfWork;
import org.mapstruct.Mapper;

@Mapper
public interface AuditMessageMapper {

    AuditMessage toPresentation(com.backbase.stream.audit.http.model.AuditMessage auditMessage);

    UnitOfWorkResponse toHttp(UnitOfWork<AuditMessagesTask> unitOfWork);

}
