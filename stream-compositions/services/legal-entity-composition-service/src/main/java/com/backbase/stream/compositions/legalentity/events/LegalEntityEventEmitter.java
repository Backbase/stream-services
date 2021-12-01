package com.backbase.stream.compositions.legalentity.events;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.com.backbase.stream.compositions.events.egress.event.spec.v1.LegalEntityIngestCompletedEvent;
import com.backbase.com.backbase.stream.compositions.events.egress.event.spec.v1.LegalEntityIngestFailedEvent;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class LegalEntityEventEmitter {
    //private final EventBus eventBus;

    public void emitCompletedEvent() {
        LegalEntityIngestCompletedEvent event = new LegalEntityIngestCompletedEvent();

        EnvelopedEvent<LegalEntityIngestCompletedEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(event);
        //eventBus.emitEvent(envelopedEvent);
    }

    public void emitFailedEvent() {
        LegalEntityIngestFailedEvent event = new LegalEntityIngestFailedEvent();

        EnvelopedEvent<LegalEntityIngestFailedEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(event);
        //eventBus.emitEvent(envelopedEvent);
    }
}
