package com.backbase.stream.cursor.events;

import com.backbase.stream.TransactionService;
import com.backbase.stream.cursor.configuration.CursorServiceConfigurationProperties;
import com.backbase.stream.cursor.model.AuditMessagesEvent;
import com.backbase.stream.service.EntitlementsService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;

/**
 * Audit Login Event Listener. Requires Audit to emit events and Audit Identity integration.
 */
@Slf4j
public class AuditLoginEventListener extends AbstractLoginEventListener {

    private static final String VIRTUAL_TOPIC_BACKBASE_AUTH_LOGIN
        = "VirtualTopic.com.backbase.audit.persistence.event.spec.v1.AuditMessagesCreatedEvent";

    private final ObjectMapper objectMapper;

    public AuditLoginEventListener(
        EntitlementsService entitlementsService,
        TransactionService transactionService,
        CursorServiceConfigurationProperties properties
    ) {
        super(entitlementsService, transactionService, properties);
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    }

    @JmsListener(destination = VIRTUAL_TOPIC_BACKBASE_AUTH_LOGIN)
    private void listen(byte[] message) {
        AuditMessagesEvent auditMessagesEvent;
        try {
            auditMessagesEvent = objectMapper.readValue(message, AuditMessagesEvent.class);
            log.info("auditMessagesEvent: {}", auditMessagesEvent);

            AuditMessagesEvent.AuditMessage auditMessage = auditMessagesEvent.getAuditMessages().get(0);

            if (auditMessage.getEventCategory().equalsIgnoreCase("Identity and Access")
                && auditMessage.getObjectType().equalsIgnoreCase("Authentication")
                && auditMessage.getEventAction().equalsIgnoreCase("Attempt Login")
                && auditMessage.getStatus().equalsIgnoreCase("Successful")) {

                log.info("auditMessage is an Identity login event");

                String username = auditMessage.getUsername();
                super.publishIngestionCursorsFor(auditMessage, username);
            }
        } catch (IOException e) {
            log.warn("Unable to read message: {}", new String(message), e);
        }

    }

}
