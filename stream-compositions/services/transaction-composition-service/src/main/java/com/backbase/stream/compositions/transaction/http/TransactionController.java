package com.backbase.stream.compositions.transaction.http;

import com.backbase.stream.compositions.transaction.api.TransactionCompositionApi;
import com.backbase.stream.compositions.transaction.api.model.TransactionIngestionResponse;
import com.backbase.stream.compositions.transaction.api.model.TransactionPullIngestionRequest;
import com.backbase.stream.compositions.transaction.api.model.TransactionPushIngestionRequest;
import com.backbase.stream.compositions.transaction.core.mapper.TransactionMapper;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestPullRequest;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestPushRequest;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestResponse;
import com.backbase.stream.compositions.transaction.core.service.TransactionIngestionService;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@AllArgsConstructor
@Slf4j
public class TransactionController implements TransactionCompositionApi {

  private final TransactionIngestionService transactionIngestionService;
  private final TransactionMapper mapper;

  @Override
  public Mono<ResponseEntity<TransactionIngestionResponse>> pullTransactions(
      Mono<TransactionPullIngestionRequest> pullIngestionRequest, ServerWebExchange exchange) {
    return pullIngestionRequest
        .map(this::buildPullRequest)
        .flatMap(transactionIngestionService::ingestPull)
        .map(this::mapIngestionToResponse);
  }

  /** {@inheritDoc} */
  @Override
  public Mono<ResponseEntity<TransactionIngestionResponse>> pushIngestTransactions(
      @Valid Mono<TransactionPushIngestionRequest> pushIngestionRequest,
      ServerWebExchange exchange) {
    return pushIngestionRequest
        .map(this::buildPushRequest)
        .flatMap(transactionIngestionService::ingestPush)
        .map(this::mapIngestionToResponse);
  }

  /**
   * Builds ingestion request for downstream service.
   *
   * @param request PullIngestionRequest
   * @return ProductIngestPullRequest
   */
  private TransactionIngestPullRequest buildPullRequest(TransactionPullIngestionRequest request) {
    return TransactionIngestPullRequest.builder()
        .arrangementId(request.getArrangementId())
        .legalEntityInternalId(request.getLegalEntityInternalId())
        .externalArrangementId(request.getExternalArrangementId())
        .dateRangeStart(request.getDateRangeStart())
        .dateRangeEnd(request.getDateRangeEnd())
        .additions(request.getAdditions())
        .build();
  }

  /**
   * Builds ingestion request for downstream service.
   *
   * @param request PushIngestionRequest
   * @return ProductIngestPushRequest
   */
  private TransactionIngestPushRequest buildPushRequest(TransactionPushIngestionRequest request) {
    return TransactionIngestPushRequest.builder()
        .transactions(
            request.getTransactions().stream()
                .map(mapper::mapCompositionToStream)
                .collect(Collectors.toList()))
        .build();
  }

  /**
   * Builds ingestion response for API endpoint.
   *
   * @param response ProductCatalogIngestResponse
   * @return IngestionResponse
   */
  private ResponseEntity<TransactionIngestionResponse> mapIngestionToResponse(
      TransactionIngestResponse response) {
    return new ResponseEntity<>(
        new TransactionIngestionResponse()
            .withTransactions(
                response.getTransactions().stream()
                    .map(mapper::mapStreamToComposition)
                    .collect(Collectors.toList())),
        HttpStatus.CREATED);
  }
}
