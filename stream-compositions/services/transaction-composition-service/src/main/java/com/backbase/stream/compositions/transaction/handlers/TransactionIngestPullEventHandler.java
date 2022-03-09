package com.backbase.stream.compositions.transaction.handlers;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.handler.EventHandler;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.com.backbase.stream.compositions.events.egress.event.spec.v1.TransactionsIngestCompletedEvent;
import com.backbase.com.backbase.stream.compositions.events.egress.event.spec.v1.TransactionsIngestFailedEvent;
import com.backbase.com.backbase.stream.compositions.events.ingress.event.spec.v1.TransactionsIngestPullEvent;
import com.backbase.stream.compositions.transaction.core.config.TransactionConfigurationProperties;
import com.backbase.stream.compositions.transaction.core.mapper.TransactionMapper;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestPullRequest;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestResponse;
import com.backbase.stream.compositions.transaction.core.service.TransactionIngestionService;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@AllArgsConstructor
@EnableConfigurationProperties(TransactionConfigurationProperties.class)
public class TransactionIngestPullEventHandler implements EventHandler<TransactionsIngestPullEvent> {
    private final TransactionConfigurationProperties configProperties;
    private final TransactionIngestionService productIngestionService;
    private final TransactionMapper mapper;
    private final EventBus eventBus;

    /**
     * Handles ProductsIngestPullEvent.
     *
     * @param envelopedEvent EnvelopedEvent<ProductsIngestPullEvent>
     */
    @Override
    public void handle(EnvelopedEvent<TransactionsIngestPullEvent> envelopedEvent) {
        productIngestionService
                .ingestPull(buildRequest(envelopedEvent.getEvent()))
                .doOnError(this::handleError)
                .subscribe(this::handleResponse);
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param event ProductsIngestPullEvent
     * @return ProductIngestPullRequest
     */
    private Mono<TransactionIngestPullRequest> buildRequest(TransactionsIngestPullEvent event) {
        return Mono.just(
                TransactionIngestPullRequest.builder()
                        .build());
    }

    /**
     * Handles reponse from ingestion service.
     *
     * @param response ProductIngestResponse
     */
    private void handleResponse(TransactionIngestResponse response) {
        if (Boolean.FALSE.equals(configProperties.getEnableCompletedEvents())) {
            return;
        }
        TransactionsIngestCompletedEvent event = new TransactionsIngestCompletedEvent()
                .withEventId(UUID.randomUUID().toString());

        EnvelopedEvent<TransactionsIngestCompletedEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(event);
        eventBus.emitEvent(envelopedEvent);
    }

    /**
     * Handles error from ingestion service.
     *
     * @param ex Throwable
     */
    private void handleError(Throwable ex) {
        if (Boolean.FALSE.equals(configProperties.getEnableFailedEvents())) {
            return;
        }
        TransactionsIngestFailedEvent event = new TransactionsIngestFailedEvent()
                .withEventId(UUID.randomUUID().toString())
                .withMessage(ex.getMessage());

        EnvelopedEvent<TransactionsIngestFailedEvent> envelopedEvent = new EnvelopedEvent<>();
        envelopedEvent.setEvent(event);
        eventBus.emitEvent(envelopedEvent);
    }
}
