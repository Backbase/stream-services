package com.backbase.stream.compositions.legalentity.handlers;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.handler.EventHandler;
import com.backbase.stream.compositions.events.ingress.event.spec.v1.LegalEntityPushEvent;
import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapper;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityPushRequest;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityIngestionService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class LegalEntityPushEventHandler implements EventHandler<LegalEntityPushEvent> {
    private final LegalEntityIngestionService legalEntityIngestionService;
    private final LegalEntityMapper mapper;

    @Override
    public void handle(EnvelopedEvent<LegalEntityPushEvent> envelopedEvent) {
        legalEntityIngestionService.ingestPush(buildRequest(envelopedEvent));
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param envelopedEvent EnvelopedEvent<LegalEntityIngestPushEvent>
     * @return LegalEntityIngestPullRequest
     */
    private Mono<LegalEntityPushRequest> buildRequest(EnvelopedEvent<LegalEntityPushEvent> envelopedEvent) {
        return Mono.just(LegalEntityPushRequest.builder()
                .legalEntity(mapper.mapEventToStream(envelopedEvent.getEvent().getLegalEntity()))
                .build());
    }
}
