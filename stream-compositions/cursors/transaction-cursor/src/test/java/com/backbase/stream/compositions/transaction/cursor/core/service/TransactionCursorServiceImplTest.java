package com.backbase.stream.compositions.transaction.cursor.core.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.backbase.stream.compositions.transaction.cursor.core.domain.TransactionCursorEntity;
import com.backbase.stream.compositions.transaction.cursor.core.mapper.TransactionCursorMapper;
import com.backbase.stream.compositions.transaction.cursor.core.repository.TransactionCursorRepository;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursor;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursor.StatusEnum;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorPatchRequest;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorUpsertRequest;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorUpsertResponse;
import java.text.ParseException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class TransactionCursorServiceImplTest {

  private TransactionCursorService transactionCursorService;

  // @Mock
  TransactionCursorMapper mapper = Mappers.getMapper(TransactionCursorMapper.class);

  @Mock
  TransactionCursorRepository transactionCursorRepository;


  @BeforeEach
  void setUp() {
    transactionCursorService = new TransactionCursorServiceImpl(transactionCursorRepository,
        mapper);
  }

  @Test
  void findByArrangementId_success() {
    when(transactionCursorRepository.findByArrangementId(anyString()))
        .thenReturn(Optional.of(new TransactionCursorEntity()));
    StepVerifier.create(transactionCursorService.findByArrangementId(anyString()))
        .expectNext(new ResponseEntity<>(HttpStatus.OK));
  }

  @Test
  void findByArrangementId_noContent() {
    when(transactionCursorRepository.findByArrangementId(anyString()))
        .thenReturn(Optional.empty());
    StepVerifier.create(transactionCursorService.findByArrangementId(anyString()))
        .expectNext(new ResponseEntity<>(HttpStatus.NO_CONTENT));
  }

  @Test
  void deleteByArrangementId_success() {
    Mono<ResponseEntity<Void>> responseEntity = transactionCursorService
        .deleteByArrangementId("");
    assertNotNull(responseEntity);
  }

  @Test
  void findById_noContent() {
    when(transactionCursorRepository.findById(anyString()))
        .thenReturn(Optional.of(new TransactionCursorEntity()));
    StepVerifier.create(transactionCursorService.findById(anyString()))
        .expectNext(new ResponseEntity<>(HttpStatus.NO_CONTENT));
  }

  @Test
  void findById_success() {
    when(transactionCursorRepository.findById(anyString()))
        .thenReturn(Optional.of(new TransactionCursorEntity()));
    StepVerifier.create(transactionCursorService.findById(anyString()))
        .expectNext(new ResponseEntity<>(HttpStatus.OK));
  }

  @Test
  void upsertCursor_success() {
    TransactionCursorEntity transactionCursorEntity = new TransactionCursorEntity();
    transactionCursorEntity.setId("1234567890");
    when(transactionCursorRepository.save(any())).thenReturn(transactionCursorEntity);
    TransactionCursorUpsertRequest transactionCursorUpsertRequest = new TransactionCursorUpsertRequest()
        .withCursor(new TransactionCursor().withArrangementId("123"));
    TransactionCursorUpsertResponse transactionCursorUpsertResponse = new TransactionCursorUpsertResponse()
        .withId("1234567890");
    StepVerifier
        .create(transactionCursorService.upsertCursor(Mono.just(transactionCursorUpsertRequest)))
        .expectNext(new ResponseEntity<>
            (transactionCursorUpsertResponse, HttpStatus.CREATED)).verifyComplete();
  }

  @Test
  void patchByArrangementId_success() {
    lenient().when(transactionCursorRepository.patchByArrangementId(anyString(), any()))
        .thenReturn(1);

    StepVerifier.create(transactionCursorService
        .patchByArrangementId("123", Mono.just(new TransactionCursorPatchRequest())))
        .expectNext(new ResponseEntity<>(HttpStatus.OK));
  }

  @Test
  void patchByArrangementId_error() {
    StepVerifier.create(transactionCursorService.patchByArrangementId("123",
        Mono.just(new TransactionCursorPatchRequest().withLastTxnDate("123-123-123")
            .withStatus(StatusEnum.SUCCESS.getValue())))).expectError(ParseException.class);
  }


}