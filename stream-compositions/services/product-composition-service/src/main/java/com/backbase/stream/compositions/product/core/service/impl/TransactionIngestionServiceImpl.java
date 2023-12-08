package com.backbase.stream.compositions.product.core.service.impl;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.dbs.transaction.api.service.v2.TransactionPresentationServiceApi;
import com.backbase.dbs.transaction.api.service.v2.model.ArrangementItem;
import com.backbase.stream.compositions.events.ingress.event.spec.v1.TransactionsPullEvent;
import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties;
import com.backbase.stream.compositions.product.core.mapper.EventRequestsMapper;
import com.backbase.stream.compositions.product.core.service.TransactionIngestionService;
import com.backbase.stream.compositions.transaction.client.TransactionCompositionApi;
import com.backbase.stream.compositions.transaction.client.model.TransactionPullIngestionRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
            return refreshTransactions(res);
        }
        return transactionCompositionApi.pullTransactions(res)
            .doOnNext(response -> log.debug("Response from Transaction Composition: {}",
                response.getTransactions()));
    }

    @Override
    public Mono<?> ingestTransactionsAsync(TransactionPullIngestionRequest res) {
        if (properties.getChains().getTransactionManager().getEnabled()) {
            return Mono.just(res)
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(request -> refreshTransactions(request).subscribe())
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

    private Mono<Void> refreshTransactions(TransactionPullIngestionRequest res) {
        var arrangement = new ArrangementItem().arrangementId(res.getExternalArrangementId());
        return transactionManagerApi.postRefresh(List.of(arrangement), null, null, null)
            .doOnSuccess(r -> log.debug("Refreshed transactions for account {}", res.getExternalArrangementId()));
    }

}
