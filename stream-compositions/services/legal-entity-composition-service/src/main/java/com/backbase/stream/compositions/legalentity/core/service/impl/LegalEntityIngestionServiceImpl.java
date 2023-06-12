package com.backbase.stream.compositions.legalentity.core.service.impl;

import com.backbase.buildingblocks.presentation.errors.BadRequestException;
import com.backbase.buildingblocks.presentation.errors.Error;
import com.backbase.stream.LegalEntitySaga;
import com.backbase.stream.LegalEntityTask;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityPullRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityPushRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityResponse;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityIngestionService;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityIntegrationService;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityPostIngestionService;
import com.backbase.stream.legalentity.model.LegalEntity;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@AllArgsConstructor
public class LegalEntityIngestionServiceImpl implements LegalEntityIngestionService {

  private final LegalEntitySaga legalEntitySaga;
  private final LegalEntityIntegrationService legalEntityIntegrationService;
  private final Validator validator;
  private final LegalEntityPostIngestionService legalEntityPostIngestionService;

  /** {@inheritDoc} */
  public Mono<LegalEntityResponse> ingestPull(LegalEntityPullRequest ingestPullRequest) {
    return pullLegalEntity(ingestPullRequest)
        .flatMap(this::validate)
        .flatMap(this::sendToDbs)
        .flatMap(legalEntityPostIngestionService::handleSuccess)
        .doOnError(legalEntityPostIngestionService::handleFailure);
  }

  /** {@inheritDoc} */
  public Mono<LegalEntityResponse> ingestPush(LegalEntityPushRequest ingestPushRequest) {
    return pushLegalEntity(ingestPushRequest)
        .flatMap(this::validate)
        .flatMap(this::sendToDbs)
        .doOnSuccess(legalEntityPostIngestionService::handleSuccess);
  }

  /**
   * Pulls and remaps legal entity from integration service.
   *
   * @param request LegalEntityIngestPullRequest
   * @return LegalEntity
   */
  private Mono<LegalEntityResponse> pullLegalEntity(LegalEntityPullRequest request) {
    return legalEntityIntegrationService.pullLegalEntity(request);
  }

  /**
   * Sends Legal Entity Task to DBS for persistence.
   *
   * @param res LegalEntityResponse
   * @return LegalEntity
   */
  private Mono<LegalEntityResponse> sendToDbs(LegalEntityResponse res) {
    return legalEntitySaga
        .executeTask(new LegalEntityTask(res.getLegalEntity()))
        .map(LegalEntityTask::getData)
        .map(
            le ->
                LegalEntityResponse.builder()
                    .legalEntity(le)
                    .additions(res.getAdditions())
                    .membershipAccounts(res.getMembershipAccounts())
                    .productChainEnabledFromRequest(res.getProductChainEnabledFromRequest())
                    .build());
  }

  /**
   * Perform any pre-processing on the data received from the downstream system
   *
   * @param res
   * @return A Mono publisher for LegalEntity
   */
  private Mono<LegalEntityResponse> validate(LegalEntityResponse res) {
    Set<ConstraintViolation<LegalEntity>> violations = validator.validate(res.getLegalEntity());

    if (!CollectionUtils.isEmpty(violations)) {
      List<Error> errors =
          violations.stream()
              .map(
                  c -> new Error().withMessage(c.getMessage()).withKey(Error.INVALID_INPUT_MESSAGE))
              .collect(Collectors.toList());
      return Mono.error(new BadRequestException().withErrors(errors));
    }

    return Mono.just(res);
  }

  private Mono<LegalEntityResponse> pushLegalEntity(LegalEntityPushRequest legalEntityPushRequest) {
    return Mono.just(
        LegalEntityResponse.builder().legalEntity(legalEntityPushRequest.getLegalEntity()).build());
  }
}
