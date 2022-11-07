package com.backbase.stream.compositions.paymentorders.core.service.impl;

import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostRequest;
import com.backbase.stream.PaymentOrderService;
import com.backbase.stream.compositions.paymentorders.core.mapper.PaymentOrderMapper;
import com.backbase.stream.compositions.paymentorders.core.model.PaymentOrderIngestPullRequest;
import com.backbase.stream.compositions.paymentorders.core.model.PaymentOrderIngestPushRequest;
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
                .doOnSuccess(this::handleSuccess)
                .onErrorResume(this::handleError)
                .map(paymentOrderIngestContext -> buildResponse(paymentOrderIngestContext, ingestPullRequest));
    }

    @Override
    public Mono<PaymentOrderIngestResponse> ingestPush(PaymentOrderIngestPushRequest ingestPushRequest) {
        return Mono.just(Flux.fromIterable(ingestPushRequest.getPaymentOrders()))
                .flatMap(this::sendToDbs)
                .doOnSuccess(this::handleSuccess)
                .onErrorResume(this::handleError)
                .map(paymentOrderIngestContext -> buildResponse(paymentOrderIngestContext, ingestPushRequest));
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

    private PaymentOrderIngestResponse buildResponse(PaymentOrderIngestContext paymentOrderIngestContext,
                                                    PaymentOrderIngestPullRequest ingestPullRequest) {
        return PaymentOrderIngestResponse.builder()
                .paymentOrderIngestContext(paymentOrderIngestContext)
                .memberNumber(ingestPullRequest.getMemberNumber())
                .build();
    }

    private PaymentOrderIngestResponse buildResponse(PaymentOrderIngestContext paymentOrderIngestContext,
                                                     PaymentOrderIngestPushRequest ingestPushRequest) {
        return PaymentOrderIngestResponse.builder()
                .paymentOrderIngestContext(paymentOrderIngestContext)
                .memberNumber(ingestPushRequest.getMemberNumber())
                .build();
    }

    private void handleSuccess(PaymentOrderIngestContext paymentOrderIngestContext) {
        // if we add cursor in the future, this needs to be updated to success here
        paymentOrderPostIngestionService.handleSuccess(paymentOrderIngestContext);
        log.debug("Ingested payment orders (success): {}", paymentOrderIngestContext);
    }

    private Mono<PaymentOrderIngestContext> handleError(Throwable e) {
        // if we add cursor in the future, this needs to be updated to failure here
        log.debug("Ingested payment orders (fail): {}", e.getMessage());
        return paymentOrderPostIngestionService.handleFailure(e);
    }
}
