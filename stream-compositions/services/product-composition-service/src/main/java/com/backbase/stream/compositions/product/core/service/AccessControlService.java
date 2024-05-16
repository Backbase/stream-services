package com.backbase.stream.compositions.product.core.service;

import com.backbase.dbs.accesscontrol.api.service.v3.model.LegalEntityItem;
import com.backbase.dbs.accesscontrol.api.service.v3.model.ServiceAgreementItemQuery;
import com.backbase.dbs.user.api.service.v2.model.GetUser;
import reactor.core.publisher.Mono;

public interface AccessControlService {

    /**
     * Get user by external ID.
     *
     * @param externalId External ID
     * @return User if found, Empty otherwise
     */
    Mono<GetUser> getUserByExternalId(String externalId, boolean skipHierarchyCheck);

    /**
     * Get Master service agreement by Internal legal entity ID
     *
     * @param legalEntityInternalId Internal legal entity ID
     * @return Master service agreement
     */
    Mono<ServiceAgreementItemQuery> getMasterServiceAgreementByInternalLegalEntityId(String legalEntityInternalId);

    /**
     * Get service agreement by ID
     *
     * @param serviceAgreementId service agreement ID
     * @return Service agreement
     */
    Mono<ServiceAgreementItemQuery> getServiceAgreementById(String serviceAgreementId);

    /**
     * Get Legal Entity by ID
     *
     * @param legalEntityId Legal entity ID
     * @return Legal Entity
     */
    Mono<LegalEntityItem> getLegalEntityById(String legalEntityId);

}
