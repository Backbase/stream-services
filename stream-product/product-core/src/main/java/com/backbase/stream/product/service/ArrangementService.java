package com.backbase.stream.product.service;

import static com.backbase.dbs.arrangement.api.service.v3.model.ArrangementsDeleteItem.SelectorEnum.EXTERNAL_ID;
import static java.lang.String.join;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.fromCallable;


import com.backbase.dbs.arrangement.api.integration.v3.model.ArrangementIdentification;
import com.backbase.dbs.arrangement.api.integration.v3.model.BatchResponseItem;
import com.backbase.dbs.arrangement.api.integration.v3.model.BatchResponseStatusCode;
import com.backbase.dbs.arrangement.api.integration.v3.model.BatchUpsertArrangement;
import com.backbase.dbs.arrangement.api.integration.v3.model.BatchUpsertResponse;
import com.backbase.dbs.arrangement.api.integration.v3.model.ErrorItem;
import com.backbase.dbs.arrangement.api.integration.v3.model.LegalEntitiesListPost;
import com.backbase.dbs.arrangement.api.integration.v3.model.LegalEntityExternal;
import com.backbase.dbs.arrangement.api.integration.v3.model.ArrangementPost;
import com.backbase.dbs.arrangement.api.integration.v3.model.LegalEntitiesDelete;
import com.backbase.dbs.arrangement.api.integration.v3.model.LegalEntityIdentification;
import com.backbase.dbs.arrangement.api.integration.v3.model.SubscriptionPost;
import com.backbase.dbs.arrangement.api.service.v3.ArrangementsApi;
import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementItem;
import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementPutItem;
import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementSearchesListResponse;
import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementsDeleteItem;
import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementsDeleteResponseElement;
import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementsSearchesPostRequest;
import com.backbase.stream.product.exception.ArrangementCreationException;
import com.backbase.stream.product.exception.ArrangementUpdateException;
import com.backbase.stream.product.mapping.ProductMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Manage Products (In DBS Called Arrangements).
 */
@SuppressWarnings("WeakerAccess")
@Slf4j
public class ArrangementService {

    private final ArrangementsApi arrangementsApi;
    private final com.backbase.dbs.arrangement.api.integration.v3.ArrangementsApi arrangementsIntegrationApi;
    private final ProductMapper productMapper = Mappers.getMapper(ProductMapper.class);

    public ArrangementService(ArrangementsApi arrangementsApi,
        com.backbase.dbs.arrangement.api.integration.v3.ArrangementsApi arrangementsIntegrationApi) {
        this.arrangementsApi = arrangementsApi;
        this.arrangementsIntegrationApi = arrangementsIntegrationApi;
    }

    public Mono<ArrangementItem> createArrangement(ArrangementPost postArrangement) {

        return arrangementsIntegrationApi.postArrangements(postArrangement)
            .doOnError(WebClientResponseException.class, throwable -> log.error("Failed to create arrangement: {}\n{}", postArrangement.getExternalId(), throwable.getResponseBodyAsString()))
            .onErrorMap(WebClientResponseException.class, throwable -> new ArrangementCreationException(throwable, "Failed to post arrangements"))
            .map(arrangementAddedResponse -> {
                ArrangementItem arrangementItem = productMapper.toArrangementItem(postArrangement);
                arrangementItem.setId(arrangementAddedResponse.getId());
                return arrangementItem;
            });

    }

    public Mono<ArrangementPutItem> updateArrangement(String arrangementId, ArrangementPutItem arrangementPutItem) {
        log.info("Updating Arrangement: {}", arrangementPutItem.getExternalArrangementId());
        if(arrangementPutItem.getDebitCards() == null) {
            arrangementPutItem.setDebitCards(emptyList());
        }
        return arrangementsApi.putArrangementById(arrangementId, arrangementPutItem)
            .doOnEach(aVoid -> log.info("Updated Arrangement: {}", arrangementPutItem.getExternalArrangementId()))
            .thenReturn(fromCallable(() -> arrangementPutItem))
            .thenReturn(arrangementPutItem)
            .onErrorResume(WebClientResponseException.class, throwable -> error(new ArrangementUpdateException(throwable, "Failed to update Arrangement: " + arrangementPutItem.getExternalArrangementId())));
    }

    public Flux<BatchResponseItem> upsertBatchArrangements(List<ArrangementPost> arrangementItems) {
        BatchUpsertArrangement batchUpsertArrangement = new BatchUpsertArrangement().arrangements(arrangementItems);
        return arrangementsIntegrationApi.postBatchUpsertArrangements(batchUpsertArrangement)
            .flatMapIterable(BatchUpsertResponse::getResults)
            .<BatchResponseItem>handle((r, sink) -> {
                    log.info("Batch Arrangement update result for arrangementId: {}, resourceId: {}, action: {}, result: {}", r.getArrangementId(), r.getArrangementExternalId(), r.getAction(), r.getStatus());
                    // Check if any failed, then fail everything.
                    if (!BatchResponseStatusCode.HTTP_STATUS_OK.equals(r.getStatus())) {
                        List<ErrorItem> errors = r.getErrors();
                        sink.error(new IllegalStateException("Batch arrangement update failed: '%s'; errors: %s"
                            .formatted(r.getArrangementId(), join(",", errors.stream().map(ErrorItem::toString).toList()))));
                        return;
                    }
                    sink.next(r);
            }).onErrorResume(WebClientResponseException.class, throwable ->
                Mono.error(new ArrangementUpdateException(throwable,
                    "Batch arrangement update failed for arrangements : "
                        + arrangementItems.stream()
                        .map(arrangementItem -> {
                            String uniqueIdentifier = Objects.nonNull(arrangementItem.getBBAN())? arrangementItem.getBBAN(): arrangementItem.getExternalId();
                            return arrangementItem.getName() + " | "
                                + uniqueIdentifier.substring(uniqueIdentifier.length() - 4);
                        }).toList())));
    }

    /**
     * Get Product by Internal ID.
     *
     * @param internalId Internal ID
     * @return Product
     */
    public Mono<ArrangementItem> getArrangement(String internalId) {
        return arrangementsApi.getArrangementById(internalId, false)
            .onErrorResume(WebClientResponseException.NotFound.class, ex -> {
                log.info("Arrangement: {} not found", internalId);
                return Mono.empty();
            });
    }

    public Flux<ArrangementItem> getArrangementByExternalId(List<String> externalId) {
        ArrangementsSearchesPostRequest arrangementsSearchesPostRequest = new ArrangementsSearchesPostRequest();
        arrangementsSearchesPostRequest.setExternalArrangementIds(new HashSet<>(externalId));
        return arrangementsApi.postSearchArrangements(arrangementsSearchesPostRequest)
            .flatMapIterable(ArrangementSearchesListResponse::getArrangementElements);
    }


    public Mono<ArrangementItem> getArrangementByExternalId(String externalId) {
        return getArrangementByExternalId(singletonList(externalId)).next();
    }

    public Mono<String> getArrangementInternalId(String externalId) {
        log.info("Checking if arrangement exists with externalId: {}", externalId);
        ArrangementsSearchesPostRequest arrangementsSearchesPostRequest = new ArrangementsSearchesPostRequest();
        arrangementsSearchesPostRequest.setExternalArrangementIds(Set.of(externalId));
        return arrangementsApi.postSearchArrangements(arrangementsSearchesPostRequest)
            .doOnNext(response -> {
                String internalId = response.getArrangementElements().getFirst().getId();
                log.info("Found Arrangement internalId: {} for externalId: {}", internalId, externalId);
            })
            .onErrorResume(WebClientResponseException.NotFound.class, notFound -> {
                log.info("Arrangement not found with externalId: {}", externalId);
                return Mono.empty();
            }).onErrorResume(WebClientResponseException.class, exception -> {
                log.info("Exception while getting product by externalId: {}, {}", externalId, exception.getResponseBodyAsString());
                return Mono.empty();
            }).map(arrangementSearchesListResponse -> arrangementSearchesListResponse.getArrangementElements()
                .stream().map(ArrangementItem::getId).toList().getFirst());
    }

    /**
     * Delete arrangement identified by Internal ID.
     *
     * @param arrangementInternalId arrangement internal identifier
     * @return internal identifier of removed arrangement.
     */
    public Mono<String> deleteArrangementByInternalId(String arrangementInternalId) {
        log.debug("Retrieving Arrangement by internal id {}", arrangementInternalId);
        // get arrangement externalId by internal id.
        return arrangementsApi.getArrangementById(arrangementInternalId, false)
                .mapNotNull(ArrangementItem::getExternalArrangementId)
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.warn("Failed to retrieve arrangement by internal id {}, {}", arrangementInternalId, e.getMessage());
                    return Mono.empty();
                })
                // remove arrangement.
                .flatMap(this::deleteArrangementByExternalId)
                .thenReturn(arrangementInternalId);
    }

    /**
     * Delete arrangement identified by External ID.
     *
     * @param arrangementExternalId arrangement external identifier
     * @return external identifier of removed arrangement.
     */
    public Mono<String> deleteArrangementByExternalId(String arrangementExternalId) {
        log.debug("Removing Arrangement with external id {}", arrangementExternalId);
        Set<ArrangementsDeleteItem> arrangementsDeleteItemSet = new HashSet<>();
        ArrangementsDeleteItem arrangementsDeleteItem = new ArrangementsDeleteItem();
        arrangementsDeleteItem.setSelector(EXTERNAL_ID);
        arrangementsDeleteItem.setValue(arrangementExternalId);
        arrangementsDeleteItemSet.add(arrangementsDeleteItem);
        return arrangementsApi.postDelete(arrangementsDeleteItemSet)
            .filter(arrangementsDeleteResponseElement -> EXTERNAL_ID.getValue().equals(arrangementsDeleteResponseElement.getSelector().getValue()))
            .map(ArrangementsDeleteResponseElement::getValue)
            .collectList().map(List::getFirst);
    }

    /**
     * Assign Arrangement with specified Legal Entities.
     *
     * @param arrangementExternalId external id of Arrangement.
     * @param legalEntitiesExternalIds set of Legal Entities external identifiers.
     * @return Mono<Void>
     */
    public Mono<Void> addLegalEntitiesForArrangement(String arrangementExternalId, @NonNull Set<String> legalEntitiesExternalIds) {
        Set<LegalEntityExternal> externalLegalEntitySet = legalEntitiesExternalIds.stream()
            .map(legalEntitiesExternalId -> new LegalEntityExternal().externalId(legalEntitiesExternalId))
            .collect(Collectors.toSet());
        log.debug("Attaching Arrangement {} to Legal Entities: {}", arrangementExternalId, externalLegalEntitySet.stream().map(LegalEntityExternal::getExternalId).toList());
        LegalEntitiesListPost legalEntitiesListPost = new LegalEntitiesListPost()
            .arrangement(new ArrangementIdentification().externalId(arrangementExternalId))
            .legalEntities(externalLegalEntitySet);

        return arrangementsIntegrationApi.postArrangementLegalEntities(legalEntitiesListPost);
    }

    /**
     * Detach specified Arrangement from Legal Entities.
     *
     * @param arrangementExternalId arrangement external identifier.
     * @param legalEntityExternalIds List of Legal Entities identified by external identifier.
     * @return Mono<Void>
     */
    public Mono<Void> removeLegalEntityFromArrangement(String arrangementExternalId,
        List<String> legalEntityExternalIds) {
        log.debug("Removing Arrangement {} from Legal Entities {}", arrangementExternalId, legalEntityExternalIds);

        if (CollectionUtils.isEmpty(legalEntityExternalIds)) {
            return Mono.empty();
        }

        Set<LegalEntityIdentification> legalEntityIdentifications = legalEntityExternalIds.stream()
            .map(legalEntityExternalId -> new LegalEntityIdentification().externalId(legalEntityExternalId))
            .collect(Collectors.toSet());

        LegalEntitiesDelete legalEntitiesDelete = new LegalEntitiesDelete().legalEntities(legalEntityIdentifications);

        return arrangementsIntegrationApi.deleteArrangementLegalEntities(legalEntitiesDelete);
    }

    /**
     * Create Subscriptions for specified Arrangement and list of subscription identifiers.
     *
     * @param arrangementExternalId arrangement external identifier.
     * @param subscriptionIdentifiers List of identifiers.
     * @return Mono<Void>
     */
    public Mono<Void> addSubscriptionForArrangement(
        String arrangementExternalId, List<String> subscriptionIdentifiers) {

        return Flux.fromIterable(subscriptionIdentifiers)
            .flatMap(identifier -> {
                    log.debug("Subscribe arrangement [{}] to subscription '{}'", arrangementExternalId, identifier);
                    SubscriptionPost subscriptionPost = new SubscriptionPost()
                        .arrangement(new ArrangementIdentification().externalId(arrangementExternalId))
                        .identifier(identifier);
                    return arrangementsIntegrationApi.postSubscription(subscriptionPost)
                        .onErrorResume(WebClientResponseException.class, e -> {
                            log.warn("Failed to create subscription '{}' for arrangement [{}]: {}",
                                identifier,
                                arrangementExternalId,
                                e.getMessage()
                            );
                            return Mono.empty();
                        });
                }
            ).then();
    }

}
