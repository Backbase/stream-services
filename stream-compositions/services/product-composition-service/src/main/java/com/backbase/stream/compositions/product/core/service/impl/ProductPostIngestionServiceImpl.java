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

    private final ProductConfigurationProperties productConfigurationProperties;

    private final TransactionCompositionApi transactionCompositionApi;

    private final ProductGroupMapper productGroupMapper;

    @Override
    public void handleSuccess(ProductIngestResponse res) {
        log.info("Product ingestion completed successfully.");
        if (Boolean.TRUE.equals(productConfigurationProperties.getChains().getTransactionComposition().getEnableOnComplete())) {
            extractProducts(res.getProductGroup())
                    .doOnNext(this::sendTransactionPullEvent).subscribe();
            log.info("Call transaction-composition-service for all products {}", res.getProductGroup());
        }

        if (Boolean.TRUE.equals(productConfigurationProperties.getEvents().getEnableCompleted())) {
            ProductCompletedEvent event = new ProductCompletedEvent()
                    .withProductGroup(productGroupMapper.mapStreamToEvent(res.getProductGroup()));
            EnvelopedEvent<ProductCompletedEvent> envelopedEvent = new EnvelopedEvent<>();
            envelopedEvent.setEvent(event);
            eventBus.emitEvent(envelopedEvent);
        }

        if (log.isDebugEnabled()) {
            log.debug("Ingested Product: {}", res.getProductGroup());
        }
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
        List<String> excludeList = productConfigurationProperties
                .getChains().getTransactionComposition()
                .getExcludeProductTypeExternalIds();

        if (CollectionUtils.isEmpty(excludeList)) {
            return Boolean.TRUE;
        }

        return !excludeList.contains(product.getProductTypeExternalId());
    }

    @Override
    public Mono<ProductIngestResponse> handleFailure(Throwable error) {
        log.error("Product ingestion failed. {}", error.getMessage());
        if (Boolean.TRUE.equals(productConfigurationProperties.getEvents().getEnableFailed())) {
            ProductFailedEvent event = new ProductFailedEvent()
                    .withMessage(error.getMessage());
            EnvelopedEvent<ProductFailedEvent> envelopedEvent = new EnvelopedEvent<>();
            envelopedEvent.setEvent(event);
            eventBus.emitEvent(envelopedEvent);
        }
        return Mono.empty();
    }

    private void sendTransactionPullEvent(BaseProduct product) {
        TransactionPullIngestionRequest transactionPullIngestionRequest =
                new TransactionPullIngestionRequest()
                        .withExternalArrangementId(product.getExternalId());


        if (Boolean.FALSE.equals(productConfigurationProperties.getChains().getTransactionComposition().getAsync())) {
            transactionCompositionApi.pullTransactions(transactionPullIngestionRequest)
                    .onErrorResume(this::handleTransactionError)
                    .subscribe();
        } else {
            transactionCompositionApi.pullTransactionsAsync(transactionPullIngestionRequest)
                    .onErrorResume(this::handleAsyncTransactionError)
                    .subscribe();
        }

    }

    private Mono<Void> handleAsyncTransactionError(Throwable t) {
        log.error("Error while calling Transaction Composition asynchronously: {}", t.getMessage());
        return Mono.empty();
    }

    private Mono<TransactionIngestionResponse> handleTransactionError(Throwable t) {
        log.error("Error while calling Transaction Composition: {}", t.getMessage());
        throw new InternalServerErrorException(t.getMessage());
    }
}
