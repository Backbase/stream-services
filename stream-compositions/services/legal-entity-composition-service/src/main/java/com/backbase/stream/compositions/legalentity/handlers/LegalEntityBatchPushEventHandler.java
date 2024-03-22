package com.backbase.stream.compositions.legalentity.handlers;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.handler.EventHandler;
import com.backbase.stream.compositions.events.ingress.event.spec.v1.LegalEntityBatchPushEvent;
import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapper;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityPushRequest;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityIngestionService;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class LegalEntityBatchPushEventHandler implements EventHandler<LegalEntityBatchPushEvent> {

    private final LegalEntityIngestionService legalEntityIngestionService;
    private final LegalEntityMapper mapper;

    @Override
    public void handle(EnvelopedEvent<LegalEntityBatchPushEvent> envelopedEvent) {
        log.info("Received LegalEntityBatchPushEvent {}", envelopedEvent);
        legalEntityIngestionService.ingestBatchPush(buildRequest(envelopedEvent)).block();
    }

    private List<LegalEntityPushRequest> buildRequest(EnvelopedEvent<LegalEntityBatchPushEvent> envelopedEvent) {

        return envelopedEvent.getEvent().getLegalEntities().stream()
            .map(legalEntity -> LegalEntityPushRequest.builder()
                .legalEntity(mapper.mapEventToStream(legalEntity))
                .build())
            .toList();
    }
}
