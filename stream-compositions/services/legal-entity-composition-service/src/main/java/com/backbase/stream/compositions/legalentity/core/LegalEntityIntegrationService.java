package com.backbase.stream.compositions.legalentity.core;

import com.backbase.stream.compositions.integration.legalentity.api.LegalEntityIntegrationApi;
import com.backbase.stream.compositions.integration.legalentity.model.GetLegalEntityRequest;
import com.backbase.stream.compositions.integration.legalentity.model.LegalEntity;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestPullRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@AllArgsConstructor
public class LegalEntityIntegrationService {
    private final LegalEntityIntegrationApi legalEntityIntegrationApi;

    public Flux<LegalEntity> retrieveLegalEntities(LegalEntityIngestPullRequest ingestPullRequest) {
        GetLegalEntityRequest request = prepareRequest(ingestPullRequest);

        return legalEntityIntegrationApi
                .getLegalEntities(request);

    }

    private GetLegalEntityRequest prepareRequest(LegalEntityIngestPullRequest ingestPullRequest) {
        return null;
    }
}
