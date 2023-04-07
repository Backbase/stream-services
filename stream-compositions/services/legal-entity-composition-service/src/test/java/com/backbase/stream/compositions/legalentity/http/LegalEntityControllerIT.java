package com.backbase.stream.compositions.legalentity.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.backbase.stream.LegalEntitySaga;
import com.backbase.stream.LegalEntityTask;
import com.backbase.stream.compositions.legalentity.api.model.LegalEntityPullIngestionRequest;
import com.backbase.stream.compositions.legalentity.api.model.LegalEntityPushIngestionRequest;
import com.backbase.stream.compositions.legalentity.core.config.LegalEntityConfiguration;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.streams.compositions.test.IntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.activemq.broker.BrokerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
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

import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@DirtiesContext
@AutoConfigureWebTestClient
@ExtendWith({SpringExtension.class})
class LegalEntityControllerIT extends IntegrationTest {

    private static final int INTEGRATION_SERVICE_PORT = 18000;
    private ClientAndServer integrationServer;
    private MockServerClient integrationServerClient;
    private static BrokerService broker;

    @Autowired LegalEntityConfiguration legalEntityConfiguration;

    @Autowired LegalEntityController legalEntityController;

    @MockBean private LegalEntitySaga legalEntitySaga;

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
                .when(request().withMethod("POST").withPath("/service-api/v2/legal-entity"))
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
        while (!integrationServer.hasStopped(5, 100L, TimeUnit.MILLISECONDS)) {}
    }

    @Test
    void pullIngestLegalEntity_Success() throws Exception {
        LegalEntity legalEntity =
                new ObjectMapper()
                        .readValue(
                                readContentFromClasspath("integration-data/legal-entity.json"),
                                LegalEntity.class);

        when(legalEntitySaga.executeTask(any()))
                .thenReturn(Mono.just(new LegalEntityTask(legalEntity)));

        URI uri = URI.create("/service-api/v2/ingest/pull");
        LegalEntityPullIngestionRequest pullIngestionRequest =
                new LegalEntityPullIngestionRequest().withLegalEntityExternalId("externalId");

        WebTestClient webTestClient = WebTestClient.bindToController(legalEntityController).build();
        webTestClient
                .post()
                .uri(uri)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(Mono.just(pullIngestionRequest), LegalEntityPullIngestionRequest.class)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectHeader()
                .valueEquals("Content-Type", "application/json")
                .expectBody()
                .jsonPath("$.legalEntity.name")
                .isEqualTo("Test Legal Entity");
    }

    @Test
    void pushIngestLegalEntity_Fail() {
        URI uri = URI.create("/service-api/v2/ingest/push");

        LegalEntityPushIngestionRequest pushIngestionRequest =
                Mockito.mock(LegalEntityPushIngestionRequest.class);

        WebTestClient webTestClient = WebTestClient.bindToController(legalEntityController).build();
        webTestClient
                .post()
                .uri(uri)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(Mono.just(pushIngestionRequest), LegalEntityPushIngestionRequest.class)
                .exchange()
                .expectStatus()
                .is5xxServerError();
    }
}
