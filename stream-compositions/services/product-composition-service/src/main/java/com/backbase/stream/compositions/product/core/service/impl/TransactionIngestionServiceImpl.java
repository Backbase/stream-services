package com.backbase.stream.compositions.product.core.service.impl;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.dbs.transaction.api.service.v3.TransactionPresentationServiceApi;
import com.backbase.dbs.transaction.api.service.v3.model.ArrangementItem;
import com.backbase.stream.compositions.events.ingress.event.spec.v1.TransactionsPullEvent;
import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties;
import com.backbase.stream.compositions.product.core.mapper.EventRequestsMapper;
import com.backbase.stream.compositions.product.core.service.TransactionIngestionService;
import com.backbase.stream.compositions.transaction.client.TransactionCompositionApi;
import com.backbase.stream.compositions.transaction.client.model.TransactionPullIngestionRequest;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionIngestionServiceImpl implements TransactionIngestionService {

    private final EventBus eventBus;
    private final EventRequestsMapper eventRequestsMapper;
    private final TransactionCompositionApi transactionCompositionApi;
    private final TransactionPresentationServiceApi transactionManagerApi;
    private final ProductConfigurationProperties properties;

    @Override
    public Mono<?> ingestTransactions(TransactionPullIngestionRequest res) {
        if (properties.getChains().getTransactionManager().getEnabled()) {
            return Mono.just(res)
                .map(r -> new ArrangementItem().arrangementId(r.getExternalArrangementId()))
                .flatMap(r -> refreshTransactions(List.of(r)));
        }
        return transactionCompositionApi.pullTransactions(res)
            .doOnNext(response -> log.debug("Response from Transaction Composition: {}",
                response.getTransactions()));
    }

    @Override
    public Flux<?> ingestTransactions(Flux<TransactionPullIngestionRequest> res) {
        if (properties.getChains().getTransactionManager().getEnabled()) {
            if (properties.getChains().getTransactionManager().getSplitPerArrangement()) {
                return res.flatMap(this::ingestTransactions,
                    properties.getChains().getTransactionManager().getConcurrency());
            }
            return res.map(r -> new ArrangementItem().arrangementId(r.getExternalArrangementId()))
                .collectList()
                .flatMap(this::refreshTransactions)
                .flux();
        }
        return res.flatMap(this::ingestTransactions);
    }

    @Override
    public Mono<?> ingestTransactionsAsync(TransactionPullIngestionRequest res) {
        if (properties.getChains().getTransactionManager().getEnabled()) {
            return Mono.just(res)
                .publishOn(Schedulers.boundedElastic())
                .map(request -> new ArrangementItem().arrangementId(request.getExternalArrangementId()))
                .doOnNext(request -> refreshTransactions(List.of(request)).subscribe())
                .doOnNext(
                    t -> log.info("Async transaction ingestion called for arrangement: {}", res.getArrangementId()));
        }
        return Mono.just(eventRequestsMapper.map(res))
            .map(e -> {
                var event = new EnvelopedEvent<TransactionsPullEvent>();
                event.setEvent(e);
                return event;
            })
            .doOnNext(eventBus::emitEvent)
            .doOnNext(t -> log.info("Async transaction ingestion called for arrangement via composition: {}",
                t.getEvent().getArrangementId()));
    }

    @Override
    public Flux<?> ingestTransactionsAsync(Flux<TransactionPullIngestionRequest> res) {
        if (properties.getChains().getTransactionManager().getEnabled()
            && !properties.getChains().getTransactionManager().getSplitPerArrangement()) {

            return res.map(r -> new ArrangementItem().arrangementId(r.getExternalArrangementId()))
                .publishOn(Schedulers.boundedElastic())
                .collectList()
                .doOnNext(request -> refreshTransactions(request).subscribe())
                .doOnNext(t -> log.info("Async transaction ingestion called for one or more arrangements"))
                .flux();
        }
        return res.flatMap(this::ingestTransactionsAsync);
    }

    private Mono<Void> refreshTransactions(List<ArrangementItem> res) {
        return transactionManagerApi.postRefresh(res, null, null, null)
            .doOnSuccess(r -> log.debug("Refreshed transactions for accounts {}",
                res.stream()
                    .map(ArrangementItem::getArrangementId)
                    .collect(Collectors.joining(","))));
    }

}
