package com.backbase.stream.compositions.product.core.service.impl;

import com.backbase.accesscontrol.legalentity.api.service.v1.LegalEntityApi;
import com.backbase.accesscontrol.legalentity.api.service.v1.model.LegalEntityWithParent;
import com.backbase.accesscontrol.serviceagreement.api.service.v1.ServiceAgreementApi;
import com.backbase.buildingblocks.presentation.errors.NotFoundException;
import com.backbase.dbs.user.api.service.v2.UserManagementApi;
import com.backbase.dbs.user.api.service.v2.model.GetUser;
import com.backbase.stream.compositions.product.core.service.AccessControlService;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Slf4j
@Component
public class AccessControlServiceImpl implements AccessControlService {

    private final UserManagementApi userManagementApi;
    private final LegalEntityApi legalEntityServiceApi;
    private final ServiceAgreementApi serviceAgreementServiceApi

    @Override
    public Mono<GetUser> getUserByExternalId(String externalId, boolean skipHierarchyCheck) {
        return userManagementApi.getUserByExternalId(externalId, skipHierarchyCheck)
            .doOnNext(userItem -> log.info("Found user: {} for externalId: {}", userItem.getFullName(),
                userItem.getExternalId()))
            .onErrorResume(WebClientResponseException.NotFound.class, notFound ->
                handleUserNotFound(externalId, notFound.getResponseBodyAsString()));
    }

    @Override
    public Mono<ServiceAgreement> getMasterServiceAgreementByInternalLegalEntityId(String legalEntityInternalId) {
        log.info("Getting Master Service Agreement for internal legal entity ID: {}", legalEntityInternalId);
        return legalEntityServiceApi.getSingleServiceAgreement(legalEntityInternalId)
            .doOnNext(
                serviceAgreement -> log.info("Master Service Agreement: {} found for legal entity internal ID: {}",
                    serviceAgreement.getExternalId(), legalEntityInternalId))
            .onErrorResume(WebClientResponseException.NotFound.class, notFound -> {
                log.info("Master Service Agreement not found for legal entity internal ID: {}. {}",
                    legalEntityInternalId,
                    notFound.getResponseBodyAsString());
                return Mono.error(new NotFoundException().withMessage(notFound.getResponseBodyAsString()));
            })
            .map(sa -> new ServiceAgreement().internalId(sa.getId()).externalId(sa.getExternalId()));
    }

    @Override
    public Mono<ServiceAgreement> getServiceAgreementById(String serviceAgreementId) {
        log.info("Getting Service Agreement for ID: {}", serviceAgreementId);
        return serviceAgreementServiceApi.getServiceAgreementById(serviceAgreementId)
            .doOnNext(serviceAgreement -> log.info("Service Agreement: {} found for ID: {}",
                serviceAgreement.getExternalId(), serviceAgreement))
            .onErrorResume(WebClientResponseException.NotFound.class, notFound -> {
                log.info("Service Agreement with ID: {} not found. {}", serviceAgreementId,
                    notFound.getResponseBodyAsString());
                return Mono.error(new NotFoundException().withMessage(notFound.getResponseBodyAsString()));
            })
            .map(sa -> new ServiceAgreement().internalId(sa.getId()).externalId(sa.getExternalId()));
    }

    @Override
    public Mono<LegalEntity> getLegalEntityById(String legalEntityId) {
        log.info("Getting Legal Entity for ID: {}", legalEntityId);
        return legalEntityServiceApi.getLegalEntityById(legalEntityId)
            .doOnNext(legalEntityItem -> log.info("Legal Entity: {} found for legal entity internal ID: {}",
                legalEntityItem.getExternalId(), legalEntityId))
            .onErrorResume(WebClientResponseException.NotFound.class, notFound -> {
                log.info("Legal Entity not found by ID: {}. {}", legalEntityId,
                    notFound.getResponseBodyAsString());
                return handleLegalEntityNotFound(legalEntityId, notFound.getResponseBodyAsString());
            })
            .map(legalEntityItem -> new LegalEntity().internalId(legalEntityItem.getId())
                .externalId(legalEntityItem.getExternalId()));
    }

    private Mono<? extends LegalEntityWithParent> handleLegalEntityNotFound(String legalEntityId,
        String responseBodyAsString) {
        log.info("Legal entity with id: {} does not exist: {}", legalEntityId, responseBodyAsString);
        return Mono.error(new NotFoundException().withMessage(responseBodyAsString));
    }

    private Mono<GetUser> handleUserNotFound(String externalId, String responseBodyAsString) {
        log.info("User with externalId: {} does not exist: {}", externalId, responseBodyAsString);
        return Mono.error(new NotFoundException().withMessage(responseBodyAsString));
    }

}
