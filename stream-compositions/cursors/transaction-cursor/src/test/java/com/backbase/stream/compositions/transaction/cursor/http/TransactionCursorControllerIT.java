package com.backbase.stream.compositions.transaction.cursor.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.backbase.stream.compositions.transaction.cursor.core.config.TransactionCursorConfiguration;
import com.backbase.stream.compositions.transaction.cursor.core.domain.TransactionCursorEntity;
import com.backbase.stream.compositions.transaction.cursor.core.mapper.TransactionCursorMapper;
import com.backbase.stream.compositions.transaction.cursor.core.repository.TransactionCursorRepository;
import com.backbase.stream.compositions.transaction.cursor.core.repository.TransactionCursorRepositoryImpl;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursor;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursor.StatusEnum;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorFilterRequest;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorPatchRequest;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorUpsertRequest;
import java.util.Optional;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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
  TransactionCursorRepositoryImpl transactionCursorRepositoryImpl;

  @MockBean
  WebClient webClient;

  @Autowired
  private WebTestClient webTestClient;

  @Autowired
  TransactionCursorMapper mapper;

  @MockBean
  EntityManager entityManager;


  @Test
  void deleteCursor_Success() {
    String arrangementId = "4337f8cc-d66d-41b3-a00e-f71ff15d93cq";

    webTestClient
        .method(HttpMethod.DELETE)
        .uri("/service-api/v2/cursor/arrangement/{arrangementId}", arrangementId)
        .exchange()
        .expectStatus().isOk();
    Mockito.verify(transactionCursorRepository, Mockito.times(1))
        .deleteByArrangementId(arrangementId);
  }

  @Test
  void getByArrangementId_Success() {
    String arrangementId = "4337f8cc-d66d-41b3-a00e-f71ff15d93cq";

    TransactionCursorEntity transactionCursorEntity = new TransactionCursorEntity();
    transactionCursorEntity.setArrangementId(arrangementId);
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
    transactionCursorEntity.setArrangementId("123");
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
  void patchByArrangementId_Success() {
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

  @Test
  void filterCursor_Success() {
    TransactionCursorFilterRequest transactionCursorFilterRequest = new TransactionCursorFilterRequest()
        .withLastTxnDate("2022-05-24 03:18:59").withStatus(StatusEnum.SUCCESS.getValue());
    webTestClient.post().uri("/service-api/v2/cursor/filter")
        .contentType(MediaType.APPLICATION_JSON)
        .body(Mono.just(transactionCursorFilterRequest), TransactionCursorFilterRequest.class)
        .exchange().expectStatus().isOk();
  }

  // @Test
  void upsertCursor_Success() {
    TransactionCursorUpsertRequest transactionCursorUpsertRequest =
        new TransactionCursorUpsertRequest().withCursor(new TransactionCursor()
            .withArrangementId("4337f8cc-d66d-41b3-a00e-f71ff15d93cq")
            .withExtArrangementId("5337f8cc-d66d-41b3-a00e-f71ff15d93cq")
            .withLegalEntityId("beta-emp-ext")
            .withStatus(StatusEnum.IN_PROGRESS));
    TransactionCursorEntity transactionCursorEntity = new TransactionCursorEntity();
    transactionCursorEntity.setId("3337f8cc-d66d-41b3-a00e-f71ff15d93cq");

    //when(transactionCursorRepository.save(transactionCursorEntity)).thenReturn(transactionCursorEntity);
    webTestClient.post().uri("/service-api/v2/cursor/upsert")
        .contentType(MediaType.APPLICATION_JSON)
        .body(Mono.just(transactionCursorUpsertRequest), TransactionCursorUpsertRequest.class)
        .exchange().expectStatus().isOk();

    StepVerifier.create(transactionCursorRepository.save(any()))
        .expectNext(new ResponseEntity<>(HttpStatus.OK)).verifyComplete();
    //Mockito.verify(transactionCursorRepository, Mockito.times(1)).save(transactionCursorEntity);
  }

}
