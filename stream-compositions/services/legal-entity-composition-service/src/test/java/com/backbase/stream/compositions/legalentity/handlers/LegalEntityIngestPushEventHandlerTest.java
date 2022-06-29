package com.backbase.stream.compositions.legalentity.handlers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.stream.compositions.events.ingress.event.spec.v1.LegalEntityPushEvent;
import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapperImpl;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityResponse;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityIngestionService;
import com.backbase.stream.legalentity.model.LegalEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class LegalEntityIngestPushEventHandlerTest {

  @Mock
  private LegalEntityIngestionService legalEntityIngestionService;

  @Mock
  LegalEntityMapperImpl mapper;

  @Test
  void testHandleEvent_Completed() {
    Mono<LegalEntityResponse> responseMono = Mono.just(
        LegalEntityResponse
            .builder().legalEntity(new LegalEntity().name("Legal Entity")).build());

    lenient().when(legalEntityIngestionService.ingestPush(any())).thenReturn(responseMono);

    LegalEntityPushEventHandler handler = new LegalEntityPushEventHandler(
        legalEntityIngestionService,
        mapper);

    EnvelopedEvent<LegalEntityPushEvent> envelopedEvent = new EnvelopedEvent<>();
    envelopedEvent.setEvent(new LegalEntityPushEvent());

    handler.handle(envelopedEvent);
    verify(legalEntityIngestionService).ingestPush(any());
  }
}
