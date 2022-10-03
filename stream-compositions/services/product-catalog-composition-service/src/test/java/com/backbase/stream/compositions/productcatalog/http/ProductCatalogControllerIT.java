package com.backbase.stream.compositions.productcatalog.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.backbase.stream.compositions.productcatalog.ProductCatalogCompositionApplication;
import com.backbase.stream.compositions.productcatalog.model.ProductCatalogPushIngestionRequest;
import com.backbase.stream.productcatalog.ReactiveProductCatalogService;
import com.backbase.stream.productcatalog.model.ProductCatalog;
import com.backbase.streams.compositions.test.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
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

@DirtiesContext
@SpringBootTest(classes = {ProductCatalogCompositionApplication.class})
@AutoConfigureWebTestClient
@ExtendWith({SpringExtension.class})
class ProductCatalogControllerIT extends IntegrationTest {
    private static final int INTEGRATION_SERVICE_PORT = 18000;
    private ClientAndServer integrationServer;
    private MockServerClient integrationServerClient;
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
    void initializeIntegrationServer() throws IOException {
        integrationServer = startClientAndServer(INTEGRATION_SERVICE_PORT);
        integrationServerClient = new MockServerClient("localhost", INTEGRATION_SERVICE_PORT);
        integrationServerClient.when(
                request()
                        .withMethod("GET")
                        .withPath("/service-api/v2/product-catalog"))
                .respond(
                        response()
                                .withStatusCode(200)
                                .withContentType(MediaType.APPLICATION_JSON)
                                .withBody(readContentFromClasspath("integration-data/response.json"))
                );
    }

    @AfterEach
    void stopMockServer() {
        integrationServer.stop();
    }

    @Test
    void pullIngestLegalEntity_Success() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(readContentFromClasspath("integration-data/response.json"))
            .get("productCatalog");
        ProductCatalog productCatalog = mapper.treeToValue(node, ProductCatalog.class);

        when(reactiveProductCatalogService.setupProductCatalog(any()))
                .thenReturn(Mono.just(productCatalog));

        when(reactiveProductCatalogService.updateExistingProductCatalog(any()))
                .thenReturn(Mono.just(productCatalog));

        when(reactiveProductCatalogService.upsertProductCatalog(any()))
                .thenReturn(Mono.just(productCatalog));

        ProductCatalogPushIngestionRequest request = new ProductCatalogPushIngestionRequest();
        URI uri = URI.create("/service-api/v2/pull-ingestion");
        WebTestClient webTestClient = WebTestClient.bindToController(productCatalogController).build();

        webTestClient.post().uri(uri).body(Mono.just(request), ProductCatalogPushIngestionRequest.class).exchange().expectStatus().isCreated();
    }
}
