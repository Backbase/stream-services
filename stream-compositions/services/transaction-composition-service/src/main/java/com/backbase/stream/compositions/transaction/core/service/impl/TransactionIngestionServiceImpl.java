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
import com.backbase.stream.compositions.transaction.cursor.client.model.*;
import com.backbase.stream.transaction.TransactionTask;
import com.backbase.stream.worker.model.UnitOfWork;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class TransactionIngestionServiceImpl implements TransactionIngestionService {
    private final TransactionMapper mapper;
    private final TransactionService transactionService;
    private final TransactionIntegrationService transactionIntegrationService;

    private final TransactionPostIngestionService transactionPostIngestionService;

    private final TransactionCursorApi transactionCursorApi;

    private final TransactionConfigurationProperties config;

    private static final String dateFormat = "yyyy-MM-dd hh:mm:ss";

    /**
     * Ingests transactions in pull mode.
     *
     * @param ingestPullRequest Ingest pull request
     * @return TransactionIngestResponse
     */
    public Mono<TransactionIngestResponse> ingestPull(TransactionIngestPullRequest ingestPullRequest) {
        return buildIntegrationRequest(ingestPullRequest)
                .map(this::pullTransactions)
                .flatMap(this::sendToDbs)
                .doOnSuccess(list -> handleSuccess(
                        ingestPullRequest.getArrangementId(), list))
                .onErrorResume(e -> handleError(
                        ingestPullRequest.getArrangementId(), e))
                .map(this::buildResponse);
    }

    private Mono<TransactionIngestPullRequest> buildIntegrationRequest(TransactionIngestPullRequest transactionIngestPullRequest) {
        OffsetDateTime currentTime = OffsetDateTime.now();
        transactionIngestPullRequest.setDateRangeEnd(currentTime);
        transactionIngestPullRequest.setDateRangeStart(
                currentTime.minusDays(config.getDefaultStartOffsetInDays()));

        return getCursor(transactionIngestPullRequest)
                .switchIfEmpty(createCursor(transactionIngestPullRequest));
    }

    /**
     * Ingests product group in push mode.
     *
     * @param ingestPushRequest Ingest push request
     * @return ProductIngestResponse
     */
    public Mono<TransactionIngestResponse> ingestPush(TransactionIngestPushRequest ingestPushRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Pulls and remap product group from integration service.
     *
     * @param request TransactionIngestPullRequest
     * @return Flux<TransactionsPostRequestBody>
     */
    private Flux<TransactionsPostRequestBody> pullTransactions(TransactionIngestPullRequest request) {
       return transactionIntegrationService.pullTransactions(request)
                .map(mapper::mapIntegrationToStream);
    }

    private Mono<TransactionIngestPullRequest> getCursor(TransactionIngestPullRequest request) {
        return transactionCursorApi.getByArrangementId(request.getArrangementId())
                .map(TransactionCursorResponse::getCursor)
                .flatMap(cursor -> mapCursorToIntegrationRequest(request, cursor))
                .doOnNext(r -> {
                    log.info("DoOnNext:: {}", r.getArrangementId());
                    patchCursor(r.getArrangementId(),
                            buildPatchCursorRequest(
                                    TransactionCursor.StatusEnum.IN_PROGRESS, null, null));
                });
    }

    private void patchCursor(String arrangementId, TransactionCursorPatchRequest request) {
        transactionCursorApi.patchByArrangementId(arrangementId, request).subscribe();
    }

    private TransactionCursorPatchRequest buildPatchCursorRequest(TransactionCursor.StatusEnum statusEnum,
                                                                  String lastTxnDate, String lastTxnIds) {
        TransactionCursorPatchRequest cursorPatchRequest = new TransactionCursorPatchRequest()
                .withStatus(statusEnum.toString());

        if (TransactionCursor.StatusEnum.SUCCESS.equals(statusEnum)) {
            cursorPatchRequest.setLastTxnDate(lastTxnDate);
            cursorPatchRequest.setLastTxnIds(lastTxnIds);
        }

        return cursorPatchRequest;
    }

    private Mono<TransactionIngestPullRequest> createCursor(TransactionIngestPullRequest request) {
        return transactionCursorApi.upsertCursor(buildUpsertCursorRequest(request))
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
            OffsetDateTime dateRangeStart = OffsetDateTime.parse(cursor.getLastTxnDate());
            request.setDateRangeStart(dateRangeStart);
        }
        return Mono.just(request);
    }


    private TransactionCursorUpsertRequest buildUpsertCursorRequest(TransactionIngestPullRequest request) {
        return new TransactionCursorUpsertRequest()
                .withCursor(new TransactionCursor()
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
    private Mono<List<TransactionsPostResponseBody>> sendToDbs(Flux<TransactionsPostRequestBody> transactions) {
        return transactions
                .publish(transactionService::processTransactions)
                .flatMapIterable(UnitOfWork::getStreamTasks)
                .flatMapIterable(TransactionTask::getResponse)
                .collectList();
    }

    private TransactionIngestResponse buildResponse(List<TransactionsPostResponseBody> transactions) {
        return TransactionIngestResponse.builder()
                .transactions(transactions)
                .build();
    }

    private void handleSuccess(String arrangementId, List<TransactionsPostResponseBody> transactions) {
        patchCursor(arrangementId, buildPatchCursorRequest(
                TransactionCursor.StatusEnum.SUCCESS,
                OffsetDateTime.now().format(DateTimeFormatter.ofPattern(dateFormat)),
                transactions.stream().map(TransactionsPostResponseBody::getExternalId)
                        .collect(Collectors.joining(","))));

        transactionPostIngestionService.handleSuccess(transactions);

        if (log.isDebugEnabled()) {
            log.debug("Ingested transactions: {}", transactions);
        }
    }

    private Mono<List<TransactionsPostResponseBody>> handleError(String arrangementId, Throwable e) {
        patchCursor(arrangementId, buildPatchCursorRequest(
                TransactionCursor.StatusEnum.FAILED,
                null, null));
        return transactionPostIngestionService.handleFailure(e);
    }

}
