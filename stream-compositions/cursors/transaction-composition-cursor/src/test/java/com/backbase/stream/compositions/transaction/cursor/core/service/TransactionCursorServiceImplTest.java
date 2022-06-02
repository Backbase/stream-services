package com.backbase.stream.compositions.transaction.cursor.core.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.backbase.stream.compositions.transaction.cursor.core.domain.TransactionCursorEntity;
import com.backbase.stream.compositions.transaction.cursor.core.mapper.TransactionCursorMapper;
import com.backbase.stream.compositions.transaction.cursor.core.repository.TransactionCursorRepository;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorDeleteRequest;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorPatchRequest;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorResponse;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorUpsertRequest;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorUpsertResponse;
import java.text.ParseException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

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
    when(transactionCursorRepository.findByArrangementId(anyString()))
        .thenReturn(Optional.of(new TransactionCursorEntity()));
    Mono<ResponseEntity<TransactionCursorResponse>> responseEntity =
        transactionCursorService.findByArrangementId(anyString());
    assertNotNull(responseEntity);
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
