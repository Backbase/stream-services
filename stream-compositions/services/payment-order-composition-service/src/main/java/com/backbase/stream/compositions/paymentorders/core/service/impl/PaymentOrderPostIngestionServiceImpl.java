package com.backbase.stream.compositions.paymentorders.core.service.impl;

import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.stream.compositions.paymentorders.core.config.PaymentOrderConfigurationProperties;
import com.backbase.stream.compositions.paymentorders.core.service.PaymentOrderPostIngestionService;
import com.backbase.stream.model.response.PaymentOrderIngestDbsResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@AllArgsConstructor
public class PaymentOrderPostIngestionServiceImpl implements PaymentOrderPostIngestionService {

    private final EventBus eventBus;

    private final PaymentOrderConfigurationProperties paymentOrderConfigurationProperties;

    @Override
    public void handleSuccess(List<PaymentOrderIngestDbsResponse> paymentOrderIngestDbsResponses) {
        log.info("Payment Order ingestion completed successfully.");
        // events can be handled here as part of a different ticket.
    }

    @Override
    public Mono<List<PaymentOrderIngestDbsResponse>> handleFailure(Throwable error) {
        // events can be handled here as part of a different ticket.
        return Mono.empty();
    }
}
