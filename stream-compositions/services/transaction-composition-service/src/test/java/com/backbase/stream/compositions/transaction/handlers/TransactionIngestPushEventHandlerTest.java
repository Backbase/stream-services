package com.backbase.stream.compositions.transaction.handlers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostResponseBody;
import com.backbase.stream.compositions.events.ingress.event.spec.v1.TransactionsPushEvent;
import com.backbase.stream.compositions.transaction.core.mapper.TransactionMapper;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestResponse;
import com.backbase.stream.compositions.transaction.core.service.TransactionIngestionService;
import java.util.List;
import java.util.Map;

import com.backbase.stream.compositions.transaction.handlers.TransactionIngestPushEventHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class TransactionIngestPushEventHandlerTest {

  @Mock
  private TransactionIngestionService transactionIngestionService;

  @Mock
  TransactionMapper mapper;

  @Test
  void testHandleEvent_Completed() {

    Mono<TransactionIngestResponse> responseMono = Mono.just(
        TransactionIngestResponse.builder()
            .transactions(List.of(new TransactionsPostResponseBody().id("1")
                .externalId("externalId").additions(Map.of()))).build());

    lenient().when(transactionIngestionService.ingestPush(any())).thenReturn(responseMono);

    TransactionIngestPushEventHandler handler = new TransactionIngestPushEventHandler(
        transactionIngestionService,
        mapper);

    EnvelopedEvent<TransactionsPushEvent> envelopedEvent = new EnvelopedEvent<>();
    envelopedEvent.setEvent(new TransactionsPushEvent());

    handler.handle(envelopedEvent);
    verify(transactionIngestionService).ingestPush(any());
  }

}
