package com.backbase.stream.compositions.paymentorders.core.service;

import com.backbase.stream.model.PaymentOrderIngestContext;
import reactor.core.publisher.Mono;

public interface PaymentOrderPostIngestionService {
    /**
     * Post processing for a completed ingestion process
     * @param response
     */
     void handleSuccess(PaymentOrderIngestContext response);

    /**
     * Post processing for a failed ingestion process
     * @param error
     */
    Mono<PaymentOrderIngestContext> handleFailure(Throwable error);
}
