package com.backbase.stream.compositions.listeners.product;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.handler.EventHandler;
import com.backbase.com.backbase.stream.compositions.events.ingress.event.spec.v1.LegalEntityIngestPullEvent;
import org.springframework.stereotype.Component;

@Component
public class ProductIngestPullEventListener implements EventHandler<LegalEntityIngestPullEvent> {
    public void handle(EnvelopedEvent<LegalEntityIngestPullEvent> envelopedEvent) {
    }
}
