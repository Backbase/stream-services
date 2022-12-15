package com.backbase.stream.compositions.paymentorders.core.service.impl;

import org.springframework.stereotype.Service;
import com.backbase.stream.compositions.paymentorder.integration.client.PaymentOrderIntegrationApi;
import com.backbase.stream.compositions.paymentorder.integration.client.model.PaymentOrderPostRequest;
import com.backbase.stream.compositions.paymentorder.integration.client.model.PullPaymentOrderResponse;
import com.backbase.stream.compositions.paymentorders.core.mapper.PaymentOrderMapper;
import com.backbase.stream.compositions.paymentorders.core.model.PaymentOrderIngestPullRequest;
import com.backbase.stream.compositions.paymentorders.core.service.PaymentOrderIntegrationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@AllArgsConstructor
public class PaymentOrderIntegrationServiceImpl implements PaymentOrderIntegrationService {

    private final PaymentOrderIntegrationApi paymentOrderIntegrationApi;
    private final PaymentOrderMapper paymentOrderMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public Flux<PaymentOrderPostRequest> pullPaymentOrder(PaymentOrderIngestPullRequest ingestPullRequest) {
        return paymentOrderIntegrationApi
                .pullPaymentOrders(
                        paymentOrderMapper.mapStreamToIntegration(ingestPullRequest))
                .flatMapIterable(PullPaymentOrderResponse::getPaymentOrder);
    }
}
