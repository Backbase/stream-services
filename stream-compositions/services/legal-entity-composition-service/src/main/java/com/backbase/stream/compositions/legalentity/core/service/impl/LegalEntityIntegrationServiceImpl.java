package com.backbase.stream.compositions.legalentity.core.service.impl;

import com.backbase.buildingblocks.presentation.errors.InternalServerErrorException;
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
        .map(leRes -> {
          leRes.setProductChainEnabledFromRequest(ingestPullRequest.getProductChainEnabled());
          leRes.setAdditions(ingestPullRequest.getAdditions());
          return leRes;
        })
        .onErrorResume(this::handleIntegrationError)
        .flatMap(this::handleIntegrationResponse);

  }

  private Mono<LegalEntityResponse> handleIntegrationResponse(LegalEntityResponse res) {
    log.debug("Membership Accounts received from Integration: {}", res.getMembershipAccounts());
    log.debug("Legal Entity received from Integration: {}", res.getLegalEntity());
    return Mono.just(res);
  }

  private Mono<LegalEntityResponse> handleIntegrationError(Throwable e) {
    log.error("Error while pulling Legal Entities: {}", e.getMessage());
    return Mono.error(new InternalServerErrorException().withMessage(e.getMessage()));
  }
}