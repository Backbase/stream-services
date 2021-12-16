package com.backbase.stream.compositions.legalentity.handlers;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.com.backbase.stream.compositions.events.ingress.event.spec.v1.LegalEntityIngestPullEvent;
import com.backbase.stream.compositions.legalentity.core.config.LegalEntityConfigurationProperties;
import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapperImpl;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LegalEntityIngestPullEventHandlerTest {
    @Mock
    private LegalEntityIngestionService legalEntityIngestionService;

    @Mock
    LegalEntityMapperImpl mapper;

    @Mock
    EventBus eventBus;

    @Test
    void testHandleEvent_Completed() {
        List<LegalEntity> legalEntities = new ArrayList<>();
        legalEntities.add(new LegalEntity().name("Legal Entity"));

        Mono<LegalEntityIngestResponse> responseMono = Mono.just(
                LegalEntityIngestResponse
                        .builder().legalEntities(legalEntities).build());

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
    }

    @Test
    void testHandleEvent_Failed() {
        assertThrows(Exception.class, () -> {
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
        });
    }
}
