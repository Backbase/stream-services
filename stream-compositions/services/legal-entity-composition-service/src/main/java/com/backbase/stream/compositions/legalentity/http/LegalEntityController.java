package com.backbase.stream.compositions.legalentity.http;

import com.backbase.stream.compositions.legalentity.api.LegalEntityCompositionApi;
import com.backbase.stream.compositions.legalentity.api.model.LegalEntityIngestionResponse;
import com.backbase.stream.compositions.legalentity.api.model.LegalEntityPullIngestionRequest;
import com.backbase.stream.compositions.legalentity.api.model.LegalEntityPushIngestionRequest;
import com.backbase.stream.compositions.legalentity.core.config.LegalEntityConfigurationProperties;
import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapper;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityPullRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityPushRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityResponse;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityIngestionService;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@AllArgsConstructor
@Slf4j
@EnableConfigurationProperties(LegalEntityConfigurationProperties.class)
public class LegalEntityController implements LegalEntityCompositionApi {

  private final LegalEntityIngestionService legalEntityIngestionService;
  private final LegalEntityMapper mapper;

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<ResponseEntity<LegalEntityIngestionResponse>> pullLegalEntity(
      @Valid Mono<LegalEntityPullIngestionRequest> pullIngestionRequest,
      ServerWebExchange exchange) {
    log.info("Start synchronous ingestion of Legal Entity");
    return pullIngestionRequest.map(this::buildPullRequest)
        .flatMap(legalEntityIngestionService::ingestPull)
        .map(this::mapIngestionToResponse);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<ResponseEntity<LegalEntityIngestionResponse>> pushLegalEntity(
      @Valid Mono<LegalEntityPushIngestionRequest> pushIngestionRequest,
      ServerWebExchange exchange) {
    return pushIngestionRequest.map(this::buildPushRequest)
        .flatMap(legalEntityIngestionService::ingestPush)
        .map(this::mapIngestionToResponse);
  }

  /**
   * Builds ingestion request for downstream service.
   *
   * @param request PullIngestionRequest
   * @return LegalEntityIngestPullRequest
   */
  private LegalEntityPullRequest buildPullRequest(LegalEntityPullIngestionRequest request) {
    return mapper.mapPullRequestCompositionToStream(request);
  }

  /**
   * Builds ingestion request for downstream service.
   *
   * @param request PushIngestionRequest
   * @return LegalEntityIngestPushRequest
   */
  private LegalEntityPushRequest buildPushRequest(LegalEntityPushIngestionRequest request) {
    return LegalEntityPushRequest
        .builder()
        .legalEntity(mapper.mapCompostionToStream(request.getLegalEntity()))
        .build();
  }

  /**
   * Builds ingestion response for API endpoint.
   *
   * @param response LegalEntityIngestResponse
   * @return ResponseEntity<IngestionResponse>
   */
  private ResponseEntity<LegalEntityIngestionResponse> mapIngestionToResponse(
      LegalEntityResponse response) {
    return new ResponseEntity<>(
        new LegalEntityIngestionResponse()
            .withLegalEntity(mapper.mapStreamToComposition(response.getLegalEntity())),
        HttpStatus.CREATED);
  }


}
