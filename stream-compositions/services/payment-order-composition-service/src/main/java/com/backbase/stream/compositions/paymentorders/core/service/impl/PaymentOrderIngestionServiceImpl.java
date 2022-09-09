package com.backbase.stream.compositions.paymentorders.core.service.impl;

import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostRequest;
import com.backbase.stream.PaymentOrderService;
import com.backbase.stream.compositions.paymentorders.core.mapper.PaymentOrderMapper;
import com.backbase.stream.compositions.paymentorders.core.model.PaymentOrderIngestPullRequest;
import com.backbase.stream.compositions.paymentorders.core.model.PaymentOrderIngestResponse;
import com.backbase.stream.compositions.paymentorders.core.service.PaymentOrderIngestionService;
import com.backbase.stream.compositions.paymentorders.core.service.PaymentOrderIntegrationService;
import com.backbase.stream.compositions.paymentorders.core.service.PaymentOrderPostIngestionService;
import com.backbase.stream.model.PaymentOrderIngestContext;
import com.backbase.stream.worker.model.UnitOfWork;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@AllArgsConstructor
public class PaymentOrderIngestionServiceImpl implements PaymentOrderIngestionService {

    private final PaymentOrderIntegrationService paymentOrderIntegrationService;
    private final PaymentOrderService paymentOrderService;
    private final PaymentOrderPostIngestionService paymentOrderPostIngestionService;

    private final PaymentOrderMapper paymentOrderMapper;


    @Override
    public Mono<PaymentOrderIngestResponse> ingestPull(PaymentOrderIngestPullRequest ingestPullRequest) {

        return buildIntegrationRequest(ingestPullRequest)
                .map(this::pullPaymentOrder)
                .flatMap(this::sendToDbs)
                .doOnSuccess(paymentOrderIngestContext -> handleSuccess(
                        paymentOrderIngestContext.accountNumber(), paymentOrderIngestContext))
                .onErrorResume(e -> handleError(
                        ingestPullRequest.getMemberNumber(), e))
                .map(paymentOrderIngestContext -> buildResponse(paymentOrderIngestContext, ingestPullRequest));
    }

    private Mono<PaymentOrderIngestPullRequest> buildIntegrationRequest(PaymentOrderIngestPullRequest paymentOrderIngestPullRequest) {
        return Mono.just(paymentOrderIngestPullRequest);
    }

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

    private Mono<PaymentOrderIngestContext> sendToDbs(Flux<PaymentOrderPostRequest> paymentOrderPostRequestFlux) {

        return paymentOrderPostRequestFlux
                .publish(paymentOrderService::processPaymentOrder)
                .flatMapIterable(UnitOfWork::getStreamTasks)
                .next()
                .flatMap(x -> Mono.just(x.getResponse()));
    }

    private PaymentOrderIngestResponse buildResponse(PaymentOrderIngestContext paymentOrderIngestContext,//List<PaymentOrderPostResponse> paymentOrderPostResponses,
                                                    PaymentOrderIngestPullRequest ingestPullRequest) {
        return PaymentOrderIngestResponse.builder()
                .paymentOrderIngestContext(paymentOrderIngestContext)
                .memberNumber(ingestPullRequest.getMemberNumber())
                .build();
    }

    private void handleSuccess(String arrangementId, PaymentOrderIngestContext paymentOrderIngestContext) {//List<PaymentOrderPostResponse> paymentOrderPostResponses) {
        // if we add cursor in the future, this needs to be updated to success here
        paymentOrderPostIngestionService.handleSuccess(paymentOrderIngestContext);
        log.debug("Ingested payment orders: {}", paymentOrderIngestContext);
    }

    private Mono<PaymentOrderIngestContext> handleError(String arrangementId, Throwable e) { //Mono<List<PaymentOrderPostResponse>> handleError(String arrangementId, Throwable e) {
        // if we add cursor in the future, this needs to be updated to failure here
        log.debug("Ingested payment orders: {}", e.getMessage());
        return paymentOrderPostIngestionService.handleFailure(e);
    }
}
