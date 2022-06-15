package com.backbase.stream.compositions.transaction.handlers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostResponseBody;
import com.backbase.stream.compositions.events.ingress.event.spec.v1.TransactionsPullEvent;
import com.backbase.stream.compositions.transaction.core.config.TransactionConfigurationProperties;
import com.backbase.stream.compositions.transaction.core.config.TransactionConfigurationProperties.Events;
import com.backbase.stream.compositions.transaction.core.mapper.TransactionMapper;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestResponse;
import com.backbase.stream.compositions.transaction.core.service.TransactionIngestionService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class TransactionIngestPullEventHandlerTest {

  @Mock
  private TransactionIngestionService transactionIngestionService;

  @Mock
  TransactionMapper mapper;

  @Mock
  EventBus eventBus;

  @Test
  @Tag("true")
  void testEnableHandleEvent_Completed(TestInfo testInfo) {
    String eventsConfig = testInfo.getTags().stream().findFirst().orElse("false");
    testHandleEvent_Completed(Boolean.valueOf(eventsConfig));
  }

  @Test
  @Tag("false")
  void testDisableHandleEvent_Completed(TestInfo testInfo) {
    String eventsConfig = testInfo.getTags().stream().findFirst().orElse("false");
    testHandleEvent_Completed(Boolean.valueOf(eventsConfig));
  }

  @Test
  @Tag("true")
  void testEnableHandleEvent_Failed(TestInfo testInfo) {
    String eventsConfig = testInfo.getTags().stream().findFirst().orElse("false");
    testHandleEvent_Failed(Boolean.valueOf(eventsConfig));
  }

  @Test
  @Tag("false")
  void testDisableHandleEvent_Failed(TestInfo testInfo) {
    String eventsConfig = testInfo.getTags().stream().findFirst().orElse("false");
    testHandleEvent_Failed(Boolean.valueOf(eventsConfig));
  }


  void testHandleEvent_Completed(Boolean isCompletedEvents) {

    Mono<TransactionIngestResponse> responseMono = Mono.just(
        TransactionIngestResponse.builder()
            .transactions(List.of(new TransactionsPostResponseBody().id("1")
                    .externalId("externalId1").additions(Map.of()),
                new TransactionsPostResponseBody().id("2")
                    .externalId("externalId2").additions(Map.of()))).build());

    lenient().when(transactionIngestionService.ingestPull(any())).thenReturn(responseMono);
    TransactionConfigurationProperties properties = new TransactionConfigurationProperties();
    Events events = new Events();
    events.setEnableCompleted(isCompletedEvents);
    properties.setEvents(events);

    TransactionIngestPullEventHandler handler = new TransactionIngestPullEventHandler(
        properties,
        transactionIngestionService,
        mapper,
        eventBus);

    EnvelopedEvent<TransactionsPullEvent> envelopedEvent = new EnvelopedEvent<>();
    envelopedEvent
        .setEvent(new TransactionsPullEvent().withExternalArrangementIds(List.of("ext1", "ext2")));

    handler.handle(envelopedEvent);
    verify(transactionIngestionService).ingestPull(any());
  }

  void testHandleEvent_Failed(Boolean isFailedEvents) {
    when(transactionIngestionService.ingestPull(any())).thenThrow(new RuntimeException());

    TransactionConfigurationProperties properties = new TransactionConfigurationProperties();
    Events events = new Events();
    events.setEnableFailed(isFailedEvents);
    properties.setEvents(events);

    TransactionIngestPullEventHandler handler = new TransactionIngestPullEventHandler(
        properties,
        transactionIngestionService,
        mapper,
        eventBus);

    EnvelopedEvent<TransactionsPullEvent> envelopedEvent = new EnvelopedEvent<>();
    TransactionsPullEvent event = new TransactionsPullEvent()
        .withExternalArrangementIds(List.of("1", "2"));
    envelopedEvent.setEvent(event);
    handler.handle(envelopedEvent);
  }
}
