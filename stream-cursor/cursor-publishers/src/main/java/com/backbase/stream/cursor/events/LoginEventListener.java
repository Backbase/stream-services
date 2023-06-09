package com.backbase.stream.cursor.events;

import com.backbase.stream.TransactionService;
import com.backbase.stream.cursor.configuration.CursorServiceConfigurationProperties;
import com.backbase.stream.cursor.model.LoginEvent;
import com.backbase.stream.service.EntitlementsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;

/**
 * Simple Login Event Listener listening to events emitted by Authentication Service.
 */
@Slf4j
public class LoginEventListener extends AbstractLoginEventListener {

    private static final String VIRTUAL_TOPIC_BACKBASE_AUTH_LOGIN =
        "VirtualTopic.Backbase.auth.login";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public LoginEventListener(
        EntitlementsService entitlementsService,
        TransactionService transactionService,
        CursorServiceConfigurationProperties properties) {
        super(entitlementsService, transactionService, properties);
    }

    @JmsListener(destination = VIRTUAL_TOPIC_BACKBASE_AUTH_LOGIN)
    private void listen(String message) {
        try {
            LoginEvent loginEvent = objectMapper.readValue(message, LoginEvent.class);

            super.publishIngestionCursorsFor(loginEvent, loginEvent.getUserId());

        } catch (IOException e) {
            log.error("Failed to read ingestion cursor from event: {}", message, e);
        }
    }
}
