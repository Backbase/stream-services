package com.backbase.stream.compositions.product.core.service.impl;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.buildingblocks.presentation.errors.InternalServerErrorException;
import com.backbase.com.backbase.stream.compositions.events.egress.event.spec.v1.ProductCompletedEvent;
import com.backbase.com.backbase.stream.compositions.events.egress.event.spec.v1.ProductFailedEvent;
import com.backbase.stream.compositions.paymentorder.client.PaymentOrderCompositionApi;
import com.backbase.stream.compositions.paymentorder.client.model.PaymentOrderIngestionResponse;
import com.backbase.stream.compositions.paymentorder.client.model.PaymentOrderPullIngestionRequest;
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

@Service
@Slf4j
@AllArgsConstructor
public class ProductPostIngestionServiceImpl implements ProductPostIngestionService {

    private final EventBus eventBus;

    private final ProductConfigurationProperties config;

    private final TransactionCompositionApi transactionCompositionApi;

    private final PaymentOrderCompositionApi paymentOrderCompositionApi;

    private final ProductGroupMapper mapper;

    @Override
    public Mono<ProductIngestResponse> handleSuccess(ProductIngestResponse res) {
        return Mono.just(res)
                .doOnNext(r -> log.info("Product ingestion completed successfully for SA {}",
                        res.getServiceAgreementInternalId()))
                .flatMap(this::processTransactionChains)
                .flatMap(this::processPaymentOrderChains)
                .doOnNext(this::processSuccessEvent)
                .doOnNext(r -> {
                    log.debug("Ingested product groups: {}", res.getProductGroups());
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

    private Mono<ProductIngestResponse> processTransactionChains(ProductIngestResponse res) {
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

    private Mono<ProductIngestResponse> processPaymentOrderChains(ProductIngestResponse res) {
        Mono<ProductIngestResponse> paymentOrderChainMono;

        if (!config.isPaymentOrderChainEnabled()) {
            log.debug("Payment Order Chain is disabled");
            paymentOrderChainMono = Mono.just(res);
        } else if (config.isPaymentOrderChainAsync()) {
            paymentOrderChainMono = ingestPaymentOrderAsync(res);
        } else {
            paymentOrderChainMono = ingestPaymentOrder(res);
        }

        return paymentOrderChainMono;

    }

    private Mono<ProductIngestResponse> ingestTransactions(ProductIngestResponse res) {
        return extractProducts(res.getProductGroups())
                .map(product -> buildTransactionPullRequest(product, res))
                .flatMap(transactionCompositionApi::pullTransactions)
                .onErrorResume(this::handleTransactionError)
                .doOnNext(response -> {
                    log.debug("Response from Transaction Composition: {}",
                            response.getTransactions());
                })
                .collectList()
                .map(p -> res);
    }

    private Mono<ProductIngestResponse> ingestPaymentOrder(ProductIngestResponse res) {
        return Mono.just(buildPaymentOrderPullRequest(res))
                .flatMap(paymentOrderCompositionApi::pullPaymentOrder)
                .onErrorResume(this::handlePaymentOrderError)
                .doOnNext(response -> {
                    log.debug("Response from Payment Order Composition: {} ",
                            response.getNewPaymentOrder());
                })
                .map(p -> res);
    }

    private Mono<ProductIngestResponse> ingestTransactionsAsync(ProductIngestResponse res) {
        return extractProducts(res.getProductGroups())
                .map(product -> buildTransactionPullRequest(product, res))
                .doOnNext(request -> transactionCompositionApi.pullTransactions(request).subscribe())
                .doOnNext(t -> log.info("Async transaction ingestion called for arrangement: {}",
                        t.getArrangementId()))
                .collectList()
                .map(p -> res);
    }

    private Mono<ProductIngestResponse> ingestPaymentOrderAsync(ProductIngestResponse res) {
        return Mono.just(buildPaymentOrderPullRequest(res))
                .doOnNext(request -> paymentOrderCompositionApi.pullPaymentOrder(request).subscribe())
                .doOnNext(t -> log.info("Async payment order ingestion called for Legal Entity: {}",
                        t.getLegalEntityExternalId()))
                .map(p -> res);
    }

    private void processSuccessEvent(ProductIngestResponse res) {
        if (Boolean.TRUE.equals(config.isCompletedEventEnabled())) {
            ProductCompletedEvent event = new ProductCompletedEvent()
                    .withProductGroups(res.getProductGroups().stream().map(p -> mapper.mapStreamToEvent(p)).collect(Collectors.toList()));
            EnvelopedEvent<ProductCompletedEvent> envelopedEvent = new EnvelopedEvent<>();
            envelopedEvent.setEvent(event);
            eventBus.emitEvent(envelopedEvent);
        }
    }

    private Mono<TransactionIngestionResponse> handleTransactionError(Throwable t) {
        log.error("Error while calling Transaction Composition: {}", t.getMessage());
        return Mono.error(new InternalServerErrorException(t.getMessage()));
    }

    private Mono<PaymentOrderIngestionResponse> handlePaymentOrderError(Throwable t) {
        log.error("Error while calling Payment Order Composition: {}", t.getMessage());
        return Mono.error(new InternalServerErrorException(t.getMessage()));
    }

    private Flux<BaseProduct> extractProducts(List<ProductGroup> productGroups) {
        return Flux.concat(
                        Flux.fromIterable(Optional.of(productGroups.stream()
                                        .flatMap(group -> productStream(group.getLoans()))
                                        .collect(Collectors.toList()))
                                .orElseGet(Collections::emptyList)),
                        Flux.fromIterable(Optional.of(productGroups.stream()
                                        .flatMap(group -> productStream(group.getTermDeposits()))
                                        .collect(Collectors.toList()))
                                .orElseGet(Collections::emptyList)),
                        Flux.fromIterable(Optional.of(productGroups.stream()
                                        .flatMap(group -> productStream(group.getCurrentAccounts()))
                                        .collect(Collectors.toList()))
                                .orElseGet(Collections::emptyList)),
                        Flux.fromIterable(Optional.of(productGroups.stream()
                                        .flatMap(group -> productStream(group.getSavingAccounts()))
                                        .collect(Collectors.toList()))
                                .orElseGet(Collections::emptyList)),
                        Flux.fromIterable(Optional.of(productGroups.stream()
                                        .flatMap(group -> productStream(group.getCreditCards()))
                                        .collect(Collectors.toList()))
                                .orElseGet(Collections::emptyList)),
                        Flux.fromIterable(Optional.of(productGroups.stream()
                                        .flatMap(group -> productStream(group.getInvestmentAccounts()))
                                        .collect(Collectors.toList()))
                                .orElseGet(Collections::emptyList)),
                        Flux.fromIterable(Optional.of(productGroups.stream()
                                        .flatMap(group -> productStream(group.getCustomProducts()))
                                        .collect(Collectors.toList()))
                                .orElseGet(Collections::emptyList)))
                .filter(this::excludeProducts);
    }

    private Stream<? extends BaseProduct> productStream(List<? extends BaseProduct> products) {
        return Optional.ofNullable(products)
                .map(Collection::stream)
                .orElseGet(Stream::empty);
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

    private TransactionPullIngestionRequest buildTransactionPullRequest(BaseProduct product, ProductIngestResponse res) {
        return new TransactionPullIngestionRequest()
                .withLegalEntityInternalId(product.getLegalEntities().get(0).getInternalId())
                .withAdditions(res.getAdditions())
                .withArrangementId(product.getInternalId())
                .withExternalArrangementId(product.getExternalId());
    }

    private PaymentOrderPullIngestionRequest buildPaymentOrderPullRequest(ProductIngestResponse res) {
        return new PaymentOrderPullIngestionRequest()
                .withLegalEntityInternalId(res.getLegalEntityInternalId())
                .withLegalEntityExternalId(res.getLegalEntityExternalId())
                .withInternalUserId(res.getUserInternalId())
                .withMemberNumber(res.getUserExternalId())
                .withServiceAgreementInternalId(res.getServiceAgreementInternalId())
                .withAdditions(res.getAdditions());
    }
}
