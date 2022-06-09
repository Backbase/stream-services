package com.backbase.stream.compositions.transaction.cursor.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.backbase.stream.compositions.transaction.cursor.core.config.TransactionCursorConfiguration;
import com.backbase.stream.compositions.transaction.cursor.core.domain.TransactionCursorEntity;
import com.backbase.stream.compositions.transaction.cursor.core.mapper.TransactionCursorMapper;
import com.backbase.stream.compositions.transaction.cursor.core.repository.TransactionCursorRepository;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursor.StatusEnum;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorDeleteRequest;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorPatchRequest;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorUpsertRequest;
import java.text.ParseException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@DirtiesContext
@ExtendWith({SpringExtension.class})
@ActiveProfiles("local")
@WebFluxTest(controllers = TransactionCursorController.class,
    excludeAutoConfiguration = {TransactionCursorConfiguration.class})
public class TransactionCursorControllerIT {

  @Autowired
  TransactionCursorController transactionCursorController;

  @MockBean
  TransactionCursorRepository transactionCursorRepository;

  @MockBean
  WebClient webClient;

  @Autowired
  private WebTestClient webTestClient;

  @Autowired
  TransactionCursorMapper mapper;

  @Test
  void deleteCursor_Success() {
    TransactionCursorDeleteRequest transactionCursorDeleteRequest =
        new TransactionCursorDeleteRequest().withId("f2c7dcd7-2ed9-45af-8813-a5d630c5d804")
            .withArrangementId("4337f8cc-d66d-41b3-a00e-f71ff15d93cq");

    webTestClient
        .method(HttpMethod.DELETE)
        .uri("/service-api/v2/cursor/delete")
        .body(Mono.just(transactionCursorDeleteRequest), TransactionCursorDeleteRequest.class)
        .exchange()
        .expectStatus().isOk();
    Mockito.verify(transactionCursorRepository, Mockito.times(1))
        .deleteCursor(transactionCursorDeleteRequest);
  }

  @Test
  void getByArrangementId_Success() {
    String arrangementId = "4337f8cc-d66d-41b3-a00e-f71ff15d93cq";

    TransactionCursorEntity transactionCursorEntity = new TransactionCursorEntity();
    transactionCursorEntity.setArrangement_id(arrangementId);
    transactionCursorEntity.setId("123");
    when(transactionCursorRepository.findByArrangementId(arrangementId))
        .thenReturn(Optional.of(transactionCursorEntity));

    webTestClient.get().uri("/service-api/v2/cursor/arrangement/{arrangementId}", arrangementId)
        .exchange().expectStatus().isOk()
        .expectBody()
        .jsonPath("$.cursor.id").isNotEmpty();
    Mockito.verify(transactionCursorRepository, Mockito.times(1))
        .findByArrangementId(arrangementId);
  }

  @Test
  void getById_Success() {
    String id = "f2c7dcd7-2ed9-45af-8813-a5d630c5d804";

    TransactionCursorEntity transactionCursorEntity = new TransactionCursorEntity();
    transactionCursorEntity.setArrangement_id("123");
    transactionCursorEntity.setId(id);
    when(transactionCursorRepository.findById(id))
        .thenReturn(Optional.of(transactionCursorEntity));

    webTestClient.get().uri("/service-api/v2/cursor/{id}", id)
        .exchange().expectStatus().isOk()
        .expectBody()
        .jsonPath("$.cursor.id").isNotEmpty();
    Mockito.verify(transactionCursorRepository, Mockito.times(1))
        .findById(id);
  }

  @Test
  void patchByArrangementId_Success() throws ParseException {
    String arrangementId = "4337f8cc-d66d-41b3-a00e-f71ff15d93cq";

    TransactionCursorPatchRequest transactionCursorPatchRequest = new TransactionCursorPatchRequest()
        .withLastTxnDate("2022-05-24 03:18:19")
        .withStatus(StatusEnum.SUCCESS.getValue())
        .withLastTxnIds("11,12,13,14");

    webTestClient
        .patch().uri("/service-api/v2/cursor/arrangement/{arrangementId}", arrangementId)
        .body(Mono.just(transactionCursorPatchRequest), TransactionCursorPatchRequest.class)
        .exchange().expectStatus().isOk();
    Mockito.verify(transactionCursorRepository, Mockito.times(1))
        .patchByArrangementId(arrangementId, transactionCursorPatchRequest);
  }

  // @Test
  void upsertCursor_Success() {
    webTestClient.post().uri("/service-api/v2/cursor/upsert")
        .contentType(MediaType.APPLICATION_JSON)
        .body(Mono.just(any()), TransactionCursorUpsertRequest.class)
        .exchange().expectStatus().isOk();
  }

}
