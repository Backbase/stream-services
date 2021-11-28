package com.backbase.stream.compositions.legalentity;

import com.backbase.compositions.integration.legalentity.api.service.v1.LegalEntityIntegrationApi;
import com.backbase.compositions.integration.legalentity.api.service.v1.model.GetLegalEntityRequest;
import com.backbase.compositions.integration.legalentity.api.service.v1.model.GetLegalEntityResponse;
import com.backbase.stream.compositions.legalentity.mapper.LegalEntityMapper;
import com.backbase.stream.compositions.legalentity.model.LegalEntityIngestPullRequest;
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

        return mapper.reMap(response.getLegalEntity());
    }

    private GetLegalEntityRequest prepareRequest(LegalEntityIngestPullRequest ingestPullRequest) {
        return new GetLegalEntityRequest()
                .legalEntity(mapper.reMap(ingestPullRequest.getLegalEntity()));
    }

    private GetLegalEntityResponse executeRequest(GetLegalEntityRequest request) {
        return legalEntityIntegrationApi.getlegalEntity(request).block();
    }
}
