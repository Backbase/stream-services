package com.backbase.stream.compositions.transaction.cursor.core.repository;

import com.backbase.stream.compositions.transaction.cursor.core.domain.TransactionCursorEntity;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorDeleteRequest;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorPatchRequest;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@AllArgsConstructor
@Slf4j
public class TransactionCursorRepositoryImpl implements TransactionCursorRepository {

  @PersistenceContext
  private final EntityManager entityManager;

  private static final String DATE_TIME_FORMAT = "yyyy-MM-dd hh:mm:ss";

  /**
   * Query the cursor based on arrangement_id criteria
   *
   * @param arrangementId ArrangementId of the cursor
   * @return TransactionCursorEntity
   */
  @Override
  public Optional<TransactionCursorEntity> findByArrangementId(String arrangementId) {
    CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
    CriteriaQuery<TransactionCursorEntity> cq = criteriaBuilder
        .createQuery(TransactionCursorEntity.class);
    Root<TransactionCursorEntity> transactionCursor = cq.from(TransactionCursorEntity.class);
    cq.select(transactionCursor).where(equalPredicate(
        criteriaBuilder, transactionCursor.get("arrangement_id"), arrangementId));
    return this.entityManager.createQuery(cq).getResultStream().findFirst();
  }

  /**
   * delete the cursor based on either id or arrangement_id
   *
   * @param transactionCursorDeleteRequest Request Payload to delete a cursor
   * @return if the statement is executed or not (1 or 0)
   */
  @Override
  public int deleteCursor(TransactionCursorDeleteRequest transactionCursorDeleteRequest) {
    CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
    CriteriaDelete<TransactionCursorEntity> cq = criteriaBuilder
        .createCriteriaDelete(TransactionCursorEntity.class);
    Root<TransactionCursorEntity> transactionCursor = cq.from(TransactionCursorEntity.class);
    Predicate idPredicate = equalPredicate(criteriaBuilder, transactionCursor.get("id"),
        transactionCursorDeleteRequest.getId());
    Predicate arrangementPredicate =
        equalPredicate(criteriaBuilder, transactionCursor.get("arrangement_id"),
            transactionCursorDeleteRequest.getArrangementId());
    Predicate deletePredicate = criteriaBuilder.or(idPredicate, arrangementPredicate);
    cq.where(deletePredicate);
    int result = this.entityManager.createQuery(cq).executeUpdate();
    if (log.isDebugEnabled()) {
      log.debug("TransactionCursorRepository :: deleteCursor Result {} ", result);
    }
    return result;
  }

  /**
   * Query the cursor based on id criteria
   *
   * @param id Unique key of the cursor
   * @return TransactionCursorEntity
   */
  @Override
  public Optional<TransactionCursorEntity> findById(String id) {
    CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
    CriteriaQuery<TransactionCursorEntity> cq = criteriaBuilder
        .createQuery(TransactionCursorEntity.class);
    Root<TransactionCursorEntity> transactionCursor = cq.from(TransactionCursorEntity.class);
    cq.select(transactionCursor).where(equalPredicate(
        criteriaBuilder, transactionCursor.get("id"), id));
    return this.entityManager.createQuery(cq).getResultStream().findFirst();
  }

  /**
   * Upsert the cursor
   *
   * @param transactionCursorEntity The Entity Model to Upsert
   * @return primary key of the cursor
   */
  @Override
  public Mono<String> upsertCursor(
      TransactionCursorEntity transactionCursorEntity) {
    TransactionCursorEntity transCursorEntity = this.entityManager
        .merge(transactionCursorEntity);
    return Mono.justOrEmpty(transCursorEntity.getId());
  }

  /**
   * Patch the Cursor based on arrangement_id
   *
   * @param arrangementId                 ArrangementId of the Cursor
   * @param transactionCursorPatchRequest Request Payload to Patch the Cursor
   * @return if the statement is executed or not (1 or 0)
   * @throws ParseException exception raised in case of date parsing
   */
  @Override
  public int patchByArrangementId(String arrangementId,
      TransactionCursorPatchRequest transactionCursorPatchRequest) throws ParseException {
    CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
    CriteriaUpdate<TransactionCursorEntity> cq = criteriaBuilder
        .createCriteriaUpdate(TransactionCursorEntity.class);
    Root<TransactionCursorEntity> transactionCursor = cq.from(TransactionCursorEntity.class);
    cq.set(transactionCursor.get("status"), transactionCursorPatchRequest.getStatus());
    if (null != transactionCursorPatchRequest.getLastTxnIds()) {
      cq.set(transactionCursor.get("last_txn_ids"), transactionCursorPatchRequest.getLastTxnIds());
    }
    if (null != transactionCursorPatchRequest.getLastTxnDate()) {
      cq.set(transactionCursor.get("last_txn_date"),
          convertStringToTimestampFormat(transactionCursorPatchRequest.getLastTxnDate()));
    }

    cq.where(
        equalPredicate(criteriaBuilder, transactionCursor.get("arrangement_id"), arrangementId));
    int result = this.entityManager.createQuery(cq).executeUpdate();
    if (log.isDebugEnabled()) {
      log.debug("TransactionCursorRepository :: patchByArrangementId Result {} ", result);
    }
    return result;
  }

  /**
   * Generate the Predicate based on expression & operator
   *
   * @param criteriaBuilder CriteriaBuilder
   * @param expression      Path Expression
   * @param value           Path Expression Value
   * @return Predicate Returns the predicate
   */
  private Predicate equalPredicate(CriteriaBuilder criteriaBuilder, Path<String> expression,
      String value) {
    return criteriaBuilder.equal(expression, value);
  }

  /**
   * Convert String to SQL timestamp for the lastTxnDate column
   *
   * @param lastTxnDate Last Transaction Date of the Cursor
   * @return Parsed Timestamp
   * @throws ParseException exception raised in case of date parsing
   */
  private Timestamp convertStringToTimestampFormat(String lastTxnDate) throws ParseException {
    if (Objects.nonNull(lastTxnDate)) {
      SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_TIME_FORMAT);
      return new Timestamp(simpleDateFormat.parse(lastTxnDate).getTime());
    }
    return Timestamp.from(Instant.now());
  }

}
