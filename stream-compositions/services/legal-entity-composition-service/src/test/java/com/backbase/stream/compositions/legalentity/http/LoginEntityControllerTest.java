package com.backbase.stream.compositions.legalentity.http;

import com.backbase.stream.LegalEntitySaga;
import com.backbase.stream.LegalEntityTask;
import com.backbase.stream.compositions.legalentity.model.PullIngestionRequest;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.streams.compositions.test.IntegrationTest;
import com.google.gson.Gson;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
@AutoConfigureWebTestClient
@ExtendWith(SpringExtension.class)
class LoginEntityControllerTest extends IntegrationTest {
    private static final int TOKEN_CONVERTER_PORT = 10000;
    private static final int INTEGRATION_SERVICE_PORT = 18000;
    private ClientAndServer integrationServer;
    private ClientAndServer tokenConverterServer;
    private MockServerClient integrationServerClient;
    private MockServerClient tokenConverterServerClient;

    @Autowired
    LegalEntityController legalEntityController;

    @MockBean
    private LegalEntitySaga legalEntitySaga;


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
                        .withPath("/integration-api/v2/legal-entity"))
                .respond(
                        response()
                                .withStatusCode(200)
                                .withContentType(MediaType.APPLICATION_JSON)
                                .withBody(readContentFromClasspath("integration-data/response.json"))
                );
    }

    @AfterEach
    void stopMockServesr() {
        tokenConverterServer.stop();
        integrationServer.stop();
    }

    @Test
    public void pullIngestLegalEntity_Success() throws Exception {
        LegalEntity[] legalEntity = new Gson()
                .fromJson(readContentFromClasspath("integration-data/response.json"), LegalEntity[].class);

        when(legalEntitySaga.executeTask(any()))
                .thenReturn(Mono.just(new LegalEntityTask(legalEntity[0])));

        URI uri = URI.create("/service-api/v2/pull-ingestion");

        PullIngestionRequest pullIngestionRequest =
                new PullIngestionRequest().withLegalEntityExternalId("externalId");

        WebTestClient webTestClient = WebTestClient.bindToController(legalEntityController).build();

        webTestClient.post().uri(uri).body(Mono.just(pullIngestionRequest), PullIngestionRequest.class).exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Content-Type", "application/json")
                .expectBody().jsonPath("$.legalEntities[0].name").isEqualTo("Test Legal Entity");
    }
}

