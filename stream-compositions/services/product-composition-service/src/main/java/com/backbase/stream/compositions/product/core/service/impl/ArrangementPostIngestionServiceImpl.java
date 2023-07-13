package com.backbase.stream.compositions.product.core.service.impl;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.buildingblocks.presentation.errors.InternalServerErrorException;
import com.backbase.stream.compositions.events.egress.event.spec.v1.ArrangementCompletedEvent;
import com.backbase.stream.compositions.events.egress.event.spec.v1.ProductFailedEvent;
import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties;
import com.backbase.stream.compositions.product.core.model.ArrangementIngestResponse;
import com.backbase.stream.compositions.product.core.service.ArrangementPostIngestionService;
import com.backbase.stream.compositions.transaction.client.TransactionCompositionApi;
import com.backbase.stream.compositions.transaction.client.model.TransactionIngestionResponse;
import com.backbase.stream.compositions.transaction.client.model.TransactionPullIngestionRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@Slf4j
@AllArgsConstructor
public class ArrangementPostIngestionServiceImpl implements ArrangementPostIngestionService {

    private final EventBus eventBus;
    private final ProductConfigurationProperties config;
    private final TransactionCompositionApi transactionCompositionApi;

    @Override
    public Mono<ArrangementIngestResponse> handleSuccess(ArrangementIngestResponse res) {
        return Mono.just(res)
                .doOnNext(r -> log.info("Arrangement ingestion completed successfully for external id {}",
                        res.getArrangement().getExternalArrangementId()))
                .flatMap(this::processTransactionChains)
                .doOnNext(this::processSuccessEvent)
                .doOnNext(r -> log.debug("Ingested arrangement: {}", res.getArrangement()));
    }

    @Override
    public void handleFailure(Throwable error) {
        log.error("Arrangement ingestion failed. {}", error.getMessage());
        if (Boolean.TRUE.equals(config.getEvents().getEnableFailed())) {
            ProductFailedEvent event = new ProductFailedEvent()
                    .withMessage(error.getMessage());
            EnvelopedEvent<ProductFailedEvent> envelopedEvent = new EnvelopedEvent<>();
            envelopedEvent.setEvent(event);
            eventBus.emitEvent(envelopedEvent);
        }
    }

    private Mono<ArrangementIngestResponse> processTransactionChains(ArrangementIngestResponse res) {
        Mono<ArrangementIngestResponse> transactionChainMono;

        if (!config.isTransactionChainEnabled(res.getConfig())) {
            log.debug("Transaction Chain is disabled");
            transactionChainMono = Mono.just(res);
        } else if (config.isTransactionChainAsync(res.getConfig())) {
            transactionChainMono = ingestTransactionsAsync(res);
        } else {
            transactionChainMono = ingestTransactions(res);
        }

        return transactionChainMono;
    }

    private Mono<ArrangementIngestResponse> ingestTransactions(ArrangementIngestResponse res) {
        return Mono.just(res)
                .map(this::buildTransactionPullRequest)
                .flatMap(transactionCompositionApi::pullTransactions)
                .onErrorResume(this::handleTransactionError)
                .doOnNext(response -> log.debug("Response from Transaction Composition: {}", response.getTransactions()))
                .map(p -> res);
    }

    private Mono<ArrangementIngestResponse> ingestTransactionsAsync(ArrangementIngestResponse res) {
        return Mono.just(res)
                .map(this::buildTransactionPullRequest)
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(request -> transactionCompositionApi.pullTransactions(request).subscribe())
                .doOnNext(t -> log.info("Async transaction ingestion called for arrangement: {}",
                        t.getArrangementId()))
                .map(p -> res);
    }

    private void processSuccessEvent(ArrangementIngestResponse res) {
        if (Boolean.TRUE.equals(config.isCompletedEventEnabled())) {
            log.info("Emitting arrangement update completed event for externalArrangementId: {}, source: {}",
                    res.getArrangement().getExternalArrangementId(), res.getSource());
            ArrangementCompletedEvent event = new ArrangementCompletedEvent()
                    .withArrangementExternalId(res.getArrangement().getExternalArrangementId())
                    .withSource(res.getSource());
            EnvelopedEvent<ArrangementCompletedEvent> envelopedEvent = new EnvelopedEvent<>();
            envelopedEvent.setEvent(event);
            eventBus.emitEvent(envelopedEvent);
            log.info("Emitted arrangement update completed event for externalArrangementId: {}, source: {}",
                    res.getArrangement().getExternalArrangementId(), res.getSource());
        }
    }

    private Mono<TransactionIngestionResponse> handleTransactionError(Throwable t) {
        log.error("Error while calling Transaction Composition: {}", t.getMessage());
        return Mono.error(new InternalServerErrorException(t.getMessage()));
    }

    private TransactionPullIngestionRequest buildTransactionPullRequest(ArrangementIngestResponse res) {
        return new TransactionPullIngestionRequest()
                .withArrangementId(res.getArrangementInternalId())
                .withExternalArrangementId(res.getArrangement().getExternalArrangementId());
    }
}
