package com.backbase.stream.compositions.transaction.core.service.impl;

import com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostRequestBody;
import com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostResponseBody;
import com.backbase.stream.TransactionService;
import com.backbase.stream.compositions.transaction.core.config.TransactionConfigurationProperties;
import com.backbase.stream.compositions.transaction.core.mapper.TransactionMapper;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestPullRequest;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestPushRequest;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestResponse;
import com.backbase.stream.compositions.transaction.core.service.TransactionIngestionService;
import com.backbase.stream.compositions.transaction.core.service.TransactionIntegrationService;
import com.backbase.stream.compositions.transaction.core.service.TransactionPostIngestionService;
import com.backbase.stream.compositions.transaction.cursor.client.TransactionCursorApi;
import com.backbase.stream.compositions.transaction.cursor.client.model.TransactionCursor;
import com.backbase.stream.compositions.transaction.cursor.client.model.TransactionCursorPatchRequest;
import com.backbase.stream.compositions.transaction.cursor.client.model.TransactionCursorResponse;
import com.backbase.stream.compositions.transaction.cursor.client.model.TransactionCursorUpsertRequest;
import com.backbase.stream.compositions.transaction.cursor.client.model.TransactionCursorUpsertResponse;
import com.backbase.stream.transaction.TransactionTask;
import com.backbase.stream.worker.model.UnitOfWork;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@AllArgsConstructor
public class TransactionIngestionServiceImpl implements TransactionIngestionService {

  public static final String DELIMITER = ",";
  public static final String dateFormat = "yyyy-MM-dd hh:mm:ss";
  private final TransactionMapper mapper;
  private final TransactionService transactionService;
  private final TransactionIntegrationService transactionIntegrationService;

  private final TransactionPostIngestionService transactionPostIngestionService;

  private final TransactionCursorApi transactionCursorApi;

  private final TransactionConfigurationProperties config;

  private final DateTimeFormatter offsetDateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

  /**
   * Ingests transactions in pull mode.
   *
   * @param ingestPullRequest Ingest pull request
   * @return TransactionIngestResponse
   */
  public Mono<TransactionIngestResponse> ingestPull(
      TransactionIngestPullRequest ingestPullRequest) {
    return buildIntegrationRequest(ingestPullRequest)
        .map(this::pullTransactions)
        .map(f -> filterExisting(f, ingestPullRequest.getLastIngestedExternalIds()))
        .flatMap(this::sendToDbs)
        .doOnSuccess(list -> handleSuccess(ingestPullRequest.getArrangementId(), true, list))
        .onErrorResume(e -> handleError(ingestPullRequest.getArrangementId(), true, e))
        .map(list -> buildResponse(list, ingestPullRequest));
  }

  /*
     Filter by existing/already ingested external ids only works
     - if the list is not empty
     - if the filter is enabled in configuration
  */
  private Flux<TransactionsPostRequestBody> filterExisting(
      Flux<TransactionsPostRequestBody> transactionsPostRequestBodyFlux,
      List<String> lastIngestedExternalIds) {
    if (!config.isTransactionIdsFilterEnabled()
        || CollectionUtils.isEmpty(lastIngestedExternalIds)) {
      return transactionsPostRequestBodyFlux;
    }

    return transactionsPostRequestBodyFlux.filter(
        t -> !lastIngestedExternalIds.contains(t.getExternalId()));
  }

  private Mono<TransactionIngestPullRequest> buildIntegrationRequest(
      TransactionIngestPullRequest transactionIngestPullRequest) {
    OffsetDateTime currentTime = OffsetDateTime.now();
    OffsetDateTime dateRangeStartFromRequest = transactionIngestPullRequest.getDateRangeStart();
    OffsetDateTime dateRangeEndFromRequest = transactionIngestPullRequest.getDateRangeEnd();

    transactionIngestPullRequest.setDateRangeStart(
        dateRangeStartFromRequest == null
            ? currentTime.minusDays(config.getDefaultStartOffsetInDays())
            : dateRangeStartFromRequest);
    transactionIngestPullRequest.setDateRangeEnd(
        dateRangeEndFromRequest == null ? currentTime : dateRangeEndFromRequest);

    if (dateRangeStartFromRequest == null && config.isCursorEnabled()) {
      log.info("Transaction Cursor is enabled and Request has no Start Date");
      return getCursor(transactionIngestPullRequest)
          .switchIfEmpty(createCursor(transactionIngestPullRequest));
    }

    return Mono.just(transactionIngestPullRequest);
  }

  /**
   * Ingests product group in push mode.
   *
   * @param ingestPushRequest Ingest push request
   * @return ProductIngestResponse
   */
  public Mono<TransactionIngestResponse> ingestPush(
      TransactionIngestPushRequest ingestPushRequest) {
    return Mono.just(Flux.fromIterable(ingestPushRequest.getTransactions()))
        .flatMap(this::sendToDbs)
        .doOnSuccess(list -> handleSuccess(ingestPushRequest.getArrangementId(), false, list))
        .onErrorResume(e -> handleError(ingestPushRequest.getArrangementId(), false, e))
        .map(list -> buildResponse(list, ingestPushRequest));
  }

  /**
   * Pulls and remap product group from integration service.
   *
   * @param request TransactionIngestPullRequest
   * @return Flux<TransactionsPostRequestBody>
   */
  private Flux<TransactionsPostRequestBody> pullTransactions(TransactionIngestPullRequest request) {
    return transactionIntegrationService
        .pullTransactions(request)
        .map(mapper::mapIntegrationToStream);
  }

  private Mono<TransactionIngestPullRequest> getCursor(TransactionIngestPullRequest request) {
    return transactionCursorApi
        .getByArrangementId(request.getArrangementId())
        .map(TransactionCursorResponse::getCursor)
        .flatMap(cursor -> mapCursorToIntegrationRequest(request, cursor))
        .doOnNext(
            r -> {
              log.info("Patch Transaction Cursor to IN_PROGRESS for :: {}", r.getArrangementId());
              patchCursor(
                  r.getArrangementId(),
                  buildPatchCursorRequest(TransactionCursor.StatusEnum.IN_PROGRESS, null, null));
            });
  }

  private void patchCursor(String arrangementId, TransactionCursorPatchRequest request) {
    transactionCursorApi.patchByArrangementId(arrangementId, request).subscribe();
  }

  private TransactionCursorPatchRequest buildPatchCursorRequest(
      TransactionCursor.StatusEnum statusEnum, String lastTxnDate, String lastTxnIds) {
    TransactionCursorPatchRequest cursorPatchRequest =
        new TransactionCursorPatchRequest().withStatus(statusEnum.toString());

    if (TransactionCursor.StatusEnum.SUCCESS.equals(statusEnum)) {
      cursorPatchRequest.setLastTxnDate(lastTxnDate);
      cursorPatchRequest.setLastTxnIds(lastTxnIds);
    }

    return cursorPatchRequest;
  }

  private Mono<TransactionIngestPullRequest> createCursor(TransactionIngestPullRequest request) {
    return transactionCursorApi
        .upsertCursor(buildUpsertCursorRequest(request))
        .map(TransactionCursorUpsertResponse::getId)
        .flatMap(this::getCursorById)
        .map(TransactionCursorResponse::getCursor)
        .flatMap(cursor -> mapCursorToIntegrationRequest(request, cursor));
  }

  private Mono<TransactionCursorResponse> getCursorById(String id) {
    return transactionCursorApi.getById(id);
  }

  private Mono<TransactionIngestPullRequest> mapCursorToIntegrationRequest(
      TransactionIngestPullRequest request, TransactionCursor cursor) {

    if (cursor.getLastTxnDate() != null) {
      OffsetDateTime dateRangeStart =
          OffsetDateTime.parse(cursor.getLastTxnDate(), offsetDateTimeFormatter);
      request.setDateRangeStart(dateRangeStart);
    }

    if (!CollectionUtils.isEmpty(cursor.getLastTxnIds())) {
      request.setLastIngestedExternalIds(cursor.getLastTxnIds());
    }

    return Mono.just(request);
  }

  private TransactionCursorUpsertRequest buildUpsertCursorRequest(
      TransactionIngestPullRequest request) {
    return new TransactionCursorUpsertRequest()
        .withCursor(
            new TransactionCursor()
                .withArrangementId(request.getArrangementId())
                .withLegalEntityId(request.getLegalEntityInternalId())
                .withExtArrangementId(request.getExternalArrangementId())
                .withStatus(TransactionCursor.StatusEnum.IN_PROGRESS));
  }

  /**
   * Ingests transactions to DBS.
   *
   * @param transactions Transactions
   * @return Ingested transactions
   */
  private Mono<List<TransactionsPostResponseBody>> sendToDbs(
      Flux<TransactionsPostRequestBody> transactions) {
    return transactions
        .publish(transactionService::processTransactions)
        .flatMapIterable(UnitOfWork::getStreamTasks)
        .flatMapIterable(TransactionTask::getResponse)
        .collectList();
  }

  private TransactionIngestResponse buildResponse(
      List<TransactionsPostResponseBody> transactions, TransactionIngestPushRequest ingestRequest) {
    return TransactionIngestResponse.builder()
        .transactions(transactions)
        .arrangementId(ingestRequest.getArrangementId())
        .build();
  }

  private TransactionIngestResponse buildResponse(
      List<TransactionsPostResponseBody> transactions, TransactionIngestPullRequest ingestRequest) {
    return TransactionIngestResponse.builder()
        .transactions(transactions)
        .arrangementId(ingestRequest.getArrangementId())
        .build();
  }

  private void handleSuccess(
      String arrangementId, boolean pullMode, List<TransactionsPostResponseBody> transactions) {
    if (config.isCursorEnabled() && pullMode) {
      String lastTxnIds = null;
      if (config.isTransactionIdsFilterEnabled()) {
        log.info("Transaction Id based filter is enabled");
        lastTxnIds =
            transactions.stream()
                .map(TransactionsPostResponseBody::getExternalId)
                .collect(Collectors.joining(DELIMITER));
      }
      patchCursor(
          arrangementId,
          buildPatchCursorRequest(
              TransactionCursor.StatusEnum.SUCCESS,
              OffsetDateTime.now().format(DateTimeFormatter.ofPattern(dateFormat)),
              lastTxnIds));
    }

    transactionPostIngestionService.handleSuccess(transactions);

    log.debug("Ingested transactions: {}", transactions);
  }

  private Mono<List<TransactionsPostResponseBody>> handleError(
      String arrangementId, boolean pullMode, Throwable e) {
    if (config.isCursorEnabled() && pullMode) {
      patchCursor(
          arrangementId, buildPatchCursorRequest(TransactionCursor.StatusEnum.FAILED, null, null));
    }
    return transactionPostIngestionService.handleFailure(e);
  }
}
