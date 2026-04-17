package com.backbase.stream.compositions.product.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementPutItem;
import com.backbase.stream.compositions.product.api.model.ArrangementPullIngestionRequest;
import com.backbase.stream.compositions.product.api.model.ArrangementPushIngestionRequest;
import com.backbase.stream.compositions.product.api.model.ProductPullIngestionRequest;
import com.backbase.stream.compositions.product.api.model.ProductPushIngestionRequest;
import com.backbase.stream.compositions.product.core.mapper.ArrangementMapper;
import com.backbase.stream.compositions.product.core.mapper.ArrangementRestMapper;
import com.backbase.stream.compositions.product.core.mapper.ConfigMapper;
import com.backbase.stream.compositions.product.core.mapper.ProductGroupMapper;
import com.backbase.stream.compositions.product.core.mapper.ProductRestMapper;
import com.backbase.stream.compositions.product.core.model.ArrangementIngestResponse;
import com.backbase.stream.compositions.product.core.model.ProductIngestResponse;
import com.backbase.stream.compositions.product.core.service.ArrangementIngestionService;
import com.backbase.stream.compositions.product.core.service.ProductIngestionService;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ProductControllerIT {

    @Mock
    private ProductIngestionService productIngestionService;
    @Mock
    private ArrangementIngestionService arrangementIngestionService;

    private ProductController productController;

    @BeforeEach
    void setUp() {
        ProductGroupMapper productGroupMapper = Mappers.getMapper(ProductGroupMapper.class);
        ArrangementMapper arrangementMapper = Mappers.getMapper(ArrangementMapper.class);
        ProductRestMapper productRestMapper = new ProductRestMapper(productGroupMapper);
        ArrangementRestMapper arrangementRestMapper = new ArrangementRestMapper(arrangementMapper, new ConfigMapper());
        ProductSubController productSubController = new ProductSubController(productIngestionService, productRestMapper);
        ArrangementSubController arrangementSubController =
                new ArrangementSubController(arrangementIngestionService, arrangementRestMapper);
        productController = new ProductController(productSubController, arrangementSubController);
    }

    @Test
    void pullIngestProduct_Success() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(readContentFromClasspath("integration-data/response.json"))
                .get("productGroups").get(0);
        ProductGroup productGroup = mapper.treeToValue(node, ProductGroup.class);
        productGroup.setServiceAgreement(new ServiceAgreement().internalId("sa_internalId"));


        when(productIngestionService.ingestPull(any()))
                .thenReturn(Mono.just(ProductIngestResponse.builder()
                        .serviceAgreementInternalId("sa_internalId")
                        .productGroups(List.of(productGroup))
                        .additions(Map.of())
                        .build()));

        ProductPullIngestionRequest pullIngestionRequest =
                new ProductPullIngestionRequest()
                        .legalEntityExternalId("leId")
                        .serviceAgreementExternalId("saExId")
                        .serviceAgreementInternalId("saId")
                        .userExternalId("userId")
                        .referenceJobRoleNames(List.of("Admin Role"))
                        .membershipAccounts(null)
                        .additions(Map.of());

        URI uri = URI.create("/service-api/v2/ingest/pull");
        WebTestClient webTestClient = WebTestClient.bindToController(productController).build();

        webTestClient.post().uri(uri)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(Mono.just(pullIngestionRequest), ProductPullIngestionRequest.class).exchange()
                .expectStatus().isCreated();
    }

    @Test
    void pushIngestProduct_Success() {
        when(productIngestionService.ingestPush(any()))
                .thenReturn(Mono.error(new RuntimeException("push failed")));

        ProductPushIngestionRequest pushIngestionRequest = new ProductPushIngestionRequest()
                .productGroup(new com.backbase.stream.compositions.product.api.model.ProductGroup());
        URI uri = URI.create("/service-api/v2/ingest/push");
        WebTestClient webTestClient = WebTestClient.bindToController(productController).build();

        webTestClient.post().uri(uri)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(Mono.just(pushIngestionRequest), ProductPushIngestionRequest.class).exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void pullIngestArrangement_Success() {
        when(arrangementIngestionService.ingestPull(any()))
                .thenReturn(Mono.just(ArrangementIngestResponse.builder()
                        .arrangement(new ArrangementPutItem())
                        .build()));

        ArrangementPullIngestionRequest pullIngestionRequest =
                new ArrangementPullIngestionRequest()
                        .internalArrangementId("arrangementId")
                        .externalArrangementId("externalArrangementId")
                        .additions(Map.of());

        URI uri = URI.create("/service-api/v2/ingest/arrangement/pull");
        WebTestClient webTestClient = WebTestClient.bindToController(productController).build();

        webTestClient.put().uri(uri)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(Mono.just(pullIngestionRequest), ProductPullIngestionRequest.class).exchange()
                .expectStatus().isOk();
    }

    @Test
    void pullIngestArrangement_Fail() {
        when(arrangementIngestionService.ingestPull(any()))
                .thenReturn(Mono.error(new RuntimeException("pull failed")));

        ArrangementPullIngestionRequest pullIngestionRequest =
                new ArrangementPullIngestionRequest()
                        .internalArrangementId("arrangementId")
                        .externalArrangementId("externalArrangementId")
                        .additions(Map.of());

        URI uri = URI.create("/service-api/v2/ingest/arrangement/pull");
        WebTestClient webTestClient = WebTestClient.bindToController(productController).build();

        webTestClient.put().uri(uri)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(Mono.just(pullIngestionRequest), ProductPullIngestionRequest.class).exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void pushIngestArrangement_Success() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(readContentFromClasspath("integration-data/arrangement-response.json"))
                .get("arrangement");
        com.backbase.stream.compositions.product.api.model.AccountArrangementItemPut arrangementItemPut =
                mapper.treeToValue(node, com.backbase.stream.compositions.product.api.model.AccountArrangementItemPut.class);

        when(arrangementIngestionService.ingestPush(any()))
                .thenReturn(Mono.just(ArrangementIngestResponse.builder()
                        .arrangement(new ArrangementPutItem())
                        .build()));

        ArrangementPushIngestionRequest pushIngestionRequest =
                new ArrangementPushIngestionRequest()
                        .internalArrangementId("arrangementId")
                        .arrangement(arrangementItemPut);

        URI uri = URI.create("/service-api/v2/ingest/arrangement/push");
        WebTestClient webTestClient = WebTestClient.bindToController(productController).build();

        webTestClient.put().uri(uri)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(Mono.just(pushIngestionRequest), ArrangementPushIngestionRequest.class).exchange()
                .expectStatus().isOk();
    }

    private String readContentFromClasspath(String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
