package com.backbase.stream.service;

import com.backbase.dbs.accesscontrol.api.service.v2.LegalEntitiesApi;
import com.backbase.dbs.accesscontrol.api.service.v2.LegalEntityApi;
import com.backbase.dbs.accesscontrol.api.service.v2.model.LegalEntitiesBatchDelete;
import com.backbase.dbs.accesscontrol.api.service.v2.model.LegalEntityCreateItem;
import com.backbase.dbs.accesscontrol.api.service.v2.model.LegalEntityItemId;
import com.backbase.dbs.accesscontrol.api.service.v2.model.LegalEntityPut;
import com.backbase.stream.exceptions.LegalEntityException;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.mapper.AccessGroupMapper;
import com.backbase.stream.mapper.LegalEntityMapper;
import com.backbase.stream.product.utils.BatchResponseUtils;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
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

    @NonNull
    private final LegalEntitiesApi legalEntitiesApi;
    @NonNull
    private final LegalEntityApi legalEntityApi;

    private final LegalEntityMapper mapper = Mappers.getMapper(LegalEntityMapper.class);
    private final AccessGroupMapper serviceAgreementMapper = Mappers.getMapper(AccessGroupMapper.class);

    // Configured at access-control service
    @Value("${backbase.accesscontrol.token.key:Bar12345Bar12345}")
    private final String accessControlApiTokenKey = "Bar12345Bar12345";

    @Value("${backbase.accesscontrol.token.initPhrase:RandomInitVecto2}")
    private final String accessControlApiTokenInitPhrase = "RandomInitVecto2";

    /**
     * Create Legal Entity in Access Control.
     *
     * @param legalEntity The Legal Entity to Create
     * @return The Created Legal Entity including Internal ID
     */
    public Mono<LegalEntity> createLegalEntity(LegalEntity legalEntity) {
        LegalEntityCreateItem legalEntityCreateItem = mapper.toPresentation(legalEntity);
        return createLegalEntity(legalEntityCreateItem)
            .map(createdLegalEntityId -> {
                log.info("Created Legal Entity: {} with ID: {}", legalEntity.getName(), createdLegalEntityId.getId());
                legalEntity.setInternalId(createdLegalEntityId.getId());
                return legalEntity;
            });
    }

    private Mono<LegalEntityItemId> createLegalEntity(LegalEntityCreateItem legalEntity) {
        // Create Legal Entity with out master service agreement
        return legalEntitiesApi.postCreateLegalEntities(legalEntity)
            .doOnError(WebClientResponseException.class, this::handleWebClientResponseException)
            .onErrorResume(WebClientResponseException.class, exception ->
                Mono.error(new LegalEntityException(legalEntity, "Failed to create Legal Entity",  exception)))
            .onErrorStop();
    }

    /**
     * Get Subsidiaries for Legal Entity.
     *
     * @param legalEntityExternalId External Legal Entity ID
     * @param pageable              Page information
     * @return List of Legal Entities
     */
    public Flux<LegalEntity> getSubEntities(String legalEntityExternalId, Pageable pageable) {
        return getLegalEntityByExternalId(legalEntityExternalId)
            .flux()
            .flatMap(legalEntity ->
                legalEntitiesApi.getSubEntities(legalEntity.getInternalId(), null,
                    Math.toIntExact(pageable.getOffset()), pageable.getPageSize(), null)
                    .map(mapper::toStream));
    }

    /**
     * Get Master Service Agreement for External Legal Entity ID.
     *
     * @param legalEntityExternalId External Legal Entity ID
     * @return Service Agreement
     */
    public Mono<ServiceAgreement> getMasterServiceAgreementForExternalLegalEntityId(String legalEntityExternalId) {
        return legalEntitiesApi.getMasterServiceAgreementByExternalLegalEntity(legalEntityExternalId)
                .doOnNext(serviceAgreementItem -> log.info("Service Agreement: {} found for legal entity: {}", serviceAgreementItem.getExternalId(), legalEntityExternalId))
                .onErrorResume(WebClientResponseException.NotFound.class, throwable -> {
                    log.info("Master Service Agreement not found for: {}. Request:[{}] {}  Response: {}", legalEntityExternalId,  throwable.getRequest().getMethod(), throwable.getRequest().getURI() , throwable.getResponseBodyAsString());
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
        return legalEntityApi.getMasterServiceAgreement(legalEntityInternalId)
            .doOnNext(serviceAgreementItem -> log.info("Service Agreement: {} found for legal entity: {}", serviceAgreementItem.getExternalId(), legalEntityInternalId))
            .onErrorResume(WebClientResponseException.NotFound.class, throwable -> {
                log.info("Master Service Agreement not found for: {}. Request:[{}] {}  Response: {}", legalEntityInternalId,  throwable.getRequest().getMethod(), throwable.getRequest().getURI() , throwable.getResponseBodyAsString());
                return Mono.empty();
            })
            .map(serviceAgreementMapper::toStream);
    }

    public Mono<LegalEntity> getLegalEntityByExternalId(String externalId) {
        try {
            return legalEntitiesApi.getLegalEntityByExternalId(externalId)
                    .onErrorResume(WebClientResponseException.NotFound.class, notFound -> {
                        log.info("Legal Entity with externalId: {} does not exist: {}", externalId, notFound.getResponseBodyAsString());
                        return Mono.empty();
                    })
                    .map(mapper::toStream);
        } catch (RestClientException e) {
            return Mono.error(e);
        }
    }

    public Mono<LegalEntity> getLegalEntityByInternalId(String internalId) {
        return legalEntitiesApi.getLegalEntityById(internalId)
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
        return legalEntitiesApi.postLegalEntitiesBatchDelete(
                new LegalEntitiesBatchDelete()
                        .accessToken(getAccessControlAccessToken())
                        .externalIds(Collections.singletonList(legalEntityExternalId)))
                .map(r -> BatchResponseUtils.checkBatchResponseItem(r, "Remove Legal Entity", r.getStatus().getValue(), r.getResourceId(), r.getErrors()))
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
     * Generate access token which is usually verified by Access Control APIs during delete action.
     *
     * This implementation is brought here to avoid calling the
     *
     * @return access token.
     */
    private String getAccessControlAccessToken() {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(accessControlApiTokenKey.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec iv = new IvParameterSpec(accessControlApiTokenInitPhrase.getBytes(StandardCharsets.UTF_8));
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(1, secretKeySpec, iv);

            byte[] encrypted = cipher.doFinal(Long.toString(Date.from(
                    LocalDateTime.now()
                            .atZone(ZoneId.systemDefault()).toInstant()).getTime()).getBytes());

            ByteBuffer bb = ByteBuffer.wrap(encrypted);
            long firstLong = bb.getLong();
            long secondLong = bb.getLong();
            return new UUID(firstLong, secondLong).toString();
        } catch (Exception e) {
            log.error("Error obtaining access token: ", e);
            return null;
        }
    }

    /**
     * Update Legal Entity in Access Control.
     *
     * @param legalEntity The Legal Entity to update
     * @return The Updated Legal Entity
     */
    public Mono<LegalEntity> putLegalEntity(LegalEntity legalEntity) {
        LegalEntityPut legalEntityPut = mapper.toLegalEntityPut(legalEntity);

        return legalEntitiesApi.putLegalEntities(Collections.singletonList(legalEntityPut))
                .doOnError(WebClientResponseException.class, this::handleWebClientResponseException)
                .onErrorResume(WebClientResponseException.class, exception -> Mono.error(new RuntimeException("Failed to update Legal Entity",  exception)))
                .onErrorStop()
                .then(getLegalEntityByExternalId(legalEntityPut.getExternalId()));
    }

}
