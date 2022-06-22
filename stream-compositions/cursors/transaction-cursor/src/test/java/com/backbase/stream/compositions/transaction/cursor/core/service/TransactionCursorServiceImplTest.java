package com.backbase.stream.compositions.transaction.cursor.core.service;

import com.backbase.stream.compositions.transaction.cursor.core.config.TransactionCursorConfigurationProperties;
import com.backbase.stream.compositions.transaction.cursor.core.domain.TransactionCursorEntity;
import com.backbase.stream.compositions.transaction.cursor.core.mapper.TransactionCursorMapper;
import com.backbase.stream.compositions.transaction.cursor.core.repository.TransactionCursorRepository;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursor;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursor.StatusEnum;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorDeleteRequest;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorPatchRequest;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorUpsertRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.text.ParseException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionCursorServiceImplTest {

  private TransactionCursorService transactionCursorService;

  @Mock
  TransactionCursorMapper mapper;

  @Mock
  TransactionCursorRepository transactionCursorRepository;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  TransactionCursorConfigurationProperties transactionCursorConfigurationProperties;

  @BeforeEach
  void setUp() {
    transactionCursorService = new TransactionCursorServiceImpl(
            transactionCursorRepository,
            mapper,
            transactionCursorConfigurationProperties);
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
    when(transactionCursorRepository.findByArrangementId(anyString())).thenReturn(Optional.empty());
    StepVerifier.create(transactionCursorService.findByArrangementId(anyString()))
        .expectNext(new ResponseEntity<>(HttpStatus.NO_CONTENT));
  }

  @Test
  void deleteCursor_success() {
    when(transactionCursorRepository.deleteCursor(any())).thenReturn(anyInt());
    Mono<ResponseEntity<Void>> responseEntity = transactionCursorService
        .deleteCursor(Mono.just(new TransactionCursorDeleteRequest()));
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
    StepVerifier
        .create(transactionCursorService.upsertCursor(Mono.just(new TransactionCursorUpsertRequest()
            .withCursor(new TransactionCursor().withId("123")))))
        .expectNext(new ResponseEntity<>(HttpStatus.CREATED));

    StepVerifier
        .create(transactionCursorService.upsertCursor(Mono.just(new TransactionCursorUpsertRequest()
            .withCursor(new TransactionCursor().withId("123")))))
        .consumeNextWith(transactionCursorUpsertResponseResponseEntity
            -> assertEquals(
            "123",transactionCursorUpsertResponseResponseEntity.getBody().getId()));
  }

  @Test
  void patchByArrangementId_success() throws ParseException {
    when(transactionCursorRepository.patchByArrangementId(anyString(), any(TransactionCursorPatchRequest.class))).thenReturn(1);
    when(transactionCursorConfigurationProperties.getTransactionIdPersistence().isEnabled()).thenReturn(true);

    StepVerifier.create(transactionCursorService
            .patchByArrangementId("123", Mono.just(new TransactionCursorPatchRequest())))
            .expectNext(new ResponseEntity<>(HttpStatus.OK));
  }

  @Test
  void patchByArrangementId_error() throws ParseException {
    when(transactionCursorRepository.patchByArrangementId("123",
        new TransactionCursorPatchRequest().withLastTxnDate("123-123-123")
            .withStatus(StatusEnum.SUCCESS.getValue())))
        .thenThrow(new ParseException("1", 0));

    when(transactionCursorConfigurationProperties.getTransactionIdPersistence().isEnabled()).thenReturn(true);

    StepVerifier.create(transactionCursorService.patchByArrangementId("123",
        Mono.just(new TransactionCursorPatchRequest().withLastTxnDate("123-123-123")
            .withStatus(StatusEnum.SUCCESS.getValue())))).expectError(ParseException.class);

  }


}
