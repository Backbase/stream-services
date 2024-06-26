package com.backbase.stream.compositions.paymentorders.core.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.dbs.paymentorder.api.service.v3.model.PaymentOrderPostRequest;
import com.backbase.dbs.paymentorder.api.service.v3.model.PaymentOrderPutRequest;
import com.backbase.stream.PaymentOrderService;
import com.backbase.stream.compositions.paymentorders.core.mapper.PaymentOrderMapper;
import com.backbase.stream.compositions.paymentorders.core.model.PaymentOrderIngestPullRequest;
import com.backbase.stream.compositions.paymentorders.core.model.PaymentOrderIngestPushRequest;
import com.backbase.stream.compositions.paymentorders.core.model.PaymentOrderIngestResponse;
import com.backbase.stream.compositions.paymentorders.core.service.PaymentOrderIngestionService;
import com.backbase.stream.compositions.paymentorders.core.service.PaymentOrderIntegrationService;
import com.backbase.stream.compositions.paymentorders.core.service.PaymentOrderPostIngestionService;
import com.backbase.stream.model.request.DeletePaymentOrderIngestRequest;
import com.backbase.stream.model.request.NewPaymentOrderIngestRequest;
import com.backbase.stream.model.request.PaymentOrderIngestRequest;
import com.backbase.stream.model.request.UpdatePaymentOrderIngestRequest;
import com.backbase.stream.model.response.PaymentOrderIngestDbsResponse;
import com.backbase.stream.paymentorder.PaymentOrderTask;
import com.backbase.stream.worker.model.UnitOfWork;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(
    MockitoExtension.class
)
class PaymentOrderIngestionServiceImplTest {

    private PaymentOrderIngestionService paymentOrderIngestionService;

    @Mock
    private PaymentOrderIntegrationService paymentOrderIntegrationService;

    PaymentOrderMapper mapper = Mappers.getMapper(PaymentOrderMapper.class);

    @Mock
    PaymentOrderService paymentOrderService;

    PaymentOrderPostIngestionService paymentOrderPostIngestionService;

    @Mock
    PaymentOrderMapper paymentOrderMapper;

    @Mock
    EventBus eventBus;

    @BeforeEach
    void setUp() {
        paymentOrderPostIngestionService = new PaymentOrderPostIngestionServiceImpl(eventBus);

        paymentOrderIngestionService = new PaymentOrderIngestionServiceImpl(
            paymentOrderIntegrationService,
            paymentOrderService,
            paymentOrderPostIngestionService,
            mapper
        );
    }

    void mockPaymentOrderService() {
        List<PaymentOrderIngestDbsResponse> responses = new ArrayList<>();

        PaymentOrderTask dbsResTask = new PaymentOrderTask("id", null);
        dbsResTask.setResponses(responses);

        when(paymentOrderService.processPaymentOrder(any())).thenReturn(
                Flux.just(UnitOfWork.from("id", dbsResTask)));
    }

    @Test
    void ingestionInPull() {

        PaymentOrderIngestPullRequest paymentOrderIngestPullRequest = PaymentOrderIngestPullRequest.builder()
            .memberNumber("123")
            .internalUserId("456")
            .legalEntityInternalId("789")
            .legalEntityExternalId("012")
            .dateRangeStart("2019-08-31T23:49:05.629+08:00")
            .dateRangeEnd("2022-01-01T23:49:05.629+08:00")
            .build();

        com.backbase.stream.compositions.paymentorder.integration.client.model.PaymentOrderPostRequest paymentOrderPostRequest = new com.backbase.stream.compositions.paymentorder.integration.client.model.PaymentOrderPostRequest();
        Flux<com.backbase.stream.compositions.paymentorder.integration.client.model.PaymentOrderPostRequest> paymentOrderPostRequestFlux = Flux
            .just(paymentOrderPostRequest);

        when(paymentOrderIntegrationService.pullPaymentOrder(paymentOrderIngestPullRequest))
            .thenReturn(paymentOrderPostRequestFlux);

        String unitOfOWorkId = "payment-orders-mixed-" + System.currentTimeMillis();
        List<PaymentOrderIngestRequest> data = new ArrayList<>();
        data.add(new NewPaymentOrderIngestRequest(new PaymentOrderPostRequest()));
        data.add(new UpdatePaymentOrderIngestRequest(new PaymentOrderPutRequest()));
        data.add(new DeletePaymentOrderIngestRequest("paymentOrderId", "bankReferenceId"));

        List<PaymentOrderIngestDbsResponse> responses = new ArrayList<>();

        PaymentOrderTask paymentOrderTask = new PaymentOrderTask(unitOfOWorkId, data);
        paymentOrderTask.setResponses(responses);

        Stream<UnitOfWork<PaymentOrderTask>> unitOfWorkStream = Stream.of(
            UnitOfWork.from(unitOfOWorkId, paymentOrderTask)
        );
        Flux<UnitOfWork<PaymentOrderTask>> unitOfWorkFlux = Flux.fromStream(unitOfWorkStream);

        when(paymentOrderService.processPaymentOrder(any())).thenReturn(unitOfWorkFlux);

        Mono<PaymentOrderIngestResponse> paymentOrderIngestResponseMono = paymentOrderIngestionService
            .ingestPull(paymentOrderIngestPullRequest);

        StepVerifier.create(paymentOrderIngestResponseMono)
            .assertNext(Assertions::assertNotNull)
            .verifyComplete();
    }

    @Test
    void ingestionInPush() {
        mockPaymentOrderService();
        PaymentOrderIngestPushRequest request = PaymentOrderIngestPushRequest.builder()
                .internalUserId("userId")
                .paymentOrders(Collections.singletonList(
                        new PaymentOrderPostRequest()
                                .internalUserId("userId")
                                .bankReferenceId("bankRefId")
                                .serviceAgreementId("4337f8cc-d66d-41b3-a00e-f71ff15d93cf")
                                .paymentSetupId("paymentSetupId")
                                .paymentSubmissionId("paymentSubmissionId")))
                .build();

        Mono<PaymentOrderIngestResponse> productIngestResponse = paymentOrderIngestionService
                .ingestPush(request);
        StepVerifier.create(productIngestResponse)
                .assertNext(Assertions::assertNotNull).verifyComplete();
    }

}
