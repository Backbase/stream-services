package com.backbase.stream.compositions.transaction.cursor.http;

import com.backbase.stream.compositions.transaction.cursor.api.TransactionCursorApi;
import com.backbase.stream.compositions.transaction.cursor.core.service.TransactionCursorService;
import com.backbase.stream.compositions.transaction.cursor.model.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@AllArgsConstructor
@Slf4j
public class TransactionCursorController implements TransactionCursorApi {

  private final TransactionCursorService transactionCursorService;

  @Override
  public Mono<ResponseEntity<Void>> deleteCursor(String arrangementId, ServerWebExchange exchange) {
    log.debug("TransactionCursorController :: deleteCursor");
    return transactionCursorService.deleteCursor(arrangementId);
  }

  @Override
  public Mono<ResponseEntity<TransactionCursorUpsertResponse>> filterCursor(Mono<TransactionCursorFilterRequest> transactionCursorFilterRequest, ServerWebExchange exchange) {
    return null;
  }

  @Override
  public Mono<ResponseEntity<TransactionCursorResponse>> getByArrangementId(String arrangementId,
      ServerWebExchange exchange) {
      log.debug("TransactionCursorController :: getByArrangementId {} ", arrangementId);
    return transactionCursorService.findByArrangementId(arrangementId);
  }

  @Override
  public Mono<ResponseEntity<TransactionCursorResponse>> getById(String id,
      ServerWebExchange exchange) {
      log.debug("TransactionCursorController :: getById {} ", id);
    return transactionCursorService.findById(id);
  }

  @Override
  public Mono<ResponseEntity<Void>> patchByArrangementId(String arrangementId,
      Mono<TransactionCursorPatchRequest> transactionCursorPatchRequest,
      ServerWebExchange exchange) {
      log.debug("TransactionCursorController :: patchByArrangementId {} ", arrangementId);
    return transactionCursorService
        .patchByArrangementId(arrangementId, transactionCursorPatchRequest);
  }

  @Override
  public Mono<ResponseEntity<TransactionCursorUpsertResponse>> upsertCursor(
      Mono<TransactionCursorUpsertRequest> transactionCursorUpsertRequest,
      ServerWebExchange exchange) {
      log.debug("TransactionCursorController :: upsertCursor");
    return transactionCursorService.upsertCursor(transactionCursorUpsertRequest);
  }
}
