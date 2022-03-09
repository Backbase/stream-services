package com.backbase.stream.compositions.transaction.handlers;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.handler.EventHandler;
import com.backbase.com.backbase.stream.compositions.events.ingress.event.spec.v1.TransactionsIngestPushEvent;
import com.backbase.stream.compositions.transaction.core.mapper.TransactionMapper;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestPushRequest;
import com.backbase.stream.compositions.transaction.core.service.TransactionIngestionService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class TransactionIngestPushEventHandler implements EventHandler<TransactionsIngestPushEvent> {
    private final TransactionIngestionService transactionIngestionService;
    private final TransactionMapper mapper;

    @Override
    public void handle(EnvelopedEvent<TransactionsIngestPushEvent> envelopedEvent) {
        transactionIngestionService.ingestPush(buildRequest(envelopedEvent));
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param envelopedEvent EnvelopedEvent<TransactionsIngestPushEvent>
     * @return TransactionsIngestPushEvent
     */
    private Mono<TransactionIngestPushRequest> buildRequest(EnvelopedEvent<TransactionsIngestPushEvent> envelopedEvent) {
        return Mono.just(
                TransactionIngestPushRequest.builder()
                        .transactions(envelopedEvent.getEvent().getTransactions().stream().map(item -> mapper.mapEventToStream(item)).collect(Collectors.toList()))
                        .build());
    }
}
