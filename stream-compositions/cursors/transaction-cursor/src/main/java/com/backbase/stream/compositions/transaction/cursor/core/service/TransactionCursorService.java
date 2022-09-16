package com.backbase.stream.compositions.transaction.cursor.core.service;

import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorFilterRequest;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorPatchRequest;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorResponse;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorUpsertRequest;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorUpsertResponse;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionCursorService {

    /**
     * The Service to filter the cursor based on arrangementId
     *
     * @param arrangementId ArrangementId of the Cursor
     * @return TransactionCursorResponse
     */
    Mono<ResponseEntity<TransactionCursorResponse>> findByArrangementId(String arrangementId);

    /**
     * The Service to delete the cursor based on either id or arrangementId
     *
     * @param arrangementId ArrangementId of the Cursor
     * @return Response Entity
     */
    Mono<ResponseEntity<Void>> deleteByArrangementId(
            String arrangementId);

    /**
     * The Service to filter the cursor based on id
     *
     * @param id Id of the Cursor
     * @return TransactionCursorResponse
     */
    Mono<ResponseEntity<TransactionCursorResponse>> findById(String id);

    /**
     * The Service to upsert a cursor
     *
     * @param transactionCursorUpsertRequest TransactionCursorUpsertRequest payload
     * @return TransactionCursorUpsertResponse
     */
    Mono<ResponseEntity<TransactionCursorUpsertResponse>> upsertCursor(
            Mono<TransactionCursorUpsertRequest> transactionCursorUpsertRequest);

    /**
     * The Service to patch the cursor to update lastTxnIds, lastTxnDate & status based on
     * arrangementId
     *
     * @param arrangementId                 ArrangementId of the Cursor
     * @param transactionCursorPatchRequest TransactionCursorPatchRequest Payload
     * @return Response Entity
     */
    Mono<ResponseEntity<Void>> patchByArrangementId(String arrangementId,
                                                    Mono<TransactionCursorPatchRequest> transactionCursorPatchRequest);

    /**
     * The Service to filter the cursor based on status & lastTxnDate
     *
     * @param transactionCursorFilterRequest TransactionCursorFilterRequest Payload
     * @return TransactionCursorResponse
     */
    Mono<ResponseEntity<Flux<TransactionCursorResponse>>> filterCursor(
            Mono<TransactionCursorFilterRequest> transactionCursorFilterRequest);
}
