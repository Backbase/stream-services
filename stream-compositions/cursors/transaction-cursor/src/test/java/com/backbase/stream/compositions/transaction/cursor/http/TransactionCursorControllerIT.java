package com.backbase.stream.compositions.transaction.cursor.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.backbase.stream.compositions.transaction.cursor.core.service.TransactionCursorService;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursor;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorFilterRequest;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorPatchRequest;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorResponse;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorUpsertRequest;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorUpsertResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class TransactionCursorControllerIT {

    @Mock
    private TransactionCursorService transactionCursorService;

    private TransactionCursorController transactionCursorController;
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        transactionCursorController = new TransactionCursorController(transactionCursorService);
        webTestClient = WebTestClient.bindToController(transactionCursorController).build();
    }

    @Test
    void deleteCursor_Success() {
        String arrangementId = "4337f8cc-d66d-41b3-a00e-f71ff15d93cq";
        when(transactionCursorService.deleteByArrangementId(arrangementId))
                .thenReturn(Mono.just(ResponseEntity.ok().build()));

        webTestClient
                .method(HttpMethod.DELETE)
                .uri("/service-api/v2/cursor/arrangement/{arrangementId}", arrangementId)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getByArrangementId_Success() {
        String arrangementId = "4337f8cc-d66d-41b3-a00e-f71ff15d93cq";
        TransactionCursorResponse response = new TransactionCursorResponse()
                .cursor(new TransactionCursor().arrangementId(arrangementId).id("123"));
        when(transactionCursorService.findByArrangementId(arrangementId))
                .thenReturn(Mono.just(ResponseEntity.ok(response)));

        webTestClient.get().uri("/service-api/v2/cursor/arrangement/{arrangementId}", arrangementId)
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.cursor.id").isEqualTo("123")
                .jsonPath("$.cursor.arrangementId").isEqualTo(arrangementId);
    }

    @Test
    void getById_Success() {
        String id = "f2c7dcd7-2ed9-45af-8813-a5d630c5d804";
        TransactionCursorResponse response = new TransactionCursorResponse()
                .cursor(new TransactionCursor().arrangementId("123").id(id));
        when(transactionCursorService.findById(id))
                .thenReturn(Mono.just(ResponseEntity.ok(response)));

        webTestClient.get().uri("/service-api/v2/cursor/{id}", id)
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.cursor.id").isEqualTo(id);
    }

    @Test
    void patchByArrangementId_Success() {
        String arrangementId = "4337f8cc-d66d-41b3-a00e-f71ff15d93cq";
        TransactionCursorPatchRequest transactionCursorPatchRequest = new TransactionCursorPatchRequest()
                .lastTxnDate("2022-05-24 03:18:19")
                .status(TransactionCursor.StatusEnum.SUCCESS.getValue())
                .lastTxnIds("11,12,13,14");
        when(transactionCursorService.patchByArrangementId(anyString(), any()))
                .thenReturn(Mono.just(ResponseEntity.ok().build()));

        webTestClient
                .patch().uri("/service-api/v2/cursor/arrangement/{arrangementId}", arrangementId)
                .body(Mono.just(transactionCursorPatchRequest), TransactionCursorPatchRequest.class)
                .exchange().expectStatus().isOk();
    }

    @Test
    void filterCursor_Success() {
        TransactionCursorFilterRequest transactionCursorFilterRequest = new TransactionCursorFilterRequest()
                .lastTxnDate("2022-05-24 03:18:59")
                .status(TransactionCursor.StatusEnum.SUCCESS.getValue());
        TransactionCursorResponse response = new TransactionCursorResponse()
                .cursor(new TransactionCursor().id("123").arrangementId("arrangement-id"));
        when(transactionCursorService.filterCursor(any()))
                .thenReturn(Mono.just(ResponseEntity.ok(Flux.just(response))));

        webTestClient.post().uri("/service-api/v2/cursor/filter")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(transactionCursorFilterRequest), TransactionCursorFilterRequest.class)
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].cursor.id").isEqualTo("123");
    }

    @Test
    void upsertCursor_Success() {
        TransactionCursorUpsertRequest transactionCursorUpsertRequest =
                new TransactionCursorUpsertRequest().cursor(new TransactionCursor()
                        .arrangementId("4337f8cc-d66d-41b3-a00e-f71ff15d93cq")
                        .extArrangementId("5337f8cc-d66d-41b3-a00e-f71ff15d93cq")
                        .legalEntityId("beta-emp-ext")
                        .status(TransactionCursor.StatusEnum.IN_PROGRESS));
        TransactionCursorUpsertResponse response = new TransactionCursorUpsertResponse()
                .id("3337f8cc-d66d-41b3-a00e-f71ff15d93cq");
        when(transactionCursorService.upsertCursor(any()))
                .thenReturn(Mono.just(new ResponseEntity<>(response, HttpStatus.CREATED)));

        webTestClient.post().uri("/service-api/v2/cursor/upsert")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(transactionCursorUpsertRequest), TransactionCursorUpsertRequest.class)
                .exchange().expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo("3337f8cc-d66d-41b3-a00e-f71ff15d93cq");
    }
}
