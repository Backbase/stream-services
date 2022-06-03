package com.backbase.stream.compositions.transaction.cursor.core.service;

import com.backbase.stream.compositions.transaction.cursor.core.domain.TransactionCursorEntity;
import com.backbase.stream.compositions.transaction.cursor.core.mapper.TransactionCursorMapper;
import com.backbase.stream.compositions.transaction.cursor.core.repository.TransactionCursorRepository;
import com.backbase.stream.compositions.transaction.cursor.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.text.ParseException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionCursorServiceImplTest {

  private TransactionCursorService transactionCursorService;

  @Mock
  TransactionCursorMapper mapper;

  @Mock
  TransactionCursorRepository transactionCursorRepository;

  @BeforeEach
  void setUp() {
    transactionCursorService = new TransactionCursorServiceImpl(transactionCursorRepository,
        mapper);
  }

  @Test
  void deleteCursor_success() {
    when(transactionCursorRepository.deleteCursor(any())).thenReturn(anyInt());
    Mono<ResponseEntity<Void>> responseEntity = transactionCursorService
        .deleteCursor(Mono.just(new TransactionCursorDeleteRequest()));
    assertNotNull(responseEntity);
  }

  @Test
  void findByArrangementId_success() {
    TransactionCursorEntity entity = new TransactionCursorEntity();

    when(transactionCursorRepository.findByArrangementId(anyString()))
        .thenReturn(Optional.of(new TransactionCursorEntity()));
    Mono<ResponseEntity<TransactionCursorResponse>> responseEntity =
        transactionCursorService.findByArrangementId(anyString());
    assertNotNull(responseEntity);
  }

  @Test
  void findByArrangementId_notfound() {
    when(transactionCursorRepository.findByArrangementId(anyString()))
            .thenReturn(Optional.empty());
    Mono<ResponseEntity<TransactionCursorResponse>> responseEntity =
            transactionCursorService.findByArrangementId(anyString());
    assertNotNull(responseEntity);
    StepVerifier.create(transactionCursorService.findByArrangementId(anyString()))
            .expectNext(new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @Test
  void findById_success() {
    when(transactionCursorRepository.findById(anyString()))
        .thenReturn(Optional.of(new TransactionCursorEntity()));
    Mono<ResponseEntity<TransactionCursorResponse>> responseEntity =
        transactionCursorService.findById(anyString());
    assertNotNull(responseEntity);
  }

  @Test
  void upsertCursor_success() {
    Mono<ResponseEntity<TransactionCursorUpsertResponse>> responseEntity = transactionCursorService
        .upsertCursor(Mono.just(new TransactionCursorUpsertRequest()));
    assertNotNull(responseEntity);
  }

  @Test
  void patchByArrangementId_success() throws ParseException {
    when(transactionCursorRepository.patchByArrangementId(anyString(), any()))
        .thenReturn(anyInt());
    Mono<ResponseEntity<Void>> responseEntity =
        transactionCursorService
            .patchByArrangementId(anyString(), Mono.just(new TransactionCursorPatchRequest()));
    assertNotNull(responseEntity);
  }

}
