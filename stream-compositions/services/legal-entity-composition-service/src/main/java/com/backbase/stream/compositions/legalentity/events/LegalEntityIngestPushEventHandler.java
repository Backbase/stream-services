package com.backbase.stream.compositions.legalentity.events;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.handler.EventHandler;
import com.backbase.com.backbase.stream.compositions.events.ingress.event.spec.v1.LegalEntityIngestPushEvent;
import com.backbase.stream.compositions.legalentity.core.LegalEntityIngestionService;
import com.backbase.stream.compositions.legalentity.core.RequestSource;
import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapper;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestPushRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

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
    private LegalEntityIngestPushRequest buildRequest(EnvelopedEvent<LegalEntityIngestPushEvent> envelopedEvent) {

        return LegalEntityIngestPushRequest.builder()
                .legalEntities(envelopedEvent.getEvent().getLegalEntities()
                        .stream()
                        .map(mapper::mapEventToStream)
                        .collect(Collectors.toList()))
                .build();
    }
}
