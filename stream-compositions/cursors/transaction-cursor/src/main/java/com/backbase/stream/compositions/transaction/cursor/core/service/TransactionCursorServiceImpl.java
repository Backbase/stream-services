package com.backbase.stream.compositions.transaction.cursor.core.service;

import com.backbase.stream.compositions.transaction.cursor.core.domain.TransactionCursorEntity;
import com.backbase.stream.compositions.transaction.cursor.core.mapper.TransactionCursorMapper;
import com.backbase.stream.compositions.transaction.cursor.core.repository.TransactionCursorRepository;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorFilterRequest;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorPatchRequest;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorResponse;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorUpsertRequest;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorUpsertResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
@Slf4j
public class TransactionCursorServiceImpl implements TransactionCursorService {

  private final TransactionCursorRepository transactionCursorRepository;
  private final TransactionCursorMapper mapper;

  /**
   * The Service to filter the cursor based on arrangementId
   *
   * @param arrangementId ArrangementId of the Cursor
   * @return TransactionCursorResponse
   */
  @Override
  public Mono<ResponseEntity<TransactionCursorResponse>> findByArrangementId(String arrangementId) {
    log.debug("TransactionCursorService :: findByArrangementId {} ", arrangementId);
    return transactionCursorRepository
        .findByArrangementId(arrangementId)
        .map(this::mapModelToResponse)
        .orElse(Mono.just(new ResponseEntity<>(HttpStatus.NO_CONTENT)));
  }

  /**
   * The Service to delete the cursor based on either id or arrangementId
   *
   * @param arrangementId ArrangementId of the Cursor
   * @return Response Entity
   */
  @Override
  public Mono<ResponseEntity<Void>> deleteByArrangementId(String arrangementId) {
    log.debug("TransactionCursorService :: deleteByArrangementId {} ", arrangementId);
    transactionCursorRepository.deleteByArrangementId(arrangementId);
    return Mono.empty();
  }

  /**
   * The Service to filter the cursor based on id
   *
   * @param id Id of the Cursor
   * @return TransactionCursorResponse
   */
  @Override
  public Mono<ResponseEntity<TransactionCursorResponse>> findById(String id) {
    log.debug("TransactionCursorService :: findById {} ", id);
    return transactionCursorRepository
        .findById(id)
        .map(this::mapModelToResponse)
        .orElse(Mono.just(new ResponseEntity<>(HttpStatus.NO_CONTENT)));
  }

  /**
   * The Service to upsert a cursor
   *
   * @param transactionCursorUpsertRequest TransactionCursorUpsertRequest payload
   * @return TransactionCursorUpsertResponse
   */
  @Override
  public Mono<ResponseEntity<TransactionCursorUpsertResponse>> upsertCursor(
      Mono<TransactionCursorUpsertRequest> transactionCursorUpsertRequest) {
    log.debug("TransactionCursorService :: upsertCursor");
    return transactionCursorUpsertRequest
        .map(mapper::mapToDomain)
        .map(transactionCursorRepository::save)
        .doOnNext(entity -> log.info("Id is {}", entity.getId()))
        .map(
            entity ->
                new ResponseEntity<>(
                    new TransactionCursorUpsertResponse().withId(entity.getId()),
                    HttpStatus.CREATED));
  }

  /**
   * The Service to patch the cursor to update lastTxnIds, lastTxnDate & status based on
   * arrangementId
   *
   * @param arrangementId ArrangementId of the Cursor
   * @param transactionCursorPatchRequest TransactionCursorPatchRequest Payload
   * @return Response Entity
   */
  @Override
  public Mono<ResponseEntity<Void>> patchByArrangementId(
      String arrangementId, Mono<TransactionCursorPatchRequest> transactionCursorPatchRequest) {
    log.debug("TransactionCursorService :: patchByArrangementId {} ", arrangementId);

    return transactionCursorPatchRequest
        .map(
            transactionCursorPatchReq ->
                transactionCursorRepository.patchByArrangementId(
                    arrangementId, transactionCursorPatchReq))
        .doOnNext(result -> log.debug("Patch By ArrangementId result {} ", result))
        .doOnError(result -> new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR))
        .map(result -> new ResponseEntity<>(HttpStatus.OK));
  }

  @Override
  public Mono<ResponseEntity<Flux<TransactionCursorResponse>>> filterCursor(
      Mono<TransactionCursorFilterRequest> transactionCursorFilterRequest) {
    log.debug("TransactionCursorService :: filterCursor");
    Flux<TransactionCursorResponse> filteredCursors =
        transactionCursorFilterRequest
            .map(transactionCursorRepository::filterCursor)
            .flatMapIterable(mapper::mapToListModel);
    return Mono.just(ResponseEntity.ok(filteredCursors));
  }

  /**
   * Transform domain to model response
   *
   * @param response Model to CursorResponse
   * @return TransactionCursorResponse
   */
  private Mono<ResponseEntity<TransactionCursorResponse>> mapModelToResponse(
      TransactionCursorEntity response) {
    return Mono.just(new ResponseEntity<>(mapper.mapToModel(response), HttpStatus.OK));
  }
}
