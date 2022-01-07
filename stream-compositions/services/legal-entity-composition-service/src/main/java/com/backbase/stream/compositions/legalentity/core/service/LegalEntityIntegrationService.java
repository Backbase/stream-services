package com.backbase.stream.compositions.legalentity.core.service;

import com.backbase.stream.compositions.integration.legalentity.model.LegalEntity;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestPullRequest;
import reactor.core.publisher.Mono;

public interface LegalEntityIntegrationService {
    /**
     * Pulls legal entity from external integration service.
     *
     * @param ingestPullRequest LegalEntityIngestPullRequest
     * @return LegalEntity
     */
    Mono<LegalEntity> pullLegalEntity(LegalEntityIngestPullRequest ingestPullRequest);
}
