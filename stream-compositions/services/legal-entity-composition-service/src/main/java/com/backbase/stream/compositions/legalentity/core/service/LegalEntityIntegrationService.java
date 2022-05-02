package com.backbase.stream.compositions.legalentity.core.service;

import com.backbase.stream.compositions.legalentity.core.model.LegalEntityPullRequest;
import com.backbase.stream.compositions.legalentity.integration.client.model.LegalEntity;
import reactor.core.publisher.Mono;

public interface LegalEntityIntegrationService {
    /**
     * Pulls legal entity from external integration service.
     *
     * @param ingestPullRequest LegalEntityIngestPullRequest
     * @return LegalEntity
     */
    Mono<LegalEntity> pullLegalEntity(LegalEntityPullRequest ingestPullRequest);
}
