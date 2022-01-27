package com.backbase.stream.compositions.legalentity.handlers;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.com.backbase.stream.compositions.events.ingress.event.spec.v1.LegalEntityIngestPullEvent;
import com.backbase.stream.compositions.legalentity.core.config.LegalEntityConfigurationProperties;
import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapper;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestResponse;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityIngestionService;
import com.backbase.stream.legalentity.model.LegalEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LegalEntityIngestPullEventHandlerTest {
    @Mock
    private LegalEntityIngestionService legalEntityIngestionService;

    @Mock
    LegalEntityMapper mapper;

    @Mock
    EventBus eventBus;

    @Test
    void testHandleEvent_Completed() {
        Mono<LegalEntityIngestResponse> responseMono = Mono.just(
                LegalEntityIngestResponse
                        .builder().legalEntity(new LegalEntity().name("Legal Entity")).build());

        lenient().when(legalEntityIngestionService.ingestPull(any())).thenReturn(responseMono);

        LegalEntityConfigurationProperties properties = new LegalEntityConfigurationProperties();

        LegalEntityIngestPullEventHandler handler = new LegalEntityIngestPullEventHandler(
                properties,
                legalEntityIngestionService,
                mapper,
                eventBus);

        EnvelopedEvent<LegalEntityIngestPullEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(new LegalEntityIngestPullEvent().withLegalEntityExternalId("externalLegalId"));

        handler.handle(envelopedEvent);
        verify(legalEntityIngestionService).ingestPull(any());
    }

    @Test
    void testHandleEvent_Failed() {
        List<LegalEntity> legalEntities = new ArrayList<>();
        legalEntities.add(new LegalEntity().name("Legal Entity"));

        when(legalEntityIngestionService.ingestPull(any())).thenThrow(new RuntimeException());

        LegalEntityConfigurationProperties properties = new LegalEntityConfigurationProperties();

        LegalEntityIngestPullEventHandler handler = new LegalEntityIngestPullEventHandler(
                properties,
                legalEntityIngestionService,
                mapper,
                eventBus);

        EnvelopedEvent<LegalEntityIngestPullEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(new LegalEntityIngestPullEvent().withLegalEntityExternalId("externalLegalId"));

        handler.handle(envelopedEvent);
        verify(legalEntityIngestionService).ingestPull(any());
    }
}
