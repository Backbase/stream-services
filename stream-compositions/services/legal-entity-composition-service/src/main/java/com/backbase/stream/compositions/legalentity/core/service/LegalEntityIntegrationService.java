package com.backbase.stream.compositions.legalentity.core.service;

import com.backbase.stream.compositions.integration.legalentity.model.LegalEntity;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestPullRequest;
import reactor.core.publisher.Flux;

public interface LegalEntityIntegrationService {
    Flux<LegalEntity> retrieveLegalEntities(LegalEntityIngestPullRequest ingestPullRequest);
}
