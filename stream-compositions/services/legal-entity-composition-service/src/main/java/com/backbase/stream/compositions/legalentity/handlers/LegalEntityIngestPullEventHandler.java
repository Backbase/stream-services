package com.backbase.stream.compositions.legalentity.handlers;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.handler.EventHandler;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.com.backbase.stream.compositions.events.egress.event.spec.v1.LegalEntityIngestCompletedEvent;
import com.backbase.com.backbase.stream.compositions.events.egress.event.spec.v1.LegalEntityIngestFailedEvent;
import com.backbase.com.backbase.stream.compositions.events.ingress.event.spec.v1.LegalEntityIngestPullEvent;
import com.backbase.stream.compositions.legalentity.core.config.LegalEntityConfigurationProperties;
import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapper;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestPullRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestResponse;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityIngestionService;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
@EnableConfigurationProperties(LegalEntityConfigurationProperties.class)
public class LegalEntityIngestPullEventHandler implements EventHandler<LegalEntityIngestPullEvent> {
    private final LegalEntityConfigurationProperties configProperties;
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
        try {
            legalEntityIngestionService
                    .ingestPull(buildRequest(envelopedEvent.getEvent()))
                    .doOnError(this::handleError)
                    .subscribe(this::handleResponse);
        } catch (Exception ex) {
            this.handleError(ex);
        }
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param legalEntityIngestPullEvent LegalEntityIngestPullEvent
     * @return LegalEntityIngestPullRequest
     */
    private Mono<LegalEntityIngestPullRequest> buildRequest(LegalEntityIngestPullEvent legalEntityIngestPullEvent) {
        return Mono.just(
                LegalEntityIngestPullRequest.builder()
                        .legalEntityExternalId(legalEntityIngestPullEvent.getLegalEntityExternalId())
                        .build());
    }

    /**
     * Handles response from ingestion service.
     *
     * @param response LegalEntityIngestResponse
     */
    private void handleResponse(LegalEntityIngestResponse response) {
        if (Boolean.FALSE.equals(configProperties.getEnableCompletedEvents())) {
            return;
        }
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
        if (Boolean.FALSE.equals(configProperties.getEnableFailedEvents())) {
            return;
        }
        LegalEntityIngestFailedEvent event = new LegalEntityIngestFailedEvent()
                .withEventId(UUID.randomUUID().toString())
                .withMessage(ex.getMessage());

        EnvelopedEvent<LegalEntityIngestFailedEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(event);
        eventBus.emitEvent(envelopedEvent);
    }
}
