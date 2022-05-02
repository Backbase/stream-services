package com.backbase.stream.compositions.legalentity.handlers;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.handler.EventHandler;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.stream.compositions.events.ingress.event.spec.v1.LegalEntityPullEvent;
import com.backbase.stream.compositions.events.egress.event.spec.v1.LegalEntityCompletedEvent;
import com.backbase.stream.compositions.events.egress.event.spec.v1.LegalEntityFailedEvent;
import com.backbase.stream.compositions.legalentity.core.config.LegalEntityConfigurationProperties;
import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapper;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityPullRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityResponse;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityIngestionService;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@AllArgsConstructor
@EnableConfigurationProperties(LegalEntityConfigurationProperties.class)
public class LegalEntityPullEventHandler implements EventHandler<LegalEntityPullEvent> {
    private final LegalEntityConfigurationProperties configProperties;
    private final LegalEntityIngestionService legalEntityIngestionService;
    private final LegalEntityMapper mapper;
    private final EventBus eventBus;

    /**
     * Handles LegalEntityIngestPullEvent.
     *
     * @param envelopedEvent EnvelopedEvent<LegalEntityPullEvent>
     */
    @Override
    public void handle(EnvelopedEvent<LegalEntityPullEvent> envelopedEvent) {
        try {
            System.out.println("Event received   " + envelopedEvent.getEvent().getExternalId());
            legalEntityIngestionService.ingestPull(buildRequest(envelopedEvent.getEvent()));
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
    private Mono<LegalEntityPullRequest> buildRequest(LegalEntityPullEvent legalEntityIngestPullEvent) {
        return Mono.just(
                LegalEntityPullRequest.builder()
                        .legalEntityExternalId(legalEntityIngestPullEvent.getExternalId())
                        .build());
    }

    /**
     * Handles response from ingestion service.
     *
     * @param response LegalEntityIngestResponse
     */
    private void handleResponse(LegalEntityResponse response) {
        if (Boolean.TRUE.equals(configProperties.getEnableCompletedEvent())) {
            sendCompletedEvent(response);
        }
        if (Boolean.TRUE.equals(configProperties.getChainProductEvent())) {
            //sendProductPullEvent(response);
        }
    }

    private void sendCompletedEvent(LegalEntityResponse response) {
        LegalEntityCompletedEvent event = new LegalEntityCompletedEvent()
                .withLegalEntity(mapper.mapStreamToEvent(response.getLegalEntity()));

        EnvelopedEvent<LegalEntityCompletedEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(event);
        eventBus.emitEvent(envelopedEvent);
    }
/*
    private void sendProductPullEvent(LegalEntityResponse response) {
        ProductPullEvent event = new ProductPullEvent()
                .withEventId(UUID.randomUUID().toString())
                .withLegalEntityExternalId(response.getLegalEntity().getExternalId());

        EnvelopedEvent<ProductPullEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(event);
        eventBus.emitEvent(envelopedEvent);
    }
*/
    /**
     * Handles error from ingestion service.
     *
     * @param ex Throwable
     */
    private void handleError(Throwable ex) {
        if (Boolean.FALSE.equals(configProperties.getEnableFailedEvent())) {
            return;
        }
        LegalEntityFailedEvent event = new LegalEntityFailedEvent()
                .withEventId(UUID.randomUUID().toString())
                .withMessage(ex.getMessage());

        EnvelopedEvent<LegalEntityFailedEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(event);
        eventBus.emitEvent(envelopedEvent);
    }
}
