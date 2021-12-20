package com.backbase.stream.compositions.legalentity.core.service;

import com.backbase.stream.compositions.integration.legalentity.model.GetLegalEntityListResponse;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestPullRequest;
import reactor.core.publisher.Mono;

public interface LegalEntityIntegrationService {
    Mono<GetLegalEntityListResponse> retrieveLegalEntities(LegalEntityIngestPullRequest ingestPullRequest);
}
