package com.backbase.stream.compositions.legalentity.handlers;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.handler.EventHandler;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.com.backbase.stream.compositions.events.egress.event.spec.v1.LegalEntityIngestCompletedEvent;
import com.backbase.com.backbase.stream.compositions.events.egress.event.spec.v1.LegalEntityIngestFailedEvent;
import com.backbase.com.backbase.stream.compositions.events.ingress.event.spec.v1.LegalEntityIngestPullEvent;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityIngestionService;
import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapper;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestPullRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class LegalEntityIngestPullEventHandler implements EventHandler<LegalEntityIngestPullEvent> {
    private final LegalEntityIngestionService legalEntityIngestionService;
    private final LegalEntityMapper mapper;
    private final EventBus eventBus;

    /**
     * Handles LegalEntityIngestPullEvent.
     *
     * @param envelopedEvent EnvelopedEvent<LegalEntityIngestPullEvent>
     */
    @Override
    public void handle(EnvelopedEvent<LegalEntityIngestPullEvent> envelopedEvent) {
        legalEntityIngestionService
                .ingestPull(buildRequest(envelopedEvent.getEvent()))
                .doOnError(this::handleError)
                .subscribe(this::handleResponse);
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param legalEntityIngestPullEvent LegalEntityIngestPullEvent
     * @return LegalEntityIngestPullRequest
     */
    private LegalEntityIngestPullRequest buildRequest(LegalEntityIngestPullEvent legalEntityIngestPullEvent) {
        return LegalEntityIngestPullRequest.builder()
                .legalEntityExternalId(legalEntityIngestPullEvent.getLegalEntityExternalId())
                .build();
    }

    /**
     * Handles reponse from ingestion service.
     *
     * @param response LegalEntityIngestResponse
     */
    private void handleResponse(LegalEntityIngestResponse response) {
        LegalEntityIngestCompletedEvent event = new LegalEntityIngestCompletedEvent()
                .withEventId(UUID.randomUUID().toString())
                .withLegalEntities(
                        response.getLegalEntities()
                                .stream()
                                .map(mapper::mapStreamToEvent)
                                .collect(Collectors.toList())
                );

        EnvelopedEvent<LegalEntityIngestCompletedEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(event);
        eventBus.emitEvent(envelopedEvent);
    }

    /**
     * Handles error from ingestion service.
     *
     * @param ex Throwable
     */
    private void handleError(Throwable ex) {
        LegalEntityIngestFailedEvent event = new LegalEntityIngestFailedEvent()
                .withEventId(UUID.randomUUID().toString())
                .withMessage(ex.getMessage());

        EnvelopedEvent<LegalEntityIngestFailedEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(event);
        eventBus.emitEvent(envelopedEvent);
    }
}
