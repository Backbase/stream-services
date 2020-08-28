package com.backbase.stream.audit;

import com.backbase.dbs.audit.service.model.AuditMessage;
import com.backbase.dbs.audit.service.model.AuditMessagesPostResponseBody;
import com.backbase.stream.worker.model.StreamTask;
import java.util.List;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class AuditMessagesTask extends StreamTask {

    private List<AuditMessage> auditMessages;
    private AuditMessagesPostResponseBody responseBody;

    public AuditMessagesTask(String id, List<AuditMessage> auditMessages) {
        super(id);
        this.auditMessages = auditMessages;
    }

    public List<AuditMessage> getAuditMessages() {
        return auditMessages;
    }

    public AuditMessagesPostResponseBody getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(AuditMessagesPostResponseBody responseBody) {
        this.responseBody = responseBody;
    }

    @Override
    public String getName() {
        return "auditMessages";
    }
}
