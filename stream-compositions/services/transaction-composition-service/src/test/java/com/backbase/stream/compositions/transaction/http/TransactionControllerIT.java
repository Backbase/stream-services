package com.backbase.stream.compositions.transaction.http;

import com.backbase.stream.TransactionService;
import com.backbase.stream.compositions.transaction.TransactionCompositionApplication;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestResponse;
import com.backbase.stream.compositions.transaction.model.PullIngestionRequest;
import com.backbase.streams.compositions.test.IntegrationTest;
import com.google.gson.Gson;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@DirtiesContext
@SpringBootTest(classes = {TransactionCompositionApplication.class})
@AutoConfigureWebTestClient
@ExtendWith({SpringExtension.class})
class TransactionControllerIT extends IntegrationTest {
    private static final int TOKEN_CONVERTER_PORT = 10000;
    private static final int INTEGRATION_SERVICE_PORT = 18000;
    private ClientAndServer integrationServer;
    private ClientAndServer tokenConverterServer;
    private MockServerClient integrationServerClient;
    private MockServerClient tokenConverterServerClient;
    private static BrokerService broker;

    @MockBean
    TransactionService transactionService;

    @Autowired
    com.backbase.stream.compositions.transaction.http.TransactionController transactionController;

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
                                .withMethod("GET")
                                .withPath("/integration-api/v2/product-group"))
                .respond(
                        response()
                                .withStatusCode(200)
                                .withContentType(MediaType.APPLICATION_JSON)
                                .withBody(readContentFromClasspath("integration-data/response.json"))
                );
    }

    @AfterEach
    void stopMockServer() {
        tokenConverterServer.stop();
        integrationServer.stop();
    }

    @Test
    void pullIngestLegalEntity_Success() throws Exception {
        TransactionIngestResponse[] transactions = new Gson()
                .fromJson(readContentFromClasspath("integration-data/response.json"), TransactionIngestResponse[].class);

        when(transactionService.processTransactions(any(Flux.class)))
                .thenReturn(Flux.empty());

        PullIngestionRequest pullIngestionRequest =
                new PullIngestionRequest()
                        .withExternalArrangementIds("id1");

        URI uri = URI.create("/service-api/v2/pull-ingestion");
        WebTestClient webTestClient = WebTestClient.bindToController(transactionController).build();

        webTestClient.post().uri(uri).body(Mono.just(pullIngestionRequest), PullIngestionRequest.class).exchange().expectStatus().isCreated();
    }
}