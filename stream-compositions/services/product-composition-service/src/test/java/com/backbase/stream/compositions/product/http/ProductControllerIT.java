package com.backbase.stream.compositions.product.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementItemPut;
import com.backbase.stream.compositions.paymentorder.client.model.PaymentOrderIngestionResponse;
import com.backbase.stream.compositions.paymentorder.client.model.PaymentOrderPostResponse;
import com.backbase.stream.compositions.product.api.model.ArrangementPullIngestionRequest;
import com.backbase.stream.compositions.product.api.model.ArrangementPushIngestionRequest;
import com.backbase.stream.compositions.product.api.model.ProductPullIngestionRequest;
import com.backbase.stream.compositions.product.api.model.ProductPushIngestionRequest;
import com.backbase.stream.compositions.transaction.client.model.TransactionIngestionResponse;
import com.backbase.stream.compositions.transaction.client.model.TransactionsPostResponseBody;
import com.backbase.stream.legalentity.model.BatchProductGroup;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.product.BatchProductIngestionSaga;
import com.backbase.stream.product.service.ArrangementService;
import com.backbase.stream.product.task.BatchProductGroupTask;
import com.backbase.stream.product.task.ProductGroupTask;
import com.backbase.streams.compositions.test.IntegrationTest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@DirtiesContext
@SpringBootTest
@AutoConfigureWebTestClient
@ExtendWith({SpringExtension.class})
@Slf4j
class ProductControllerIT extends IntegrationTest {

  private static final int INTEGRATION_SERVICE_PORT = 18000;
  private static final int TRANSACTION_SERVICE_PORT = 12000;
  private static final int PAYMENT_ORDER_SERVICE_PORT = 13000;
  private static BrokerService broker;

  @MockBean
  @Qualifier("batchProductIngestionSaga")
  BatchProductIngestionSaga batchProductIngestionSaga;

  @MockBean ArrangementService arrangementService;
  @Autowired ProductController productController;
  @Autowired ObjectMapper objectMapper;
  private ClientAndServer integrationServer;
  private ClientAndServer transactionServer;
  private ClientAndServer paymentOrderServer;
  private MockServerClient integrationServerClient;
  private MockServerClient transactionServerClient;
  private MockServerClient paymentOrderServerClient;

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
        .when(request().withMethod("POST").withPath("/service-api/v2/product-group"))
        .respond(
            response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(readContentFromClasspath("integration-data/response.json")));

    integrationServerClient
        .when(request().withMethod("PUT").withPath("/service-api/v2/arrangement"))
        .respond(
            response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(readContentFromClasspath("integration-data/arrangement-response.json")));
  }

  @BeforeEach
  void initializeTransactionServer() throws JsonProcessingException {
    transactionServer = startClientAndServer(TRANSACTION_SERVICE_PORT);
    transactionServerClient = new MockServerClient("localhost", TRANSACTION_SERVICE_PORT);
    transactionServerClient
        .when(request().withMethod("POST").withPath("/service-api/v2/ingest/pull"))
        .respond(
            response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(
                    objectMapper.writeValueAsString(
                        new TransactionIngestionResponse()
                            .withTransactions(
                                List.of(
                                    new TransactionsPostResponseBody()
                                        .withId("id")
                                        .withExternalId("externalId"))))));
  }

  @BeforeEach
  void initializePaymentOrderServer() throws JsonProcessingException {
    paymentOrderServer = startClientAndServer(PAYMENT_ORDER_SERVICE_PORT);
    paymentOrderServerClient = new MockServerClient("localhost", PAYMENT_ORDER_SERVICE_PORT);
    paymentOrderServerClient
        .when(request().withMethod("POST").withPath("/service-api/v2/ingest/pull"))
        .respond(
            response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(
                    objectMapper.writeValueAsString(
                        new PaymentOrderIngestionResponse()
                            .withNewPaymentOrder(
                                List.of(new PaymentOrderPostResponse().withId("id"))))));
  }

  @AfterEach
  void stopMockServer() {
    integrationServer.stop();
    while (!integrationServer.hasStopped(3, 100L, TimeUnit.MILLISECONDS)) {}
    transactionServer.stop();
    while (!transactionServer.hasStopped(3, 100L, TimeUnit.MILLISECONDS)) {}
    paymentOrderServer.stop();
    while (!paymentOrderServer.hasStopped(3, 100L, TimeUnit.MILLISECONDS)) {}
  }

  @Test
  void pullIngestProduct_Success() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node =
        mapper
            .readTree(readContentFromClasspath("integration-data/response.json"))
            .get("productGroups")
            .get(0);
    ProductGroup productGroup = mapper.treeToValue(node, ProductGroup.class);
    productGroup.setServiceAgreement(new ServiceAgreement().internalId("sa_internalId"));

    ProductGroupTask productGroupTask = new ProductGroupTask(productGroup);
    Mono<ProductGroupTask> productGroupTaskMono = Mono.just(productGroupTask);

    when(batchProductIngestionSaga.process(any(ProductGroupTask.class)))
        .thenReturn(productGroupTaskMono);

    when(batchProductIngestionSaga.process(any(BatchProductGroupTask.class)))
        .thenReturn(
            Mono.just(
                new BatchProductGroupTask()
                    .data(
                        new BatchProductGroup()
                            .productGroups(List.of(productGroup))
                            .serviceAgreement(productGroup.getServiceAgreement()))));

    ProductPullIngestionRequest pullIngestionRequest =
        new ProductPullIngestionRequest()
            .withLegalEntityExternalId("leId")
            .withServiceAgreementExternalId("saExId")
            .withServiceAgreementInternalId("saId")
            .withUserExternalId("userId")
            .withReferenceJobRoleNames(List.of("Admin Role"))
            .withMembershipAccounts(null)
            .withAdditions(Map.of());

    URI uri = URI.create("/service-api/v2/ingest/pull");
    WebTestClient webTestClient = WebTestClient.bindToController(productController).build();

    webTestClient
        .post()
        .uri(uri)
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .body(Mono.just(pullIngestionRequest), ProductPullIngestionRequest.class)
        .exchange()
        .expectStatus()
        .isCreated();
  }

  @Test
  void pushIngestProduct_Success() throws Exception {
    ProductPushIngestionRequest pushIngestionRequest =
        new ProductPushIngestionRequest()
            .withProductGroup(
                new com.backbase.stream.compositions.product.api.model.ProductGroup());
    URI uri = URI.create("/service-api/v2/ingest/push");
    WebTestClient webTestClient = WebTestClient.bindToController(productController).build();

    webTestClient
        .post()
        .uri(uri)
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .body(Mono.just(pushIngestionRequest), ProductPushIngestionRequest.class)
        .exchange()
        .expectStatus()
        .is5xxServerError();
  }

  @Test
  void pullIngestArrangement_Success() throws Exception {
    when(arrangementService.updateArrangement(any()))
        .thenReturn(Mono.just(new AccountArrangementItemPut()));

    ArrangementPullIngestionRequest pullIngestionRequest =
        new ArrangementPullIngestionRequest()
            .withInternalArrangementId("arrangementId")
            .withExternalArrangementId("externalArrangementId")
            .withAdditions(Map.of());

    URI uri = URI.create("/service-api/v2/ingest/arrangement/pull");
    WebTestClient webTestClient = WebTestClient.bindToController(productController).build();

    webTestClient
        .put()
        .uri(uri)
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .body(Mono.just(pullIngestionRequest), ProductPullIngestionRequest.class)
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void pullIngestArrangement_Fail() throws Exception {
    when(arrangementService.updateArrangement(any())).thenThrow(new RuntimeException());

    ArrangementPullIngestionRequest pullIngestionRequest =
        new ArrangementPullIngestionRequest()
            .withInternalArrangementId("arrangementId")
            .withExternalArrangementId("externalArrangementId")
            .withAdditions(Map.of());

    URI uri = URI.create("/service-api/v2/ingest/arrangement/pull");
    WebTestClient webTestClient = WebTestClient.bindToController(productController).build();

    webTestClient
        .put()
        .uri(uri)
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .body(Mono.just(pullIngestionRequest), ProductPullIngestionRequest.class)
        .exchange()
        .expectStatus()
        .is5xxServerError();
  }

  @Test
  void pushIngestArrangement_Success() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node =
        mapper
            .readTree(readContentFromClasspath("integration-data/arrangement-response.json"))
            .get("arrangement");
    com.backbase.stream.compositions.product.api.model.AccountArrangementItemPut
        arrangementItemPut =
            mapper.treeToValue(
                node,
                com.backbase.stream.compositions.product.api.model.AccountArrangementItemPut.class);

    when(arrangementService.updateArrangement(any()))
        .thenReturn(Mono.just(new AccountArrangementItemPut()));

    ArrangementPushIngestionRequest pushIngestionRequest =
        new ArrangementPushIngestionRequest()
            .withInternalArrangementId("arrangementId")
            .withArrangement(arrangementItemPut);

    URI uri = URI.create("/service-api/v2/ingest/arrangement/push");
    WebTestClient webTestClient = WebTestClient.bindToController(productController).build();

    webTestClient
        .put()
        .uri(uri)
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .body(Mono.just(pushIngestionRequest), ArrangementPushIngestionRequest.class)
        .exchange()
        .expectStatus()
        .isOk();
  }
}
