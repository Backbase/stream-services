package com.backbase.stream.compositions.legalentity.core.service.impl;

import com.backbase.stream.compositions.integration.legalentity.api.LegalEntityIntegrationApi;
import com.backbase.stream.compositions.integration.legalentity.model.GetLegalEntityListResponse;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestPullRequest;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityIntegrationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@AllArgsConstructor
public class LegalEntityIntegrationServiceImpl implements LegalEntityIntegrationService {
    private final LegalEntityIntegrationApi legalEntityIntegrationApi;

    public Mono<GetLegalEntityListResponse> retrieveLegalEntities(LegalEntityIngestPullRequest ingestPullRequest) {
        return legalEntityIntegrationApi.getLegalEntities(ingestPullRequest.getLegalEntityExternalId());
    }
}
