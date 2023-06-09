package com.backbase.stream.compositions.paymentorders.core.service;

import com.backbase.stream.compositions.paymentorder.integration.client.model.PaymentOrderPostRequest;
import com.backbase.stream.compositions.paymentorders.core.model.PaymentOrderIngestPullRequest;
import reactor.core.publisher.Flux;

public interface PaymentOrderIntegrationService {

    /**
     * Pulls payment order from external integration service.
     *
     * @param ingestPullRequest PaymentOrderIngestPullRequest
     * @return Mono<PaymentOrderPostRequestBody>
     */
    Flux<PaymentOrderPostRequest> pullPaymentOrder(PaymentOrderIngestPullRequest ingestPullRequest);
}
