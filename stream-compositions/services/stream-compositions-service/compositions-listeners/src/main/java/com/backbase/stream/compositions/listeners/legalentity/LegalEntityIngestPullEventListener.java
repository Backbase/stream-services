package com.backbase.stream.compositions.listeners.legalentity;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.handler.EventHandler;
import com.backbase.com.backbase.stream.compositions.events.ingress.event.spec.v1.LegalEntityIngestPullEvent;
import org.springframework.stereotype.Component;

@Component
public class LegalEntityIngestPullEventListener implements EventHandler<LegalEntityIngestPullEvent> {
    public void handle(EnvelopedEvent<LegalEntityIngestPullEvent> envelopedEvent) {
    }
}
