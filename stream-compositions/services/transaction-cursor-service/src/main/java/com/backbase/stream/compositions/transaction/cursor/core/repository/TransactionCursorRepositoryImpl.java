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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@Slf4j
public class TransactionCursorRepositoryImpl implements TransactionCursorRepository {

  @PersistenceContext
  private EntityManager entityManager;

  private static final String dateFormat = "yyyy-MM-dd hh:mm:ss";

  /**
   * Query the cursor based on arrangement_id criteria
   * @param arrangementId
   * @return TransactionCursorEntity
   */
  @Override
  public Optional<TransactionCursorEntity> findByArrangementId(String arrangementId) {
    CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
    CriteriaQuery<TransactionCursorEntity> cq = criteriaBuilder
        .createQuery(TransactionCursorEntity.class);
    Root<TransactionCursorEntity> transactionCursor = cq.from(TransactionCursorEntity.class);
    cq.select(transactionCursor).where(toPredicate(
        criteriaBuilder, transactionCursor.get("arrangement_id"), arrangementId, "EQUALS"));
    return this.entityManager.createQuery(cq).getResultStream().findFirst();
  }

  /**
   * delete the cursor based on either id or arrangement_id
   * @param transactionCursorDeleteRequest
   * @return if the statement is executed or not (1 or 0)
   */
  @Override
  public int deleteCursor(TransactionCursorDeleteRequest transactionCursorDeleteRequest) {
    CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
    CriteriaDelete<TransactionCursorEntity> cq = criteriaBuilder
        .createCriteriaDelete(TransactionCursorEntity.class);
    Root<TransactionCursorEntity> transactionCursor = cq.from(TransactionCursorEntity.class);
    Predicate idPredicate = toPredicate(criteriaBuilder, transactionCursor.get("id"),
        transactionCursorDeleteRequest.getId(), "EQUALS");
    Predicate arrangementPredicate =
        toPredicate(criteriaBuilder, transactionCursor.get("arrangement_id"),
            transactionCursorDeleteRequest.getArrangementId(), "EQUALS");
    Predicate deletePredicate = criteriaBuilder.or(idPredicate, arrangementPredicate);
    cq.where(deletePredicate);
    int result = this.entityManager.createQuery(cq).executeUpdate();
    log.debug("TransactionCursorDeleteRequest Result {} ", result);
    return result;
  }

  /**
   * Query the cursor based on id criteria
   * @param id
   * @return TransactionCursorEntity
   */
  @Override
  public Optional<TransactionCursorEntity> findById(String id) {
    CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
    CriteriaQuery<TransactionCursorEntity> cq = criteriaBuilder
        .createQuery(TransactionCursorEntity.class);
    Root<TransactionCursorEntity> transactionCursor = cq.from(TransactionCursorEntity.class);
    cq.select(transactionCursor).where(toPredicate(
        criteriaBuilder, transactionCursor.get("id"), id, "EQUALS"));
    return this.entityManager.createQuery(cq).getResultStream().findFirst();
  }

  /**
   * Upsert the cursor
   * @param transactionCursorEntity
   * @return primary key of the cursor
   */
  @Override
  public Mono<String> upsertCursor(
      TransactionCursorEntity transactionCursorEntity) {
    TransactionCursorEntity transCursorEntity = this.entityManager
        .merge(transactionCursorEntity);
    log.debug("I am in repository upsertCursor {} ", transCursorEntity.getId());
    return Mono.just(transCursorEntity.getId());
  }

  /**
   * Patch the Cursor based on arrangement_id
   * @param arrangementId
   * @param transactionCursorPatchRequest
   * @return if the statement is executed or not (1 or 0)
   * @throws ParseException
   */
  @Override
  public int patchByArrangementId(String arrangementId,
      TransactionCursorPatchRequest transactionCursorPatchRequest) throws ParseException {
    CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
    CriteriaUpdate<TransactionCursorEntity> cq = criteriaBuilder
        .createCriteriaUpdate(TransactionCursorEntity.class);
    Root<TransactionCursorEntity> transactionCursor = cq.from(TransactionCursorEntity.class);
    cq.set(transactionCursor.get("status"), transactionCursorPatchRequest.getStatus());
    cq.set(transactionCursor.get("last_txn_ids"), transactionCursorPatchRequest.getLastTxnIds());
    cq.set(transactionCursor.get("last_txn_date"),
        convertStringToTimestampFormat(transactionCursorPatchRequest.getLastTxnDate()));
    cq.where(toPredicate(criteriaBuilder, transactionCursor.get("arrangement_id"), arrangementId,
        "EQUALS"));
    int result = this.entityManager.createQuery(cq).executeUpdate();
    log.debug("patchByArrangementId Result {} ", result);
    return result;
  }

  /**
   * Generate the Predicate based on expression & operator
   * @param criteriaBuilder
   * @param expression
   * @param value
   * @param operator
   * @return Predicate
   */
  private Predicate toPredicate(CriteriaBuilder criteriaBuilder, Path expression, String value,
      String operator) {
    switch (operator) {
      case "EQUALS":
        return criteriaBuilder.equal(expression, value);
      default:
        return null;
    }
  }

  /**
   * Convert String to SQL timestamp for the lastTxnDate column
   * @param lastTxnDate
   * @return
   * @throws ParseException
   */
  private Timestamp convertStringToTimestampFormat(String lastTxnDate) throws ParseException {
    try {
      if (Objects.nonNull(lastTxnDate)) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
        return new Timestamp(simpleDateFormat.parse(lastTxnDate).getTime());
      }
    } catch (ParseException parseException) {
      throw parseException;
    }
    return Timestamp.from(Instant.now());
  }

}
