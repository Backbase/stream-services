package com.backbase.stream.compositions.listeners.legalentity;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.com.backbase.stream.compositions.events.egress.event.spec.v1.LegalEntityIngestCompletedEvent;
import com.backbase.com.backbase.stream.compositions.events.egress.event.spec.v1.LegalEntityIngestFailedEvent;
import com.backbase.stream.compositions.legalentity.LegalEntityEventEmitter;
import org.springframework.stereotype.Service;

@Service
public class LegalEntityEventEmitterImpl implements LegalEntityEventEmitter {
    private final EventBus eventBus;

    public LegalEntityEventEmitterImpl(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void emitCompletedEvent() {
        LegalEntityIngestCompletedEvent event = new LegalEntityIngestCompletedEvent();

        EnvelopedEvent<LegalEntityIngestCompletedEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(event);
        eventBus.emitEvent(envelopedEvent);
    }

    @Override
    public void emitFailedEvent() {
        LegalEntityIngestFailedEvent event = new LegalEntityIngestFailedEvent();

        EnvelopedEvent<LegalEntityIngestFailedEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(event);
        eventBus.emitEvent(envelopedEvent);
    }
}
