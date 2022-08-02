package com.backbase.stream.compositions.paymentorders.core.service;

import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostResponse;
import reactor.core.publisher.Mono;

import java.util.List;

public interface PaymentOrderPostIngestionService {
    /**
     * Post processing for a completed ingestion process
     * @param response
     */
    void handleSuccess(List<PaymentOrderPostResponse> response);

    /**
     * Post processing for a failed ingestion process
     * @param error
     */
    Mono<List<PaymentOrderPostResponse>> handleFailure(Throwable error);
}
