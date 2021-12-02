package com.backbase.stream.compositions.legalentity.events;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.handler.EventHandler;
import com.backbase.com.backbase.stream.compositions.events.ingress.event.spec.v1.LegalEntityIngestPullEvent;
import com.backbase.stream.compositions.legalentity.core.LegalEntityIngestionService;
import com.backbase.stream.compositions.legalentity.core.RequestSource;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestPullRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class LegalEntityIngestPullEventListener implements EventHandler<LegalEntityIngestPullEvent> {
    private final LegalEntityIngestionService legalEntityIngestionService;

    /**
     * Handles LegalEntityIngestPullEvent.
     *
     * @param envelopedEvent EnvelopedEvent<LegalEntityIngestPullEvent>
     */
    @Override
    public void handle(EnvelopedEvent<LegalEntityIngestPullEvent> envelopedEvent) {
        legalEntityIngestionService.ingestPull(buildRequest(envelopedEvent.getEvent()));
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param legalEntityIngestPullEvent LegalEntityIngestPullEvent
     * @return LegalEntityIngestPullRequest
     */
    private LegalEntityIngestPullRequest buildRequest(LegalEntityIngestPullEvent legalEntityIngestPullEvent) {
        return LegalEntityIngestPullRequest.builder()
                .soure(RequestSource.EVENT)
                .legalEntityExternalId(legalEntityIngestPullEvent.getLegalEntityExternalId())
                .build();
    }
}
