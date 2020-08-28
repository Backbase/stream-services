package com.backbase.stream.audit.http.controller;

import com.backbase.dbs.audit.service.model.AuditMessage;
import com.backbase.stream.audit.AuditMessagesTask;
import com.backbase.stream.audit.AuditUnitOfWorkExecutor;
import com.backbase.stream.audit.configuration.AuditConfiguration;
import com.backbase.stream.audit.http.api.AsyncApi;
import com.backbase.stream.audit.http.mapper.AuditMessageMapper;
import com.backbase.stream.audit.http.model.AuditMessagesPostRequestBody;
import com.backbase.stream.audit.http.model.UnitOfWorkResponse;
import com.backbase.stream.worker.model.UnitOfWork;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.mapstruct.factory.Mappers;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@Import(AuditConfiguration.class)
public class AsyncController implements AsyncApi {

    private final AuditUnitOfWorkExecutor auditUnitOfWorkExecutor;
    private final AuditMessageMapper auditMessageMapper = Mappers.getMapper(AuditMessageMapper.class);

    public AsyncController(AuditUnitOfWorkExecutor auditUnitOfWorkExecutor) {
        this.auditUnitOfWorkExecutor = auditUnitOfWorkExecutor;
    }

    @Override
    public Mono<ResponseEntity<UnitOfWorkResponse>> asyncPostAuditmessages(
        @Valid Mono<AuditMessagesPostRequestBody> auditMessagesPostRequestBody, ServerWebExchange exchange) {

        return auditMessagesPostRequestBody
            .map(this::getUnitOfWork)
            .flatMap(auditUnitOfWorkExecutor::register)
            .map(auditMessageMapper::toHttp)
            .map(ResponseEntity::ok);
    }

    private UnitOfWork<AuditMessagesTask> getUnitOfWork(AuditMessagesPostRequestBody request) {
        List<AuditMessage> auditMessages = request.getAuditMessages().stream()
            .map(auditMessageMapper::toPresentation)
            .collect(Collectors.toList());
        return auditUnitOfWorkExecutor.prepareUnitOfWork(auditMessages);
    }
}
