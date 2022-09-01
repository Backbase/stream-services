package com.backbase.stream.compositions.paymentorders.core.service;

import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostResponse;
import com.backbase.stream.model.PaymentOrderIngestContext;
import reactor.core.publisher.Mono;

import java.util.List;

public interface PaymentOrderPostIngestionService {
    /**
     * Post processing for a completed ingestion process
     * @param response
     */
    // todo with payment context
     void handleSuccess(PaymentOrderIngestContext response);
//    void handleSuccess(List<PaymentOrderPostResponse> response);

    /**
     * Post processing for a failed ingestion process
     * @param error
     */
    //todo with payment context
    Mono<PaymentOrderIngestContext> handleFailure(Throwable error);
//    Mono<List<PaymentOrderPostResponse>> handleFailure(Throwable error);
}
