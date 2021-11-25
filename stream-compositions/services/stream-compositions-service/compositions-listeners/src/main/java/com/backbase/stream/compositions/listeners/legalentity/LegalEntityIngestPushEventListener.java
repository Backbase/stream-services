package com.backbase.stream.compositions.listeners.legalentity;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.handler.EventHandler;
import com.backbase.com.backbase.stream.compositions.events.ingress.event.spec.v1.LegalEntityIngestPushEvent;
import org.springframework.stereotype.Component;

@Component
public class LegalEntityIngestPushEventListener implements EventHandler<LegalEntityIngestPushEvent> {
    @Override
    public void handle(EnvelopedEvent<LegalEntityIngestPushEvent> envelopedEvent) {

    }
}
