package com.backbase.stream.compositions.legalentity.handlers;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.handler.EventHandler;
import com.backbase.com.backbase.stream.compositions.events.ingress.event.spec.v1.LegalEntityIngestPushEvent;
import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapper;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestPushRequest;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityIngestionService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class LegalEntityIngestPushEventHandler implements EventHandler<LegalEntityIngestPushEvent> {
    private final LegalEntityIngestionService legalEntityIngestionService;
    private final LegalEntityMapper mapper;

    @Override
    public void handle(EnvelopedEvent<LegalEntityIngestPushEvent> envelopedEvent) {
        legalEntityIngestionService.ingestPush(buildRequest(envelopedEvent));
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param envelopedEvent EnvelopedEvent<LegalEntityIngestPushEvent>
     * @return LegalEntityIngestPullRequest
     */
    private Mono<LegalEntityIngestPushRequest> buildRequest(EnvelopedEvent<LegalEntityIngestPushEvent> envelopedEvent) {
        return Mono.just(LegalEntityIngestPushRequest.builder()
                .legalEntity(mapper.mapEventToStream(envelopedEvent.getEvent().getLegalEntity()))
                .build());
    }
}
