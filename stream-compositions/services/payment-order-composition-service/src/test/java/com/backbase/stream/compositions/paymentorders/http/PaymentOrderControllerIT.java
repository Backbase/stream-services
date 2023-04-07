package com.backbase.stream.compositions.paymentorders.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostResponse;
import com.backbase.stream.PaymentOrderService;
import com.backbase.stream.compositions.paymentorder.api.model.PaymentOrderPostRequest;
import com.backbase.stream.compositions.paymentorder.api.model.PaymentOrderPullIngestionRequest;
import com.backbase.stream.compositions.paymentorder.api.model.PaymentOrderPushIngestionRequest;
import com.backbase.stream.model.response.PaymentOrderIngestDbsResponse;
import com.backbase.stream.paymentorder.PaymentOrderTask;
import com.backbase.stream.worker.model.UnitOfWork;
import com.backbase.streams.compositions.test.IntegrationTest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.apache.activemq.broker.BrokerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@DirtiesContext
@SpringBootTest
@AutoConfigureWebTestClient
@ExtendWith({SpringExtension.class})
@Slf4j
class PaymentOrderControllerIT extends IntegrationTest {

    private static final int INTEGRATION_SERVICE_PORT = 18000;
    private ClientAndServer integrationServer;
    private MockServerClient integrationServerClient;
    private static BrokerService broker;

    @Autowired PaymentOrderController paymentOrderController;

    @MockBean PaymentOrderService paymentOrderService;

    @BeforeAll
    static void initActiveMqBroker() throws Exception {
        broker = new BrokerService();
        broker.setBrokerName("activemq");
        broker.setPersistent(false);
        broker.start();
        broker.waitUntilStarted();
    }

    @BeforeEach
    void initializeIntegrationServer() throws IOException {
        integrationServer = startClientAndServer(INTEGRATION_SERVICE_PORT);
        integrationServerClient = new MockServerClient("localhost", INTEGRATION_SERVICE_PORT);
        integrationServerClient
                .when(request().withMethod("POST").withPath("/integration-api/v2/payment-order"))
                .respond(
                        response()
                                .withStatusCode(200)
                                .withContentType(MediaType.APPLICATION_JSON)
                                .withBody(
                                        readContentFromClasspath(
                                                "integration-data/response.json")));
    }

    @AfterEach
    void stopMockServer() {
        integrationServer.stop();
        while (!integrationServer.hasStopped(3, 100L, TimeUnit.MILLISECONDS)) {}
    }

    @Test
    void pullIngestPaymentOrder_Success() throws Exception {

        URI uri = URI.create("/service-api/v2/ingest/pull");
        WebTestClient webTestClient =
                WebTestClient.bindToController(paymentOrderController).build();

        List<PaymentOrderIngestDbsResponse> responses = new ArrayList<>();

        PaymentOrderTask dbsResTask = new PaymentOrderTask("id", null);
        dbsResTask.setResponses(responses);

        when(paymentOrderService.processPaymentOrder(any()))
                .thenReturn(Flux.just(UnitOfWork.from("id", dbsResTask)));

        PaymentOrderPullIngestionRequest pullIngestionRequest =
                new PaymentOrderPullIngestionRequest()
                        .withInternalUserId("4337f8cc-d66d-41b3-a00e-f71ff15d93cg")
                        .withMemberNumber("memberId")
                        .withServiceAgreementInternalId("4337f8cc-d66d-41b3-a00e-f71ff15d93cf")
                        .withLegalEntityExternalId("leExternalId")
                        .withLegalEntityInternalId("leInternalId");
        webTestClient
                .post()
                .uri(uri)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(Mono.just(pullIngestionRequest), PaymentOrderPullIngestionRequest.class)
                .exchange()
                .expectStatus()
                .isCreated();
    }

    @Test
    void pushIngestPaymentOrder_Success() throws Exception {

        URI uri = URI.create("/service-api/v2/ingest/push");
        WebTestClient webTestClient =
                WebTestClient.bindToController(paymentOrderController).build();

        List<PaymentOrderPostResponse> paymentOrderPostResponses =
                new ObjectMapper()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .readValue(
                                readContentFromClasspath("integration-data/response.json"),
                                new TypeReference<>() {});

        List<PaymentOrderIngestDbsResponse> responses = new ArrayList<>();

        PaymentOrderTask dbsResTask = new PaymentOrderTask("id", null);
        dbsResTask.setResponses(responses);

        when(paymentOrderService.processPaymentOrder(any()))
                .thenReturn(Flux.just(UnitOfWork.from("id", dbsResTask)));

        PaymentOrderPushIngestionRequest pushIngestionRequest =
                new PaymentOrderPushIngestionRequest()
                        .withPaymentOrders(
                                List.of(
                                        new PaymentOrderPostRequest()
                                                .withInternalUserId(
                                                        "4337f8cc-d66d-41b3-a00e-f71ff15d93cg")
                                                .withBankReferenceId("bankRefId")
                                                .withServiceAgreementId(
                                                        "4337f8cc-d66d-41b3-a00e-f71ff15d93cf")
                                                .withPaymentSetupId("paymentSetupId")
                                                .withPaymentSubmissionId("paymentSubmissionId")));

        webTestClient
                .post()
                .uri(uri)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(Mono.just(pushIngestionRequest), PaymentOrderPullIngestionRequest.class)
                .exchange()
                .expectStatus()
                .is4xxClientError();
    }
}
