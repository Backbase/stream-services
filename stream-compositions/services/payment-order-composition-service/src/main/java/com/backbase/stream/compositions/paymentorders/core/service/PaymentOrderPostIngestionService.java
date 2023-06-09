package com.backbase.stream.compositions.paymentorders.core.service;

import com.backbase.stream.model.response.PaymentOrderIngestDbsResponse;
import java.util.List;
import reactor.core.publisher.Mono;

public interface PaymentOrderPostIngestionService {

    /**
     * Post processing for a completed ingestion process
     *
     * @param paymentOrderIngestDbsResponses
     */
    void handleSuccess(List<PaymentOrderIngestDbsResponse> paymentOrderIngestDbsResponses);

    /**
     * Post processing for a failed ingestion process
     *
     * @param error
     */
    Mono<List<PaymentOrderIngestDbsResponse>> handleFailure(Throwable error);
}
