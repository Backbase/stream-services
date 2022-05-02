package com.backbase.stream.compositions.legalentity.handlers;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.stream.compositions.events.ingress.event.spec.v1.LegalEntityPullEvent;
import com.backbase.stream.compositions.legalentity.core.config.LegalEntityConfigurationProperties;
import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapper;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityResponse;
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
        Mono<LegalEntityResponse> responseMono = Mono.just(
                LegalEntityResponse
                        .builder().legalEntity(new LegalEntity().name("Legal Entity")).build());

        lenient().when(legalEntityIngestionService.ingestPull(any())).thenReturn(responseMono);

        LegalEntityConfigurationProperties properties = new LegalEntityConfigurationProperties();

        LegalEntityPullEventHandler handler = new LegalEntityPullEventHandler(
                properties,
                legalEntityIngestionService,
                mapper,
                eventBus);

        EnvelopedEvent<LegalEntityPullEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(new LegalEntityPullEvent().withExternalId("externalLegalId"));

        handler.handle(envelopedEvent);
        verify(legalEntityIngestionService).ingestPull(any());
    }

    @Test
    void testHandleEvent_Failed() {
        List<LegalEntity> legalEntities = new ArrayList<>();
        legalEntities.add(new LegalEntity().name("Legal Entity"));

        when(legalEntityIngestionService.ingestPull(any())).thenThrow(new RuntimeException());

        LegalEntityConfigurationProperties properties = new LegalEntityConfigurationProperties();

        LegalEntityPullEventHandler handler = new LegalEntityPullEventHandler(
                properties,
                legalEntityIngestionService,
                mapper,
                eventBus);

        EnvelopedEvent<LegalEntityPullEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(new LegalEntityPullEvent().withExternalId("externalLegalId"));

        handler.handle(envelopedEvent);
        verify(legalEntityIngestionService).ingestPull(any());
    }
}
