package com.backbase.stream.compositions.product.core.service.impl;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.buildingblocks.presentation.errors.InternalServerErrorException;
import com.backbase.com.backbase.stream.compositions.events.egress.event.spec.v1.ProductCompletedEvent;
import com.backbase.com.backbase.stream.compositions.events.egress.event.spec.v1.ProductFailedEvent;
import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties;
import com.backbase.stream.compositions.product.core.mapper.ProductGroupMapper;
import com.backbase.stream.compositions.product.core.model.ProductIngestResponse;
import com.backbase.stream.compositions.product.core.service.ProductPostIngestionService;
import com.backbase.stream.compositions.transaction.client.TransactionCompositionApi;
import com.backbase.stream.compositions.transaction.client.model.TransactionIngestionResponse;
import com.backbase.stream.compositions.transaction.client.model.TransactionPullIngestionRequest;
import com.backbase.stream.legalentity.model.BaseProduct;
import com.backbase.stream.legalentity.model.ProductGroup;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@AllArgsConstructor
public class ProductPostIngestionServiceImpl implements ProductPostIngestionService {

    private final EventBus eventBus;

    private final ProductConfigurationProperties config;

    private final TransactionCompositionApi transactionCompositionApi;

    private final ProductGroupMapper mapper;

    @Override
    public Mono<ProductIngestResponse> handleSuccess(ProductIngestResponse res) {
        return Mono.just(res)
                .doOnNext(r -> log.info("Product ingestion completed successfully for SA {}",
                        res.getProductGroup().getServiceAgreement().getInternalId()))
                .flatMap(this::processChains)
                .doOnNext(this::processSuccessEvent)
                .doOnNext(r -> {
                    log.debug("Ingested products: {}", res.getProductGroup());
                });
    }

    @Override
    public void handleFailure(Throwable error) {
        log.error("Product ingestion failed. {}", error.getMessage());
        if (Boolean.TRUE.equals(config.getEvents().getEnableFailed())) {
            ProductFailedEvent event = new ProductFailedEvent()
                    .withMessage(error.getMessage());
            EnvelopedEvent<ProductFailedEvent> envelopedEvent = new EnvelopedEvent<>();
            envelopedEvent.setEvent(event);
            eventBus.emitEvent(envelopedEvent);
        }
    }

    private Mono<ProductIngestResponse> processChains(ProductIngestResponse res) {
        Mono<ProductIngestResponse> transactionChainMono;

        if (!config.isTransactionChainEnabled()) {
            log.debug("Transaction Chain is disabled");
            transactionChainMono = Mono.just(res);
        } else if (config.isTransactionChainAsync()) {
            transactionChainMono = ingestTransactionsAsync(res);
        } else {
            transactionChainMono = ingestTransactions(res);
        }

        return transactionChainMono;

    }

    private Mono<ProductIngestResponse> ingestTransactions(ProductIngestResponse res) {
        return extractProducts(res.getProductGroup())
                .map(this::buildTransactionPullRequest)
                .flatMap(transactionCompositionApi::pullTransactions)
                .onErrorResume(this::handleTransactionError)
                .doOnNext(response -> {
                    log.debug("Response from Transaction Composition: {}",
                            response.getTransactions());
                })
                .collectList()
                .map(p -> res);
    }

    private Mono<ProductIngestResponse> ingestTransactionsAsync(ProductIngestResponse res) {
        return extractProducts(res.getProductGroup())
                .map(this::buildTransactionPullRequest)
                .doOnNext(request -> transactionCompositionApi.pullTransactions(request).subscribe())
                .doOnNext(t -> log.info("Async transaction ingestion called for arrangement: {}",
                        t.getArrangementId()))
                .collectList()
                .map(p -> res);
    }

    private void processSuccessEvent(ProductIngestResponse res) {
        if (Boolean.TRUE.equals(config.isCompletedEventEnabled())) {
            ProductCompletedEvent event = new ProductCompletedEvent()
                    .withProductGroup(mapper.mapStreamToEvent(res.getProductGroup()));
            EnvelopedEvent<ProductCompletedEvent> envelopedEvent = new EnvelopedEvent<>();
            envelopedEvent.setEvent(event);
            eventBus.emitEvent(envelopedEvent);
        }
    }

    private Mono<TransactionIngestionResponse> handleTransactionError(Throwable t) {
        log.error("Error while calling Transaction Composition: {}", t.getMessage());
        return Mono.error(new InternalServerErrorException(t.getMessage()));
    }

    private Flux<BaseProduct> extractProducts(ProductGroup productGroup) {
        return Flux.concat(
                Flux.fromIterable(Optional.ofNullable(productGroup.getLoans())
                        .orElseGet(Collections::emptyList)),
                Flux.fromIterable(Optional.ofNullable(productGroup.getTermDeposits())
                        .orElseGet(Collections::emptyList)),
                Flux.fromIterable(Optional.ofNullable(productGroup.getCurrentAccounts())
                        .orElseGet(Collections::emptyList)),
                Flux.fromIterable(Optional.ofNullable(productGroup.getSavingAccounts())
                        .orElseGet(Collections::emptyList)),
                Flux.fromIterable(Optional.ofNullable(productGroup.getCreditCards())
                        .orElseGet(Collections::emptyList)),
                Flux.fromIterable(Optional.ofNullable(productGroup.getInvestmentAccounts())
                        .orElseGet(Collections::emptyList)),
                Flux.fromIterable(Optional.ofNullable(productGroup.getCustomProducts())
                        .orElseGet(Collections::emptyList)))
                .filter(this::excludeProducts);
    }

    private Boolean excludeProducts(BaseProduct product) {
        List<String> excludeList = config
                .getChains().getTransactionComposition()
                .getExcludeProductTypeExternalIds();

        if (CollectionUtils.isEmpty(excludeList)) {
            return Boolean.TRUE;
        }

        return !excludeList.contains(product.getProductTypeExternalId());
    }

    private TransactionPullIngestionRequest buildTransactionPullRequest(BaseProduct product) {
        return new TransactionPullIngestionRequest()
                .withLegalEntityInternalId(product.getLegalEntities().get(0).getInternalId())
                .withArrangementId(product.getInternalId())
                        .withExternalArrangementId(product.getExternalId());
    }
}
