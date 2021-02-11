package com.backbase.stream.audit;

import com.backbase.dbs.audit.api.service.v2.model.AuditMessage;
import com.backbase.dbs.audit.api.service.v2.model.AuditMessagesPostResponse;
import com.backbase.stream.worker.model.StreamTask;
import java.util.List;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class AuditMessagesTask extends StreamTask {

    private List<AuditMessage> auditMessages;
    private AuditMessagesPostResponse responseBody;

    public AuditMessagesTask(String id, List<AuditMessage> auditMessages) {
        super(id);
        this.auditMessages = auditMessages;
    }

    public List<AuditMessage> getAuditMessages() {
        return auditMessages;
    }

    public AuditMessagesPostResponse getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(AuditMessagesPostResponse responseBody) {
        this.responseBody = responseBody;
    }

    @Override
    public String getName() {
        return "auditMessages";
    }
}
