package com.backbase.stream.compositions.legalentity.core.service.impl;

import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapper;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityPullRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityResponse;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityIntegrationService;
import com.backbase.stream.compositions.legalentity.integration.client.LegalEntityIntegrationApi;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@AllArgsConstructor
public class LegalEntityIntegrationServiceImpl implements LegalEntityIntegrationService {
    private final LegalEntityIntegrationApi legalEntityIntegrationApi;

    private final LegalEntityMapper mapper;

    /**
     * {@inheritDoc}
     */
    public Mono<LegalEntityResponse> pullLegalEntity(LegalEntityPullRequest ingestPullRequest) {
        return legalEntityIntegrationApi
                .pullLegalEntity(
                        mapper.mapPullRequestStreamToIntegration(ingestPullRequest))
                .map(mapper::mapResponseIntegrationToStream)
                .flatMap(this::handleIntegrationResponse);
    }

    private Mono<LegalEntityResponse> handleIntegrationResponse(LegalEntityResponse res) {
        if (log.isDebugEnabled()) {
            log.debug("Membership Accounts received from Integration: {}", res.getMembershipAccounts());
            log.debug("Legal Entity received from Integration: {}", res.getLegalEntity());
        }
    }
}
