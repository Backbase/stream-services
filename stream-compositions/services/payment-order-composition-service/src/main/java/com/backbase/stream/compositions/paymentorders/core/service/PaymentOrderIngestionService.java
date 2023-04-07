package com.backbase.stream.compositions.paymentorders.core.service;

import com.backbase.stream.compositions.paymentorders.core.model.PaymentOrderIngestPullRequest;
import com.backbase.stream.compositions.paymentorders.core.model.PaymentOrderIngestPushRequest;
import com.backbase.stream.compositions.paymentorders.core.model.PaymentOrderIngestResponse;
import reactor.core.publisher.Mono;

public interface PaymentOrderIngestionService {

  /**
   * Ingests Payment Order in pull mode.
   *
   * @param ingestPullRequest Ingest pull request
   * @return PaymentOrderIngestResponse
   */
  Mono<PaymentOrderIngestResponse> ingestPull(PaymentOrderIngestPullRequest ingestPullRequest);

  /**
   * Ingests Payment Order in push mode.
   *
   * @param ingestPushRequest Ingest push request
   * @return PaymentOrderIngestResponse
   */
  Mono<PaymentOrderIngestResponse> ingestPush(PaymentOrderIngestPushRequest ingestPushRequest);
}
