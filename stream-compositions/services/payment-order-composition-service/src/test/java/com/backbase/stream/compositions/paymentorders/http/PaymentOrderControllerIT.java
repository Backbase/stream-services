package com.backbase.stream.compositions.paymentorders.http;

import com.backbase.stream.PaymentOrderService;
import com.backbase.stream.compositions.paymentorder.api.model.PaymentOrderPostRequest;
import com.backbase.stream.compositions.paymentorder.api.model.PaymentOrderPullIngestionRequest;
import com.backbase.stream.compositions.paymentorder.api.model.PaymentOrderPushIngestionRequest;
import com.backbase.stream.compositions.paymentorders.core.mapper.PaymentOrderMapper;
import com.backbase.stream.compositions.paymentorders.core.model.PaymentOrderIngestResponse;
import com.backbase.stream.compositions.paymentorders.core.service.PaymentOrderIngestionService;
import com.backbase.stream.model.response.PaymentOrderIngestDbsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentOrderControllerIT {

    @Mock
    private PaymentOrderIngestionService paymentOrderIngestionService;
    @Mock
    private PaymentOrderService paymentOrderService;

    private PaymentOrderController paymentOrderController;

    @BeforeEach
    void setUp() {
        PaymentOrderMapper paymentOrderMapper = Mappers.getMapper(PaymentOrderMapper.class);
        paymentOrderController = new PaymentOrderController(
                paymentOrderIngestionService,
                paymentOrderService,
                paymentOrderMapper);
    }

    @Test
    void pullIngestPaymentOrder_Success() {

        URI uri = URI.create("/service-api/v2/ingest/pull");
        WebTestClient webTestClient = WebTestClient.bindToController(paymentOrderController).build();

        List<PaymentOrderIngestDbsResponse> responses = new ArrayList<>();

        when(paymentOrderIngestionService.ingestPull(any())).thenReturn(
                Mono.just(PaymentOrderIngestResponse.builder()
                        .paymentOrderIngestDbsResponses(responses)
                        .memberNumber("memberId")
                        .build()));

        PaymentOrderPullIngestionRequest pullIngestionRequest =
                new PaymentOrderPullIngestionRequest()
                        .internalUserId("4337f8cc-d66d-41b3-a00e-f71ff15d93cg")
                        .memberNumber("memberId")
                        .serviceAgreementInternalId("4337f8cc-d66d-41b3-a00e-f71ff15d93cf")
                        .legalEntityExternalId("leExternalId")
                        .legalEntityInternalId("leInternalId");
        webTestClient.post().uri(uri)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(Mono.just(pullIngestionRequest), PaymentOrderPullIngestionRequest.class).exchange()
                .expectStatus().isCreated();

    }

    @Test
    void pushIngestPaymentOrder_Success() {

        URI uri = URI.create("/service-api/v2/ingest/push");
        WebTestClient webTestClient = WebTestClient.bindToController(paymentOrderController).build();


        PaymentOrderPushIngestionRequest pushIngestionRequest =
                new PaymentOrderPushIngestionRequest()
                        .paymentOrders(List.of(new PaymentOrderPostRequest()
                                .internalUserId("4337f8cc-d66d-41b3-a00e-f71ff15d93cg")
                                .bankReferenceId("bankRefId")
                                .serviceAgreementId("4337f8cc-d66d-41b3-a00e-f71ff15d93cf")
                                .paymentSetupId("paymentSetupId")
                                .paymentSubmissionId("paymentSubmissionId")));

        webTestClient.post().uri(uri)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(Mono.just(pushIngestionRequest), PaymentOrderPullIngestionRequest.class).exchange()
                .expectStatus().is4xxClientError();

    }
}
