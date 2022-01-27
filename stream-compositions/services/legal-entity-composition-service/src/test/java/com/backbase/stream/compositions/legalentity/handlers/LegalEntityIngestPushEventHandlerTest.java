package com.backbase.stream.compositions.legalentity.handlers;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.com.backbase.stream.compositions.events.ingress.event.spec.v1.LegalEntityIngestPushEvent;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LegalEntityIngestPushEventHandlerTest {
    @Mock
    private LegalEntityIngestionService legalEntityIngestionService;

    @Mock
    LegalEntityMapperImpl mapper;

    @Test
    void testHandleEvent_Completed() {
        Mono<LegalEntityIngestResponse> responseMono = Mono.just(
                LegalEntityIngestResponse
                        .builder().legalEntity(new LegalEntity().name("Legal Entity")).build());

        lenient().when(legalEntityIngestionService.ingestPush(any())).thenReturn(responseMono);

        LegalEntityConfigurationProperties properties = new LegalEntityConfigurationProperties();

        LegalEntityIngestPushEventHandler handler = new LegalEntityIngestPushEventHandler(
                legalEntityIngestionService,
                mapper);

        EnvelopedEvent<LegalEntityIngestPushEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(new LegalEntityIngestPushEvent());

        handler.handle(envelopedEvent);
        verify(legalEntityIngestionService).ingestPush(any());
    }
}
