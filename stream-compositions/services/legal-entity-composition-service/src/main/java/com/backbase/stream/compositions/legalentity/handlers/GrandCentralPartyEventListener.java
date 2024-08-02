package com.backbase.stream.compositions.legalentity.handlers;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.handler.EventHandler;
import com.backbase.gc.party.Party;
import com.backbase.stream.compositions.legalentity.core.mapper.GrandCentralPartyMapper;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityPushRequest;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GrandCentralPartyEventListener implements EventHandler<Party> {

    private final LegalEntityIngestionService service;
    private final GrandCentralPartyMapper mapper;

    @Override
    public void handle(EnvelopedEvent<Party> internalRequest) {
        var legalEntity = mapper.mapLegalEntity(internalRequest.getEvent());
        service.ingestPush(new LegalEntityPushRequest(legalEntity)).block();
    }
}
