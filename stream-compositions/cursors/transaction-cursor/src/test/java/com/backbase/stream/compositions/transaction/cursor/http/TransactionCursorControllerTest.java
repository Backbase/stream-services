package com.backbase.stream.compositions.transaction.cursor.http;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.stream.compositions.transaction.cursor.core.service.TransactionCursorService;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursor;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorResponse;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorUpsertResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class TransactionCursorControllerTest {

  @Mock
  TransactionCursorService transactionCursorService;

  TransactionCursorController transactionCursorController;

  @BeforeEach
  void setUp() {
    transactionCursorController = new TransactionCursorController(transactionCursorService);
  }

  @Test
  void testDeleteCursor_success() {
    when(transactionCursorService.deleteByArrangementId(any()))
        .thenReturn(Mono.empty());
    transactionCursorController.deleteByArrangementId(any(), null);
    verify(transactionCursorService).deleteByArrangementId(any());
  }

  @Test
  void testFindByArrangementId_success() {
    when(transactionCursorService.findByArrangementId(any()))
        .thenReturn(Mono.just(new ResponseEntity<>
            (new TransactionCursorResponse().withCursor(new TransactionCursor()), HttpStatus.OK)));
    Mono<ResponseEntity<TransactionCursorResponse>> responseEntity = transactionCursorController
        .getByArrangementId(anyString(), null);
    assertNotNull(responseEntity.block().getBody());
    verify(transactionCursorService).findByArrangementId(any());
  }

  @Test
  void testFindById_success() {
    when(transactionCursorService.findById(any()))
        .thenReturn(Mono.just(new ResponseEntity<>
            (new TransactionCursorResponse().withCursor(new TransactionCursor()), HttpStatus.OK)));
    Mono<ResponseEntity<TransactionCursorResponse>> responseEntity = transactionCursorController
        .getById(anyString(), null);
    assertNotNull(responseEntity.block().getBody());
    verify(transactionCursorService).findById(any());
  }


  @Test
  void testPatchByArrangementId_success() {
    when(transactionCursorService.patchByArrangementId(anyString(), any()))
        .thenReturn(Mono.empty());
    transactionCursorController.patchByArrangementId(anyString(), any(), null);
    verify(transactionCursorService).patchByArrangementId(anyString(), any());
  }

  @Test
  void testUpsertCursor_success() {
    when(transactionCursorService.upsertCursor(any()))
        .thenReturn(Mono.just(new ResponseEntity<>
            (new TransactionCursorUpsertResponse(), HttpStatus.OK)));
    transactionCursorController.upsertCursor(any(), null);
    verify(transactionCursorService).upsertCursor(any());
  }

}