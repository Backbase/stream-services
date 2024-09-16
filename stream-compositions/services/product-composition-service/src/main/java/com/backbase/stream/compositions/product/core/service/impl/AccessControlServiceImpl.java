package com.backbase.stream.compositions.product.core.service.impl;

import com.backbase.buildingblocks.presentation.errors.NotFoundException;
import com.backbase.dbs.accesscontrol.api.service.v3.LegalEntitiesApi;
import com.backbase.dbs.accesscontrol.api.service.v3.ServiceAgreementsApi;
import com.backbase.dbs.accesscontrol.api.service.v3.model.LegalEntityItem;
import com.backbase.dbs.accesscontrol.api.service.v3.model.ServiceAgreementItemQuery;
import com.backbase.dbs.user.api.service.v2.UserManagementApi;
import com.backbase.dbs.user.api.service.v2.model.GetUser;
import com.backbase.stream.compositions.product.core.service.AccessControlService;
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
    private final LegalEntitiesApi legalEntitiesApi;
    private final ServiceAgreementsApi serviceAgreementsApi;

    @Override
    public Mono<GetUser> getUserByExternalId(String externalId, boolean skipHierarchyCheck) {
        return userManagementApi.getUserByExternalId(externalId, skipHierarchyCheck)
                .doOnNext(userItem -> log.info("Found user: {} for externalId: {}", userItem.getFullName(), userItem.getExternalId()))
                .onErrorResume(WebClientResponseException.NotFound.class, notFound ->
                        handleUserNotFound(externalId, notFound.getResponseBodyAsString()));
    }

    @Override
    public Mono<ServiceAgreementItemQuery> getMasterServiceAgreementByInternalLegalEntityId(String legalEntityInternalId) {
        log.info("Getting Master Service Agreement for internal legal entity ID: {}", legalEntityInternalId);
        return legalEntitiesApi.getMasterServiceAgreement(legalEntityInternalId)
                .doOnNext(serviceAgreement -> log.info("Master Service Agreement: {} found for legal entity internal ID: {}",
                        serviceAgreement.getExternalId(), legalEntityInternalId))
                .onErrorResume(WebClientResponseException.NotFound.class, notFound -> {
                    log.info("Master Service Agreement not found for legal entity internal ID: {}. {}", legalEntityInternalId,
                            notFound.getResponseBodyAsString());
                    return handleServiceAgreementNotFound(notFound.getResponseBodyAsString());
                });
    }

    @Override
    public Mono<ServiceAgreementItemQuery> getServiceAgreementById(String serviceAgreementId) {
        log.info("Getting Service Agreement for ID: {}", serviceAgreementId);
        return serviceAgreementsApi.getServiceAgreement(serviceAgreementId)
                .doOnNext(serviceAgreement -> log.info("Service Agreement: {} found for ID: {}",
                        serviceAgreement.getExternalId(), serviceAgreement))
                .onErrorResume(WebClientResponseException.NotFound.class, notFound -> {
                    log.info("Service Agreement with ID: {} not found. {}", serviceAgreementId,
                            notFound.getResponseBodyAsString());
                    return handleServiceAgreementNotFound(notFound.getResponseBodyAsString());
                });
    }

    @Override
    public Mono<LegalEntityItem> getLegalEntityById(String legalEntityId) {
        log.info("Getting Legal Entity for ID: {}", legalEntityId);
        return legalEntitiesApi.getLegalEntityById(legalEntityId)
                .doOnNext(legalEntityItem -> log.info("Legal Entity: {} found for legal entity internal ID: {}",
                        legalEntityItem.getExternalId(), legalEntityId))
                .onErrorResume(WebClientResponseException.NotFound.class, notFound -> {
                    log.info("Legal Entity not found by ID: {}. {}", legalEntityId,
                            notFound.getResponseBodyAsString());
                    return handleLegalEntityNotFound(legalEntityId, notFound.getResponseBodyAsString());
                });
    }

    private Mono<? extends LegalEntityItem> handleLegalEntityNotFound(String legalEntityId,
                                                                      String responseBodyAsString) {
        log.info("Legal entity with id: {} does not exist: {}", legalEntityId, responseBodyAsString);
        return Mono.error(new NotFoundException().withMessage(responseBodyAsString));
    }

    private Mono<GetUser> handleUserNotFound(String externalId, String responseBodyAsString) {
        log.info("User with externalId: {} does not exist: {}", externalId, responseBodyAsString);
        return Mono.error(new NotFoundException().withMessage(responseBodyAsString));
    }

    private Mono<? extends ServiceAgreementItemQuery> handleServiceAgreementNotFound(String responseBodyAsString) {
        return Mono.error(new NotFoundException().withMessage(responseBodyAsString));
    }

}
