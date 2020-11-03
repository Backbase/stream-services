package com.backbase.stream.product.service;

import com.backbase.dbs.accounts.presentation.service.ApiClient;
import com.backbase.dbs.accounts.presentation.service.api.ArrangementsApi;
import com.backbase.dbs.accounts.presentation.service.model.ArrangemenItemBase;
import com.backbase.dbs.accounts.presentation.service.model.ArrangementItem;
import com.backbase.dbs.accounts.presentation.service.model.ArrangementItemPost;
import com.backbase.dbs.accounts.presentation.service.model.ArrangementItems;
import com.backbase.dbs.accounts.presentation.service.model.BatchResponseItemExtended;
import com.backbase.dbs.accounts.presentation.service.model.ExternalLegalEntityIds;
import com.backbase.dbs.accounts.presentation.service.model.InternalIdGetResponseBody;
import com.backbase.stream.product.exception.ArrangementCreationException;
import com.backbase.stream.product.exception.ArrangementUpdateException;
import com.backbase.stream.product.mapping.ProductMapper;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
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
    private final ProductMapper productMapper = Mappers.getMapper(ProductMapper.class);

    public ArrangementService(ArrangementsApi arrangementsApi) {
        this.arrangementsApi = arrangementsApi;
    }

    public Mono<ArrangementItem> createArrangement(ArrangementItemPost arrangementItemPost) {

        return arrangementsApi.postArrangements(arrangementItemPost)
            .doOnError(WebClientResponseException.class, throwable ->
                log.error("Failed to create arrangement: {}\n{}", arrangementItemPost.getExternalArrangementId(), throwable.getResponseBodyAsString()))
            .onErrorMap(WebClientResponseException.class, throwable -> new ArrangementCreationException(throwable, "Failed to post arrangements"))
            .map(arrangementAddedResponse -> {

                ArrangementItem arrangementItem = productMapper.toArrangementItem(arrangementItemPost);
                arrangementItem.setId(arrangementAddedResponse.getId());

                return arrangementItem;
            });

    }

    public Mono<ArrangemenItemBase> updateArrangement(ArrangemenItemBase arrangemenItemBase) {
        log.info("Updating Arrangement: {}", arrangemenItemBase.getExternalArrangementId());
        if (arrangemenItemBase.getDebitCards() == null)
            arrangemenItemBase.setDebitCards(Collections.emptyList());
        return arrangementsApi.putArrangements(arrangemenItemBase)
            .doOnNext(aVoid -> log.info("Updated Arrangement: {}", arrangemenItemBase.getExternalArrangementId())).map(aVoid -> arrangemenItemBase)
            .thenReturn(arrangemenItemBase)
            .onErrorResume(WebClientResponseException.class, throwable ->
                Mono.error(new ArrangementUpdateException(throwable, "Failed to update Arrangement: " + arrangemenItemBase.getExternalArrangementId())));

    }

    /**
     * Upsert list of arrangements using DBS batch upsert API.
     *
     * @param arrangementItems list of arrangements to be upserted.
     * @return flux of response items.
     */
    public Flux<BatchResponseItemExtended> upsertBatchArrangements(List<ArrangementItemPost> arrangementItems) {
        return arrangementsApi.postBatchUpsertArrangements(arrangementItems)
            .map(batchResponseItemExtended -> {
                log.info("Batch Arrangement update result for arrangementId: {}, resourceId: {}, action: {}, result: {}", batchResponseItemExtended.getArrangementId(), batchResponseItemExtended.getResourceId(), batchResponseItemExtended.getAction(), batchResponseItemExtended.getStatus());
                // Check if any failed, then fail everything.
                if (!BatchResponseItemExtended.StatusEnum._200.equals(batchResponseItemExtended.getStatus())) {
                    log.error("Failed to batch arrangements:  " + arrangementItems);
                    batchResponseItemExtended.getErrors().forEach(error -> {
                        log.error("\t[{}]: {}", error.getKey(), error.getMessage());
                    });


                    throw new IllegalStateException("Batch arrangement update failed: " + batchResponseItemExtended.getResourceId());
                }
                return batchResponseItemExtended;
            })
            .onErrorResume(WebClientResponseException.class, throwable ->

                Mono.error(new ArrangementUpdateException(throwable, "Batch arrangement update failed: " + arrangementItems)));
    }

    /**
     * Get Product by Internal ID.
     *
     * @param internalId Internal ID
     * @return Product
     */
    public Mono<ArrangementItem> getArrangement(String internalId) {
        return arrangementsApi.getArrangementById(internalId)
            .onErrorResume(WebClientResponseException.NotFound.class, ex -> {
                log.info("Arrangement: {} not found", internalId);
                return Mono.empty();
            });
    }

    public Flux<ArrangementItem> getArrangementByExternalId(List<String> externalId) {
        Flux<ArrangementItem> executeRequest = arrangementsApi.getArrangements(null, null, externalId)
            .flatMapIterable(ArrangementItems::getArrangementElements);
        return executeRequest;
    }


    public Mono<ArrangementItem> getArrangementByExternalId(String externalId) {
        return getArrangementByExternalId(Collections.singletonList(externalId)).next();
    }

    public Mono<String> getArrangementInternalId(String externalId) {
        log.info("Checking if arrangement exists with externalId: {}", externalId);
        return arrangementsApi.getInternalId(externalId)
            .doOnNext(response ->
                log.info("Found Arrangement internalId: {} for externalId: {}", response.getInternalId(), externalId))
            .onErrorResume(WebClientResponseException.NotFound.class, notFound -> {
                log.info("Arrangement not found with externalId: {}", externalId);
                return Mono.empty();
            })
            .onErrorResume(WebClientResponseException.class, exception -> {
                log.info("Exception while getting product by externalId: {}, {}", externalId, exception.getResponseBodyAsString());
                return Mono.empty();
            })

            .map(InternalIdGetResponseBody::getInternalId);
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
        return arrangementsApi.getArrangementById(arrangementInternalId)
            .map(ArrangementItem::getExternalArrangementId)
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
        return arrangementsApi.deleteexternalArrangementId(arrangementExternalId)
            .thenReturn(arrangementExternalId);
    }

    /**
     * Assign Arrangement with specified Legal Entities.
     *
     * @param arrangementExternalId    external id of Arrangement.
     * @param legalEntitiesExternalIds list of Legal Entities external identifiers.
     * @return Mono<Void>
     */
    public Mono<Void> addLegalEntitiesForArrangement(String arrangementExternalId, List<String> legalEntitiesExternalIds) {
        log.debug("Attaching Arrangement {} to Legal Entities: {}", arrangementExternalId, legalEntitiesExternalIds);
        return arrangementsApi.postArrangementLegalEntities(arrangementExternalId, new ExternalLegalEntityIds().ids(legalEntitiesExternalIds));
    }

    /**
     * Detach specified Arrangement from Legal Entities.
     *
     * @param arrangementExternalId  arrangement external identifier.
     * @param legalEntityExternalIds List of Legal Entities identified by external identifier.
     * @return Mono<Void>
     */
    public Mono<Void> removeLegalEntityFromArrangement(String arrangementExternalId, List<String> legalEntityExternalIds) {
        log.debug("Removing Arrangement {} from Legal Entities {}", arrangementExternalId, legalEntityExternalIds);
        // TODO: Very ugly, but seems like BOAT doesn't generate body for DELETE requests. Not sure it is incorrect though..
        ApiClient apiClient = arrangementsApi.getApiClient();
        return apiClient.invokeAPI(
            "/arrangements/{externalArrangementId}/legalentities",
            HttpMethod.DELETE,
            Collections.singletonMap("externalArrangementId", arrangementExternalId),
            new LinkedMultiValueMap<>(),
            Collections.singletonMap("ids", legalEntityExternalIds),
            new HttpHeaders(),
            new LinkedMultiValueMap<>(),
            new LinkedMultiValueMap<>(),
            apiClient.selectHeaderAccept(new String[]{"application/json"}),
            apiClient.selectHeaderContentType(new String[]{}),
            new String[]{},
            new ParameterizedTypeReference<Void>() {
            }
        );
    }


    public Flux<ArrangementItem> getArrangements(List<String> arrangementIds) {
        return arrangementsApi.getArrangements(null, null, arrangementIds)
            .flatMapIterable(ArrangementItems::getArrangementElements);
    }
}
