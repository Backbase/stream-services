package com.backbase.stream.compositions.productcatalog.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.backbase.stream.compositions.productcatalog.core.service.ProductCatalogIngestionService;
import com.backbase.stream.compositions.productcatalog.mapper.ProductCatalogMapper;
import com.backbase.stream.compositions.productcatalog.model.ProductCatalogPushIngestionRequest;
import com.backbase.stream.productcatalog.model.ProductCatalog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ProductCatalogControllerIT {

    @Mock
    private ProductCatalogIngestionService productCatalogIngestionService;

    private ProductCatalogController productCatalogController;

    @BeforeEach
    void setUp() {
        ProductCatalogMapper mapper = Mappers.getMapper(ProductCatalogMapper.class);
        productCatalogController = new ProductCatalogController(productCatalogIngestionService, mapper);
    }

    @Test
    void pullIngestLegalEntity_Success() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(readContentFromClasspath("integration-data/response.json"))
            .get("productCatalog");
        ProductCatalog productCatalog = mapper.treeToValue(node, ProductCatalog.class);

        when(productCatalogIngestionService.ingestPull(any()))
                .thenReturn(Mono.just(com.backbase.stream.compositions.productcatalog.core.model.ProductCatalogIngestResponse
                        .builder()
                        .productCatalog(productCatalog)
                        .build()));

        ProductCatalogPushIngestionRequest request = new ProductCatalogPushIngestionRequest();
        URI uri = URI.create("/service-api/v2/pull-ingestion");
        WebTestClient webTestClient = WebTestClient.bindToController(productCatalogController).build();

        webTestClient.post().uri(uri).body(Mono.just(request), ProductCatalogPushIngestionRequest.class).exchange().expectStatus().isCreated();
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
