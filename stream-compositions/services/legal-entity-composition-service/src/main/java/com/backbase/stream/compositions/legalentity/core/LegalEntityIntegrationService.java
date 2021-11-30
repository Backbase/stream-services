package com.backbase.stream.compositions.legalentity.core;

import com.backbase.stream.compositions.integration.legalentity.api.LegalEntityIntegrationApi;
import com.backbase.stream.compositions.integration.legalentity.model.GetLegalEntityRequest;
import com.backbase.stream.compositions.integration.legalentity.model.GetLegalEntityResponse;
import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapper;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestPullRequest;
import com.backbase.stream.legalentity.model.LegalEntity;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class LegalEntityIntegrationService {
    private final LegalEntityMapper mapper;
    private final LegalEntityIntegrationApi legalEntityIntegrationApi;

    public LegalEntity retrieveLegalEntity(LegalEntityIngestPullRequest ingestPullRequest) {
        GetLegalEntityRequest request = prepareRequest(ingestPullRequest);
        GetLegalEntityResponse response = executeRequest(request);

        return mapper.mapToStreamLegalEntity(response.getLegalEntity());
    }

    private GetLegalEntityRequest prepareRequest(LegalEntityIngestPullRequest ingestPullRequest) {
        return null;
    }

    private GetLegalEntityResponse executeRequest(GetLegalEntityRequest request) {
        return legalEntityIntegrationApi.getlegalEntity(request).block();
    }
}
