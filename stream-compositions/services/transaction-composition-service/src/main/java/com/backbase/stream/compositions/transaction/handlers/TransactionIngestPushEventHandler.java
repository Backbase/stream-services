package com.backbase.stream.compositions.transaction.handlers;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.handler.EventHandler;
import com.backbase.stream.compositions.events.ingress.event.spec.v1.TransactionsPushEvent;
import com.backbase.stream.compositions.transaction.core.mapper.TransactionMapper;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestPushRequest;
import com.backbase.stream.compositions.transaction.core.service.TransactionIngestionService;
import java.util.Collections;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class TransactionIngestPushEventHandler implements EventHandler<TransactionsPushEvent> {
  private final TransactionIngestionService transactionIngestionService;
  private final TransactionMapper mapper;

  @Override
  public void handle(EnvelopedEvent<TransactionsPushEvent> envelopedEvent) {
    buildRequest(envelopedEvent).flatMap(transactionIngestionService::ingestPush).subscribe();
  }

  /**
   * Builds ingestion request for downstream service.
   *
   * @param envelopedEvent EnvelopedEvent<TransactionsIngestPushEvent>
   * @return TransactionsIngestPushEvent
   */
  private Mono<TransactionIngestPushRequest> buildRequest(
      EnvelopedEvent<TransactionsPushEvent> envelopedEvent) {
    return Mono.just(
        TransactionIngestPushRequest.builder()
            .transactions(
                Collections.singletonList(
                    mapper.mapPushEventToStream(
                        envelopedEvent.getEvent().getTransactionsPostRequestBody())))
            .build());
  }
}
