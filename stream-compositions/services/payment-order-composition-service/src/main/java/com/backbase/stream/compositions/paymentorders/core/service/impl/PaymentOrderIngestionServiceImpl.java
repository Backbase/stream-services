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
import com.backbase.stream.model.response.PaymentOrderIngestDbsResponse;
import com.backbase.stream.worker.model.UnitOfWork;
import java.util.List;
import java.util.stream.Collectors;
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
  public Mono<PaymentOrderIngestResponse> ingestPull(
      PaymentOrderIngestPullRequest ingestPullRequest) {

    return buildIntegrationRequest(ingestPullRequest)
        .map(this::pullPaymentOrder)
        .flatMap(this::sendToDbs)
        .doOnSuccess(this::handleSuccess)
        .onErrorResume(this::handleError)
        .map(
            paymentOrderIngestDbsResponses ->
                buildResponse(paymentOrderIngestDbsResponses, ingestPullRequest));
  }

  @Override
  public Mono<PaymentOrderIngestResponse> ingestPush(
      PaymentOrderIngestPushRequest ingestPushRequest) {
    return Mono.just(Flux.fromIterable(ingestPushRequest.getPaymentOrders()))
        .flatMap(this::sendToDbs)
        .doOnSuccess(this::handleSuccess)
        .onErrorResume(this::handleError)
        .map(
            paymentOrderIngestDbsResponses ->
                buildResponse(paymentOrderIngestDbsResponses, ingestPushRequest));
  }

  private Mono<PaymentOrderIngestPullRequest> buildIntegrationRequest(
      PaymentOrderIngestPullRequest paymentOrderIngestPullRequest) {
    return Mono.just(paymentOrderIngestPullRequest);
  }

  /**
   * Pulls and remap payment order from integration service.
   *
   * @param request PaymentOrderIngestPullRequest
   * @return Flux<PaymentOrderPostRequestBody>
   */
  private Flux<PaymentOrderPostRequest> pullPaymentOrder(PaymentOrderIngestPullRequest request) {
    return paymentOrderIntegrationService
        .pullPaymentOrder(request)
        .map(paymentOrderMapper::mapIntegrationToStream);
  }

  private Mono<List<PaymentOrderIngestDbsResponse>> sendToDbs(
      Flux<PaymentOrderPostRequest> paymentOrderPostRequestFlux) {

    return paymentOrderPostRequestFlux
        .publish(paymentOrderService::processPaymentOrder)
        .flatMapIterable(UnitOfWork::getStreamTasks)
        .collectList()
        .flatMap(
            paymentOrderTaskList -> {
              List<PaymentOrderIngestDbsResponse> paymentOrderIngestDbsResponses =
                  paymentOrderTaskList.stream()
                      .flatMap(paymentOrderTask -> paymentOrderTask.getResponses().stream())
                      .collect(Collectors.toList());
              return Mono.just(paymentOrderIngestDbsResponses);
            });
  }

  private PaymentOrderIngestResponse buildResponse(
      List<PaymentOrderIngestDbsResponse> paymentOrderIngestDbsResponses,
      PaymentOrderIngestPullRequest ingestPullRequest) {
    return PaymentOrderIngestResponse.builder()
        .paymentOrderIngestDbsResponses(paymentOrderIngestDbsResponses)
        .memberNumber(ingestPullRequest.getMemberNumber())
        .build();
  }

  private PaymentOrderIngestResponse buildResponse(
      List<PaymentOrderIngestDbsResponse> paymentOrderIngestDbsResponses,
      PaymentOrderIngestPushRequest ingestPushRequest) {
    return PaymentOrderIngestResponse.builder()
        .paymentOrderIngestDbsResponses(paymentOrderIngestDbsResponses)
        .memberNumber(ingestPushRequest.getMemberNumber())
        .build();
  }

  private void handleSuccess(List<PaymentOrderIngestDbsResponse> paymentOrderIngestDbsResponses) {
    // if we add cursor in the future, this needs to be updated to success here
    paymentOrderPostIngestionService.handleSuccess(paymentOrderIngestDbsResponses);
    log.debug("Ingested payment orders (success): {}", paymentOrderIngestDbsResponses);
  }

  private Mono<List<PaymentOrderIngestDbsResponse>> handleError(Throwable e) {
    // if we add cursor in the future, this needs to be updated to failure here
    log.debug("Ingested payment orders (fail): {}", e.getMessage());
    return paymentOrderPostIngestionService.handleFailure(e);
  }
}
