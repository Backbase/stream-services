package com.backbase.stream.audit;

import com.backbase.dbs.audit.service.api.AuditMessagesApi;
import com.backbase.dbs.audit.service.model.AuditMessagesPostRequestBody;
import com.backbase.stream.worker.StreamTaskExecutor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@Slf4j
public class AuditTaskExecutor implements StreamTaskExecutor<AuditMessagesTask> {

    private final AuditMessagesApi auditMessagesApi;


    @Override
    public Mono<AuditMessagesTask> executeTask(AuditMessagesTask auditMessagesTask) {
        int size = auditMessagesTask.getAuditMessages().size();
        log.info("Ingesting {} audit messages", size);
        auditMessagesTask.info("audit-messages", "post", "", "", "", "Start Ingesting Audit Messages");
        AuditMessagesPostRequestBody auditMessagesPostRequestBody = new AuditMessagesPostRequestBody();
        auditMessagesPostRequestBody.setAuditMessages(auditMessagesTask.getAuditMessages());
        return auditMessagesApi.postAuditmessages(auditMessagesPostRequestBody)
            .map(auditMessagesPostResponseBody -> {
                log.info("Finished processing {} messages", size);
                auditMessagesTask.setResponseBody(auditMessagesPostResponseBody);
                auditMessagesTask.info("audit-messages", "post", "success", "", "", "Ingested Audit Messages");
                return auditMessagesTask;
            }).onErrorResume(WebClientResponseException.class, e -> {
                auditMessagesTask.error("audit-message", "post", "failed", "", "", e.getResponseBodyAsString());
                return Mono.error(e);
            });
    }

    @Override
    public Mono<AuditMessagesTask> rollBack(AuditMessagesTask auditMessagesTask) {
        return Mono.justOrEmpty(auditMessagesTask);
    }
}
