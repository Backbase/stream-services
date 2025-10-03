package com.backbase.stream.service;

import com.backbase.accesscontrol.legalentity.api.integration.v3.model.LegalEntityItem;
import com.backbase.accesscontrol.legalentity.api.integration.v3.model.ResultId;
import com.backbase.accesscontrol.legalentity.api.service.v1.LegalEntityApi;
import com.backbase.accesscontrol.legalentity.api.service.v1.model.LegalEntitiesList;
import com.backbase.accesscontrol.legalentity.api.service.v1.model.LegalEntityUpdate;
import com.backbase.stream.exceptions.LegalEntityException;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.mapper.AccessGroupMapper;
import com.backbase.stream.mapper.LegalEntityMapper;
import com.backbase.stream.utils.BatchResponseUtils;
import java.util.List;
import java.util.Objects;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.mapstruct.factory.Mappers;
import org.springframework.web.client.RestClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Legal Entity Service.
 */
@SuppressWarnings("WeakerAccess")
@Slf4j
@RequiredArgsConstructor
public class LegalEntityService {

    @NotNull
    private final LegalEntityApi legalEntityServiceApi;
    @NonNull
    private final com.backbase.accesscontrol.legalentity.api.integration.v3.LegalEntityApi legalEntityIntegrationApi;
    @NonNull
    private final BatchResponseUtils batchResponseUtils;

    private final LegalEntityMapper mapper = Mappers.getMapper(LegalEntityMapper.class);
    private final AccessGroupMapper serviceAgreementMapper = Mappers.getMapper(AccessGroupMapper.class);

    /**
     * Create Legal Entity in Access Control.
     *
     * @param legalEntity The Legal Entity to Create
     * @return The Created Legal Entity including Internal ID
     */
    public Mono<LegalEntity> createLegalEntity(LegalEntity legalEntity) {
        LegalEntityItem legalEntityCreateItem = mapper.toPresentation(legalEntity);
        return createLegalEntity(legalEntityCreateItem)
            .map(createdLegalEntityId -> {
                log.info("Created Legal Entity: {} with ID: {}", legalEntity.getName(), createdLegalEntityId.getId());
                legalEntity.setInternalId(createdLegalEntityId.getId());
                return legalEntity;
            });
    }

    private Mono<ResultId> createLegalEntity(LegalEntityItem legalEntity) {
        // Create Legal Entity without master service agreement
        return legalEntityIntegrationApi.ingestLegalEntity(legalEntity)
            .doOnError(WebClientResponseException.class, this::handleWebClientResponseException)
            .onErrorResume(WebClientResponseException.class, exception ->
                Mono.error(new LegalEntityException(exception)))
            .onErrorStop();
    }

    /**
     * Get Subsidiaries for Legal Entity.
     *
     * @param legalEntityExternalId External Legal Entity ID
     * @param cursor                pointer to the last returned item
     * @param size                  page size
     * @return List of Legal Entities
     */
    public Flux<LegalEntitiesList> getSubEntities(String legalEntityExternalId, String cursor, int size) {
        return getLegalEntityByExternalId(legalEntityExternalId)
            .flatMapMany(legalEntity ->
                legalEntityServiceApi.getLegalEntities(legalEntity.getInternalId(), cursor, size, null, null, null));
    }

    /**
     * Get Master Service Agreement for External Legal Entity ID.
     *
     * @param legalEntityExternalId External Legal Entity ID
     * @return Service Agreement
     */
    public Mono<ServiceAgreement> getMasterServiceAgreementForExternalLegalEntityId(String legalEntityExternalId) {
        return legalEntityIntegrationApi.getSingleServiceAgreement(legalEntityExternalId)
            .doOnNext(serviceAgreementItem -> log.info("Service Agreement: {} found for legal entity: {}",
                serviceAgreementItem.getExternalId(), legalEntityExternalId))
            .onErrorResume(WebClientResponseException.NotFound.class, throwable -> {
                log.info("Master Service Agreement not found for: {}. Request:[{}] {}  Response: {}",
                    legalEntityExternalId, throwable.getRequest().getMethod(), throwable.getRequest().getURI(),
                    throwable.getResponseBodyAsString());
                return Mono.empty();
            })
            .map(mapper::toStream);
    }

    /**
     * Get Master Service Agreement for Internal Legal Entity ID.
     *
     * @param legalEntityInternalId Internal Legal Entity ID
     * @return Service Agreement
     */
    public Mono<ServiceAgreement> getMasterServiceAgreementForInternalLegalEntityId(String legalEntityInternalId) {
        log.info("Getting Service Agreement for: {}", legalEntityInternalId);
        return legalEntityServiceApi.getSingleServiceAgreement(legalEntityInternalId)
            .doOnNext(serviceAgreementItem -> log.info("Service Agreement: {} found for legal entity: {}",
                serviceAgreementItem.getExternalId(), legalEntityInternalId))
            .onErrorResume(WebClientResponseException.NotFound.class, throwable -> {
                log.info("Master Service Agreement not found for: {}. Request:[{}] {}  Response: {}",
                    legalEntityInternalId, throwable.getRequest().getMethod(), throwable.getRequest().getURI(),
                    throwable.getResponseBodyAsString());
                return Mono.empty();
            })
            .map(serviceAgreementMapper::toStream);
    }

    public Mono<LegalEntity> getLegalEntityByExternalId(String externalId) {
        try {
            return legalEntityIntegrationApi.getLegalEntityByExternalId(externalId)
                .onErrorResume(WebClientResponseException.NotFound.class, notFound -> {
                    log.info("Legal Entity with externalId: {} does not exist: {}", externalId,
                        notFound.getResponseBodyAsString());
                    return Mono.empty();
                })
                .map(mapper::toStream);
        } catch (RestClientException e) {
            return Mono.error(e);
        }
    }

    public Mono<LegalEntity> getLegalEntityByInternalId(String internalId) {
        return legalEntityServiceApi.getLegalEntityById(internalId)
            .onErrorResume(WebClientResponseException.NotFound.class, notFound -> {
                log.info("Legal Entity with internalId: {} does not exist: {}", internalId,
                    notFound.getResponseBodyAsString());
                return Mono.empty();
            })
            .doOnError(WebClientResponseException.class, this::handleWebClientResponseException)
            .map(mapper::toStream);
    }

    /**
     * Delte Legal Entitiy from DBS.
     *
     * @param legalEntityExternalId exernal idenfier of legal entity.
     * @return Mono<Void>
     */
    public Mono<Void> deleteLegalEntity(String legalEntityExternalId) {
        return legalEntityIntegrationApi.batchDeleteLegalEntities(List.of(legalEntityExternalId))
            .map(r -> batchResponseUtils.checkBatchResponseItem(r, "Remove Legal Entity", r.getStatus().getValue(),
                r.getResourceId(), r.getErrors()))
            .collectList()
            .onErrorResume(WebClientResponseException.class, e -> {
                log.error("Failed to remove legal  entity: {}", e.getResponseBodyAsString(), e);
                return Mono.error(e);
            }).then();
    }

    private void handleWebClientResponseException(WebClientResponseException webclientResponseException) {
        log.error("Bad Request: \n[{}]: {}\nResponse: {}",
            Objects.requireNonNull(webclientResponseException.getRequest()).getMethod(),
            webclientResponseException.getRequest().getURI(),
            webclientResponseException.getResponseBodyAsString());
    }

    /**
     * Update Legal Entity in Access Control.
     *
     * @param legalEntity The Legal Entity to update
     * @return The Updated Legal Entity
     */
    public Mono<LegalEntity> putLegalEntity(LegalEntity legalEntity) {
        LegalEntityUpdate legalEntityUpdate = mapper.toLegalEntityPut(legalEntity);

        return legalEntityServiceApi.updateLegalEntity(legalEntity.getInternalId(), legalEntityUpdate)
            .doOnError(WebClientResponseException.class, this::handleWebClientResponseException)
            .onErrorResume(WebClientResponseException.class,
                exception -> Mono.error(new RuntimeException("Failed to update Legal Entity", exception)))
            .onErrorStop()
            .then(getLegalEntityByExternalId(legalEntity.getExternalId()));
    }
}
