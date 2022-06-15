package com.backbase.stream.compositions.transaction.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostResponseBody;
import com.backbase.stream.TransactionService;
import com.backbase.stream.compositions.transaction.api.model.TransactionPullIngestionRequest;
import com.backbase.stream.compositions.transaction.api.model.TransactionPushIngestionRequest;
import com.backbase.stream.compositions.transaction.api.model.TransactionsPostRequestBody;
import com.backbase.stream.compositions.transaction.cursor.client.model.TransactionCursor;
import com.backbase.stream.compositions.transaction.cursor.client.model.TransactionCursorResponse;
import com.backbase.stream.transaction.TransactionTask;
import com.backbase.stream.worker.model.UnitOfWork;
import com.backbase.streams.compositions.test.IntegrationTest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@DirtiesContext
@SpringBootTest
@AutoConfigureWebTestClient
@ExtendWith({SpringExtension.class})
@Slf4j
class TransactionControllerIT extends IntegrationTest {

  private static final int TOKEN_CONVERTER_PORT = 10000;
  private static final int INTEGRATION_SERVICE_PORT = 18000;
  private static final int TRANSACTION_CURSOR_SERVICE_PORT = 12000;
  private ClientAndServer integrationServer;
  private ClientAndServer tokenConverterServer;
  private ClientAndServer transactionCursorServer;
  private MockServerClient integrationServerClient;
  private MockServerClient tokenConverterServerClient;
  private MockServerClient transactionCursorServerClient;
  private static BrokerService broker;

  @Autowired
  TransactionController transactionController;

  @Autowired
  ObjectMapper objectMapper;

  @MockBean
  TransactionService transactionService;

  @MockBean
  TransactionTask transactionTask;

  static {
    System.setProperty("spring.application.name", "transaction-composition-service");
  }

  @BeforeAll
  static void initActiveMqBroker() throws Exception {
    broker = new BrokerService();
    broker.setBrokerName("activemq");
    broker.setPersistent(false);
    broker.start();
    broker.waitUntilStarted();
  }

  @BeforeEach
  void initializeTokenConverterServer() throws IOException {
    tokenConverterServer = startClientAndServer(TOKEN_CONVERTER_PORT);
    tokenConverterServerClient = new MockServerClient("localhost", TOKEN_CONVERTER_PORT);
    tokenConverterServerClient.when(
        request()
            .withMethod("POST")
            .withPath("/oauth/token"))
        .respond(
            response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(readContentFromClasspath("token-converter-data/token.json"))
        );
  }

  @BeforeEach
  void initializeIntegrationServer() throws IOException {
    integrationServer = startClientAndServer(INTEGRATION_SERVICE_PORT);
    integrationServerClient = new MockServerClient("localhost", INTEGRATION_SERVICE_PORT);
    integrationServerClient.when(
        request()
            .withMethod("POST")
            .withPath("/integration-api/v2/transactions"))
        .respond(
            response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(readContentFromClasspath("integration-data/response.json"))
        );
  }

  @BeforeEach
  void initializeTransactionCursorServer() throws JsonProcessingException {
    transactionCursorServer = startClientAndServer(TRANSACTION_CURSOR_SERVICE_PORT);
    transactionCursorServerClient = new MockServerClient("localhost",
        TRANSACTION_CURSOR_SERVICE_PORT);
    transactionCursorServerClient.when(
        request()
            .withMethod("GET")
            .withPath("/service-api/v2/cursor/arrangement/4337f8cc-d66d-41b3-a00e-f71ff15d93cg"))
        .respond(
            response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(
                    objectMapper.writeValueAsString(new TransactionCursorResponse()
                        .withCursor(new TransactionCursor().withId("1")
                            .withArrangementId("4337f8cc-d66d-41b3-a00e-f71ff15d93cg"))
                    )
                ));

    transactionCursorServerClient.when(
        request()
            .withMethod("PATCH")
            .withPath("/service-api/v2/cursor/arrangement/4337f8cc-d66d-41b3-a00e-f71ff15d93cg"))
        .respond(
            response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON));

  }

  @AfterEach
  void stopMockServer() {
    tokenConverterServer.stop();
    while (!tokenConverterServer.hasStopped(3, 100L, TimeUnit.MILLISECONDS)) {
    }
    integrationServer.stop();
    while (!integrationServer.hasStopped(3, 100L, TimeUnit.MILLISECONDS)) {
    }
    transactionCursorServer.stop();
    while (!transactionCursorServer.hasStopped(3, 100L, TimeUnit.MILLISECONDS)) {
    }
  }

  @Test
  void pullIngestTransactions_Success() throws Exception {

    URI uri = URI.create("/service-api/v2/ingest/pull");
    WebTestClient webTestClient = WebTestClient.bindToController(transactionController).build();

    List<TransactionsPostResponseBody> transactionsPostResponses = new Gson()
        .fromJson(readContentFromClasspath("integration-data/response.json"), ArrayList.class);
    when(transactionService.processTransactions(any())).thenReturn(Flux.just(new UnitOfWork<>()));
    when(transactionTask.getResponse()).thenReturn(transactionsPostResponses);
    TransactionPullIngestionRequest pullIngestionRequest =
        new TransactionPullIngestionRequest()
            .withArrangementId("4337f8cc-d66d-41b3-a00e-f71ff15d93cg")
            .withBillingCycles(3)
            .withExternalArrangementId("externalArrangementId")
            .withLegalEntityInternalId("leInternalId");
    webTestClient.post().uri(uri)
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .body(Mono.just(pullIngestionRequest), TransactionPullIngestionRequest.class).exchange()
        .expectStatus().isOk();
  }

  @Test
  void pushIngestTransactions_Success() throws Exception {

    URI uri = URI.create("/service-api/v2/ingest/push");
    WebTestClient webTestClient = WebTestClient.bindToController(transactionController).build();

    TransactionPushIngestionRequest pushIngestionRequest =
        new TransactionPushIngestionRequest()
            .withTransactions(List.of(new TransactionsPostRequestBody().withType("type1").
                withArrangementId("1234").withReference("ref")
                .withExternalArrangementId("externalArrId")));

    webTestClient.post().uri(uri)
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .body(Mono.just(pushIngestionRequest), TransactionPushIngestionRequest.class).exchange()
        .expectStatus().is4xxClientError();
  }
}
