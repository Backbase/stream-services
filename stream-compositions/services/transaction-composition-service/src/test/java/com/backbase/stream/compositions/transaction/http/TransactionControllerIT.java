package com.backbase.stream.compositions.transaction.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.backbase.dbs.transaction.api.service.v3.model.TransactionsPostResponseBody;
import com.backbase.stream.compositions.transaction.api.model.TransactionPullIngestionRequest;
import com.backbase.stream.compositions.transaction.api.model.TransactionPushIngestionRequest;
import com.backbase.stream.compositions.transaction.api.model.TransactionsPostRequestBody;
import com.backbase.stream.compositions.transaction.core.mapper.TransactionMapper;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestResponse;
import com.backbase.stream.compositions.transaction.core.service.TransactionIngestionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
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
class TransactionControllerIT {

    @Mock
    private TransactionIngestionService transactionIngestionService;

    private TransactionController transactionController;

    @BeforeEach
    void setUp() {
        TransactionMapper transactionMapper = Mappers.getMapper(TransactionMapper.class);
        transactionController = new TransactionController(transactionIngestionService, transactionMapper);
    }

    @Test
    void pullIngestTransactions_Success() throws Exception {

        URI uri = URI.create("/service-api/v2/ingest/pull");
        WebTestClient webTestClient = WebTestClient.bindToController(transactionController).build();

        List<TransactionsPostResponseBody> transactionsPostResponses = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .readValue(readContentFromClasspath("integration-data/response.json"), new TypeReference<>() {});
        when(transactionIngestionService.ingestPull(any())).thenReturn(
                Mono.just(TransactionIngestResponse.builder()
                        .arrangementId("4337f8cc-d66d-41b3-a00e-f71ff15d93cg")
                        .transactions(transactionsPostResponses)
                        .additions(Map.of())
                        .build()));

        TransactionPullIngestionRequest pullIngestionRequest =
                new TransactionPullIngestionRequest()
                        .arrangementId("4337f8cc-d66d-41b3-a00e-f71ff15d93cg")
                        .billingCycles(3)
                        .externalArrangementId("externalArrangementId")
                        .legalEntityInternalId("leInternalId");
        webTestClient.post().uri(uri)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(Mono.just(pullIngestionRequest), TransactionPullIngestionRequest.class).exchange()
                .expectStatus().isCreated();
    }

    @Test
    void pushIngestTransactions_Success() {

        URI uri = URI.create("/service-api/v2/ingest/push");
        WebTestClient webTestClient = WebTestClient.bindToController(transactionController).build();


        TransactionPushIngestionRequest pushIngestionRequest =
                new TransactionPushIngestionRequest()
                        .transactions(List.of(new TransactionsPostRequestBody().type("type1").
                                arrangementId("1234").reference("ref")
                                .externalArrangementId("externalArrId")));

        webTestClient.post().uri(uri)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(Mono.just(pushIngestionRequest), TransactionPushIngestionRequest.class).exchange()
                .expectStatus().is4xxClientError();
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
