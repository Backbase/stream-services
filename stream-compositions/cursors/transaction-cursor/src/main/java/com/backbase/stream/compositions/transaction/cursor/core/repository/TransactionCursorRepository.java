package com.backbase.stream.compositions.transaction.cursor.core.repository;

import com.backbase.stream.compositions.transaction.cursor.core.domain.TransactionCursorEntity;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorDeleteRequest;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorPatchRequest;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

public interface TransactionCursorRepository {

  /**
   * Query the cursor based on arrangement_id criteria
   *
   * @param arrangementId ArrangementId of the cursor
   * @return TransactionCursorEntity
   */
  @Transactional(
      readOnly = true
  )
  Optional<TransactionCursorEntity> findByArrangementId(String arrangementId);

  /**
   * delete the cursor based on either id or arrangement_id
   *
   * @param transactionCursorDeleteRequest Request Payload to delete a cursor
   * @return if the statement is executed or not (1 or 0)
   */
  @Transactional(
      rollbackFor = SQLException.class
  )
  int deleteCursor(TransactionCursorDeleteRequest transactionCursorDeleteRequest);

  /**
   * Query the cursor based on id criteria
   *
   * @param id Unique key of the cursor
   * @return TransactionCursorEntity
   */
  @Transactional(
      readOnly = true
  )
  Optional<TransactionCursorEntity> findById(String id);

  /**
   * Upsert the cursor
   *
   * @param transactionCursorEntity The Entity Model to Upsert
   * @return primary key of the cursor
   */
  @Transactional(
      rollbackFor = SQLException.class
  )
  Mono<String> upsertCursor(
      TransactionCursorEntity transactionCursorEntity);

  /**
   * Patch the Cursor based on arrangement_id
   *
   * @param arrangementId                 ArrangementId of the Cursor
   * @param transactionCursorPatchRequest Request Payload to Patch the Cursor
   * @return if the statement is executed or not (1 or 0)
   * @throws ParseException exception raised in case of date parsing
   */
  @Transactional(
      rollbackFor = SQLException.class
  )
  int patchByArrangementId(String arrangementId,
      TransactionCursorPatchRequest transactionCursorPatchRequest) throws ParseException;
}
