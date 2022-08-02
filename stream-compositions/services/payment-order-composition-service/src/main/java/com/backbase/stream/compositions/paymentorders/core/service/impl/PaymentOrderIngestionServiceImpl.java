package com.backbase.stream.compositions.paymentorders.core.service.impl;

import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostRequest;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostResponse;
import com.backbase.stream.PaymentOrderService;
import com.backbase.stream.compositions.paymentorders.core.config.PaymentOrderConfigurationProperties;
import com.backbase.stream.compositions.paymentorders.core.mapper.PaymentOrderMapper;
import com.backbase.stream.compositions.paymentorders.core.model.PaymentOrderIngestPullRequest;
import com.backbase.stream.compositions.paymentorders.core.model.PaymentOrderIngestResponse;
import com.backbase.stream.compositions.paymentorders.core.service.PaymentOrderIngestionService;
import com.backbase.stream.compositions.paymentorders.core.service.PaymentOrderIntegrationService;
import com.backbase.stream.compositions.paymentorders.core.service.PaymentOrderPostIngestionService;
import com.backbase.stream.paymentorder.PaymentOrderTask;
import com.backbase.stream.worker.model.UnitOfWork;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class PaymentOrderIngestionServiceImpl implements PaymentOrderIngestionService {

    private final PaymentOrderIntegrationService paymentOrderIntegrationService;
    private final PaymentOrderService paymentOrderService;
    private final PaymentOrderPostIngestionService paymentOrderPostIngestionService;

    private final PaymentOrderConfigurationProperties config;

    private final PaymentOrderMapper paymentOrderMapper;


    @Override
    public Mono<PaymentOrderIngestResponse> ingestPull(PaymentOrderIngestPullRequest ingestPullRequest) {
        System.out.println("PaymentOrderIngestionServiceImpl :: ingestPull");
        return buildIntegrationRequest(ingestPullRequest)
                .map(this::pullPaymentOrder)
//                .map(f -> filterExisting(f, ingestPullRequest.getLastIngestedExternalIds()))
                .flatMap(this::sendToDbs)
                .doOnSuccess(list -> handleSuccess(
                        ingestPullRequest.getArrangementId(), list))
                .onErrorResume(e -> handleError(
                        ingestPullRequest.getArrangementId(), e))
                .map(list -> buildResponse(list, ingestPullRequest));
//        return null;
    }

    private Mono<PaymentOrderIngestPullRequest> buildIntegrationRequest(PaymentOrderIngestPullRequest paymentOrderIngestPullRequest) {
//        OffsetDateTime currentTime = OffsetDateTime.now();
//        OffsetDateTime dateRangeStartFromRequest = paymentOrderIngestPullRequest.getDateRangeStart();
//        OffsetDateTime dateRangeEndFromRequest = paymentOrderIngestPullRequest.getDateRangeEnd();
//
//        paymentOrderIngestPullRequest.setDateRangeStart(dateRangeStartFromRequest == null
//                ? currentTime.minusDays(config.getDefaultStartOffsetInDays()) : dateRangeStartFromRequest);
//        paymentOrderIngestPullRequest.setDateRangeEnd(dateRangeEndFromRequest == null
//                ? currentTime : dateRangeEndFromRequest);
//
//        if (dateRangeStartFromRequest == null && config.isCursorEnabled()) {
//            log.info("Transaction Cursor is enabled and Request has no Start Date");
//            return getCursor(transactionIngestPullRequest)
//                    .switchIfEmpty(createCursor(transactionIngestPullRequest));
//        }

        return Mono.just(paymentOrderIngestPullRequest);
    }
//    public static final String DELIMITER = ",";
//    public static final String dateFormat = "yyyy-MM-dd hh:mm:ss";
//    private final TransactionMapper mapper;
//    private final TransactionService transactionService;
//    private final TransactionIntegrationService transactionIntegrationService;
//
//    private final TransactionPostIngestionService transactionPostIngestionService;
//
//    private final TransactionCursorApi transactionCursorApi;
//
//    private final TransactionConfigurationProperties config;
//
//    private final DateTimeFormatter offsetDateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
//
//    /**
//     * Ingests transactions in pull mode.
//     *
//     * @param ingestPullRequest Ingest pull request
//     * @return TransactionIngestResponse
//     */
//    public Mono<TransactionIngestResponse> ingestPull(TransactionIngestPullRequest ingestPullRequest) {
//        return buildIntegrationRequest(ingestPullRequest)
//                .map(this::pullTransactions)
//                .map(f -> filterExisting(f, ingestPullRequest.getLastIngestedExternalIds()))
//                .flatMap(this::sendToDbs)
//                .doOnSuccess(list -> handleSuccess(
//                        ingestPullRequest.getArrangementId(), list))
//                .onErrorResume(e -> handleError(
//                        ingestPullRequest.getArrangementId(), e))
//                .map(list -> buildResponse(list, ingestPullRequest));
//    }
//
//    /*
//        Filter by existing/already ingested external ids only works
//        - if the list is not empty
//        - if the filter is enabled in configuration
//     */
//    private Flux<TransactionsPostRequestBody> filterExisting(Flux<TransactionsPostRequestBody> transactionsPostRequestBodyFlux,
//                                   List<String> lastIngestedExternalIds) {
//        if (!config.isTransactionIdsFilterEnabled() ||
//                CollectionUtils.isEmpty(lastIngestedExternalIds)) {
//            return transactionsPostRequestBodyFlux;
//        }
//
//        return transactionsPostRequestBodyFlux.filter(t -> !lastIngestedExternalIds.contains(t.getExternalId()));
//    }
//
//    private Mono<TransactionIngestPullRequest> buildIntegrationRequest(TransactionIngestPullRequest transactionIngestPullRequest) {
//        OffsetDateTime currentTime = OffsetDateTime.now();
//        OffsetDateTime dateRangeStartFromRequest = transactionIngestPullRequest.getDateRangeStart();
//        OffsetDateTime dateRangeEndFromRequest = transactionIngestPullRequest.getDateRangeEnd();
//
//        transactionIngestPullRequest.setDateRangeStart(dateRangeStartFromRequest == null
//                ? currentTime.minusDays(config.getDefaultStartOffsetInDays()) : dateRangeStartFromRequest);
//        transactionIngestPullRequest.setDateRangeEnd(dateRangeEndFromRequest == null
//                ? currentTime : dateRangeEndFromRequest);
//
//        if (dateRangeStartFromRequest == null && config.isCursorEnabled()) {
//            log.info("Transaction Cursor is enabled and Request has no Start Date");
//            return getCursor(transactionIngestPullRequest)
//                    .switchIfEmpty(createCursor(transactionIngestPullRequest));
//        }
//
//        return Mono.just(transactionIngestPullRequest);
//    }
//
//    /**
//     * Ingests product group in push mode.
//     *
//     * @param ingestPushRequest Ingest push request
//     * @return ProductIngestResponse
//     */
//    public Mono<TransactionIngestResponse> ingestPush(TransactionIngestPushRequest ingestPushRequest) {
//        throw new UnsupportedOperationException();
//    }
//
    /**
     * Pulls and remap payment order from integration service.
     *
     * @param request PaymentOrderIngestPullRequest
     * @return Flux<PaymentOrderPostRequestBody>
     */
    private Flux<PaymentOrderPostRequest> pullPaymentOrder(PaymentOrderIngestPullRequest request) {
       return paymentOrderIntegrationService.pullPaymentOrder(request)
                .map(paymentOrderMapper::mapIntegrationToStream);
    }
//
//    private Mono<TransactionIngestPullRequest> getCursor(TransactionIngestPullRequest request) {
//        return transactionCursorApi.getByArrangementId(request.getArrangementId())
//                .map(TransactionCursorResponse::getCursor)
//                .flatMap(cursor -> mapCursorToIntegrationRequest(request, cursor))
//                .doOnNext(r -> {
//                    log.info("Patch Transaction Cursor to IN_PROGRESS for :: {}", r.getArrangementId());
//                    patchCursor(r.getArrangementId(),
//                            buildPatchCursorRequest(
//                                    TransactionCursor.StatusEnum.IN_PROGRESS, null, null));
//                });
//    }
//
//    private void patchCursor(String arrangementId, TransactionCursorPatchRequest request) {
//        transactionCursorApi.patchByArrangementId(arrangementId, request).subscribe();
//    }
//
//    private TransactionCursorPatchRequest buildPatchCursorRequest(TransactionCursor.StatusEnum statusEnum,
//                                                                  String lastTxnDate, String lastTxnIds) {
//        TransactionCursorPatchRequest cursorPatchRequest = new TransactionCursorPatchRequest()
//                .withStatus(statusEnum.toString());
//
//        if (TransactionCursor.StatusEnum.SUCCESS.equals(statusEnum)) {
//            cursorPatchRequest.setLastTxnDate(lastTxnDate);
//            cursorPatchRequest.setLastTxnIds(lastTxnIds);
//        }
//
//        return cursorPatchRequest;
//    }
//
//    private Mono<TransactionIngestPullRequest> createCursor(TransactionIngestPullRequest request) {
//        return transactionCursorApi.upsertCursor(buildUpsertCursorRequest(request))
//                .map(TransactionCursorUpsertResponse::getId)
//                .flatMap(this::getCursorById)
//                .map(TransactionCursorResponse::getCursor)
//                .flatMap(cursor -> mapCursorToIntegrationRequest(request, cursor));
//    }
//
//    private Mono<TransactionCursorResponse> getCursorById(String id) {
//        return transactionCursorApi.getById(id);
//    }
//
//    private Mono<TransactionIngestPullRequest> mapCursorToIntegrationRequest(
//            TransactionIngestPullRequest request, TransactionCursor cursor) {
//
//        if (cursor.getLastTxnDate() != null) {
//            OffsetDateTime dateRangeStart = OffsetDateTime.parse(cursor.getLastTxnDate(), offsetDateTimeFormatter);
//            request.setDateRangeStart(dateRangeStart);
//        }
//
//        if (!CollectionUtils.isEmpty(cursor.getLastTxnIds())) {
//            request.setLastIngestedExternalIds(cursor.getLastTxnIds());
//        }
//
//        return Mono.just(request);
//    }
//
//
//    private TransactionCursorUpsertRequest buildUpsertCursorRequest(TransactionIngestPullRequest request) {
//        return new TransactionCursorUpsertRequest()
//                .withCursor(new TransactionCursor()
//                        .withArrangementId(request.getArrangementId())
//                        .withLegalEntityId(request.getLegalEntityInternalId())
//                        .withExtArrangementId(request.getExternalArrangementId())
//                        .withStatus(TransactionCursor.StatusEnum.IN_PROGRESS));
//    }
//
    /**
     * Ingests Payment Orders to DBS.
     *
     * @param paymentOrderPostRequestFlux Payment Order
     * @return Ingested Payment Orders
     */
    private Mono<List<PaymentOrderPostResponse>> sendToDbs(Flux<PaymentOrderPostRequest> paymentOrderPostRequestFlux) {
        System.out.println("PaymentOrderIngestionServiceImpl :: sendToDbs");
        return paymentOrderPostRequestFlux
                .publish(paymentOrderService::processPaymentOrder)
                .flatMapIterable(UnitOfWork::getStreamTasks)
                .flatMapIterable(PaymentOrderTask::getResponse)
                .collectList();
    }

    private PaymentOrderIngestResponse buildResponse(List<PaymentOrderPostResponse> paymentOrderPostResponses,
                                                    PaymentOrderIngestPullRequest ingestPullRequest) {
        return PaymentOrderIngestResponse.builder()
                .paymentOrderPostResponses(paymentOrderPostResponses)
                .arrangementId(ingestPullRequest.getArrangementId())
                .build();
    }
//
    private void handleSuccess(String arrangementId, List<PaymentOrderPostResponse> paymentOrderPostResponses) {
        System.out.println("PaymentOrderIngestionServiceImpl :: handleSuccess");
//        if (config.isCursorEnabled()) {
//            String lastTxnIds = null;
//            if (config.isTransactionIdsFilterEnabled()) {
//                log.info("Transaction Id based filter is enabled");
//                lastTxnIds = transactions.stream().map(TransactionsPostResponseBody::getExternalId)
//                        .collect(Collectors.joining(DELIMITER));
//            }
//            patchCursor(arrangementId, buildPatchCursorRequest(
//                    TransactionCursor.StatusEnum.SUCCESS,
//                    OffsetDateTime.now().format(DateTimeFormatter.ofPattern(dateFormat)),
//                    lastTxnIds));
//        }

        paymentOrderPostIngestionService.handleSuccess(paymentOrderPostResponses);

        log.debug("Ingested transactions: {}", paymentOrderPostResponses);
    }

    private Mono<List<PaymentOrderPostResponse>> handleError(String arrangementId, Throwable e) {
        // todo - probably not required for payments
//        if (config.isCursorEnabled()) {
//            patchCursor(arrangementId, buildPatchCursorRequest(
//                    TransactionCursor.StatusEnum.FAILED,
//                    null, null));
//        }
        System.out.println("PaymentOrderIngestionServiceImpl :: handleError");
        return paymentOrderPostIngestionService.handleFailure(e);
    }

}
