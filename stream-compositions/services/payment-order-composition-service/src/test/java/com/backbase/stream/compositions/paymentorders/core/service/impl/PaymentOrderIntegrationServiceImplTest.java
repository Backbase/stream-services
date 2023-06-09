package com.backbase.stream.compositions.paymentorders.core.service.impl;

import static org.mockito.Mockito.when;

import com.backbase.stream.compositions.paymentorder.integration.client.PaymentOrderIntegrationApi;
import com.backbase.stream.compositions.paymentorder.integration.client.model.PaymentOrderPostRequest;
import com.backbase.stream.compositions.paymentorder.integration.client.model.PullIngestionRequest;
import com.backbase.stream.compositions.paymentorder.integration.client.model.PullPaymentOrderResponse;
import com.backbase.stream.compositions.paymentorders.core.mapper.PaymentOrderMapper;
import com.backbase.stream.compositions.paymentorders.core.model.PaymentOrderIngestPullRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class PaymentOrderIntegrationServiceImplTest {

    @Mock
    private PaymentOrderIntegrationApi paymentOrderIntegrationApi;

    @Mock
    private PaymentOrderMapper paymentOrderMapper;

    private PaymentOrderIntegrationServiceImpl paymentOrderIntegrationService;

    @BeforeEach
    void setUp() {
        paymentOrderIntegrationService =
            new PaymentOrderIntegrationServiceImpl(paymentOrderIntegrationApi, paymentOrderMapper);
    }

    @Test
    void pullPaymentOrderTest() {
        PaymentOrderIngestPullRequest paymentOrderIngestPullRequest =
            PaymentOrderIngestPullRequest.builder()
                .memberNumber("123")
                .internalUserId("456")
                .legalEntityInternalId("789")
                .legalEntityExternalId("012")
                .dateRangeStart("2019-08-31T23:49:05.629+08:00")
                .dateRangeEnd("2022-01-01T23:49:05.629+08:00")
                .build();

        PullIngestionRequest pullIngestionRequest =
            new PullIngestionRequest()
                .withArrangementId("arrangementId")
                .withExternalArrangementId("externalArrangementId")
                .withDateRangeStart("dateRangeStart")
                .withDateRangeEnd("dateRangeEnd");

        when(paymentOrderMapper.mapStreamToIntegration(paymentOrderIngestPullRequest))
            .thenReturn(pullIngestionRequest);

        PaymentOrderPostRequest paymentOrderPostRequest = new PaymentOrderPostRequest();

        PullPaymentOrderResponse pullPaymentOrderResponse =
            new PullPaymentOrderResponse().addPaymentOrderItem(paymentOrderPostRequest);

        when(paymentOrderIntegrationApi.pullPaymentOrders(pullIngestionRequest))
            .thenReturn(Mono.just(pullPaymentOrderResponse));

        StepVerifier.create(
                paymentOrderIntegrationService.pullPaymentOrder(paymentOrderIngestPullRequest))
            .expectNext(paymentOrderPostRequest)
            .expectComplete()
            .verify();
    }
}
