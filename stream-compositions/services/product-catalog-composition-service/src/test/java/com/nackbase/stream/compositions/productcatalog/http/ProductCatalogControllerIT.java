package com.nackbase.stream.compositions.productcatalog.http;

import com.backbase.stream.compositions.productcatalog.ProductCatalogCompositionApplication;
import com.backbase.stream.compositions.productcatalog.http.ProductCatalogController;
import com.backbase.stream.productcatalog.ReactiveProductCatalogService;
import com.backbase.stream.productcatalog.model.ProductCatalog;
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
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@DirtiesContext
@SpringBootTest(classes = {ProductCatalogCompositionApplication.class})
@AutoConfigureWebTestClient
@ExtendWith({SpringExtension.class})
class ProductCatalogControllerIT extends IntegrationTest {
    private static final int TOKEN_CONVERTER_PORT = 10000;
    private static final int INTEGRATION_SERVICE_PORT = 18000;
    private ClientAndServer integrationServer;
    private ClientAndServer tokenConverterServer;
    private MockServerClient integrationServerClient;
    private MockServerClient tokenConverterServerClient;
    private static BrokerService broker;

    @MockBean
    ReactiveProductCatalogService reactiveProductCatalogService;

    @Autowired
    ProductCatalogController productCatalogController;

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
                        .withPath("/integration-api/v2/product-catalog"))
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
        ProductCatalog productCatalog = new Gson()
                .fromJson(readContentFromClasspath("integration-data/response.json"), ProductCatalog.class);

        when(reactiveProductCatalogService.setupProductCatalog(any()))
                .thenReturn(Mono.just(productCatalog));

        URI uri = URI.create("/service-api/v2/pull-ingestion");
        WebTestClient webTestClient = WebTestClient.bindToController(productCatalogController).build();

        webTestClient.post().uri(uri).exchange().expectStatus().isOk();
    }
}
