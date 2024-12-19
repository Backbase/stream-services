package com.backbase.stream.compositions.transaction.cursor.core.repository;

import com.backbase.stream.compositions.transaction.cursor.core.domain.TransactionCursorEntity;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorFilterRequest;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorPatchRequest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class TransactionCursorRepositoryImpl implements TransactionCursorCustomRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd hh:mm:ss";

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private static final String ARRANGEMENT_ID = "arrangementId";

    private static final String STATUS = "status";

    private static final String LAST_TXN_DATE = "lastTxnDate";

    private static final String LAST_TXN_IDS = "lastTxnIds";


    /**
     * Patch the Cursor based on arrangement_id
     *
     * @param arrangementId                 ArrangementId of the Cursor
     * @param transactionCursorPatchRequest Request Payload to Patch the Cursor
     */
    @Override
    public Integer patchByArrangementId(String arrangementId,
                                        TransactionCursorPatchRequest transactionCursorPatchRequest) {
        CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
        CriteriaUpdate<TransactionCursorEntity> cq = criteriaBuilder
                .createCriteriaUpdate(TransactionCursorEntity.class);
        Root<TransactionCursorEntity> transactionCursor = cq.from(TransactionCursorEntity.class);
        cq.set(transactionCursor.get(STATUS), transactionCursorPatchRequest.getStatus());
        if (null != transactionCursorPatchRequest.getLastTxnIds()) {
            cq.set(transactionCursor.get(LAST_TXN_IDS), transactionCursorPatchRequest.getLastTxnIds());
        }
        if (null != transactionCursorPatchRequest.getLastTxnDate()) {
            Timestamp lastTxnDate;
            try {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_TIME_FORMAT);
                lastTxnDate = new Timestamp(
                        simpleDateFormat.parse(transactionCursorPatchRequest.getLastTxnDate()).getTime());
            } catch (ParseException parseException) {
                log.error("Parsing Exception during converting to timestamp {} ",
                        parseException.getMessage());
                throw new RuntimeException(parseException);
            }
            cq.set(transactionCursor.get(LAST_TXN_DATE), lastTxnDate);
        }

        cq.where(
                equalPredicate(criteriaBuilder, transactionCursor.get(ARRANGEMENT_ID), arrangementId));
        int result = this.entityManager.createQuery(cq).executeUpdate();
        log.debug("TransactionCursorRepository :: patchByArrangementId Result {} ", result);
        return result;
    }

    @Override
    public List<TransactionCursorEntity> filterCursor(
            TransactionCursorFilterRequest transactionCursorFilterRequest) {

        CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<TransactionCursorEntity> cq = criteriaBuilder
                .createQuery(TransactionCursorEntity.class);
        Root<TransactionCursorEntity> transactionCursor = cq.from(TransactionCursorEntity.class);
        Predicate statusPredicate = equalPredicate(criteriaBuilder, transactionCursor.get(STATUS),
                transactionCursorFilterRequest.getStatus());
        ParameterExpression<Date> parameter = criteriaBuilder.parameter(java.util.Date.class);
        java.util.Date txnDate;
        try {
            txnDate = new SimpleDateFormat(DATE_FORMAT).parse(
                    transactionCursorFilterRequest.getLastTxnDate());
        } catch (ParseException parseException) {
            log.error("Parsing Exception during extracting transaction date {} ",
                    parseException.getMessage());
            throw new RuntimeException(parseException);
        }
        Predicate lastTxnDatePredicate =
                criteriaBuilder
                        .lessThanOrEqualTo(transactionCursor.get(LAST_TXN_DATE).as(java.sql.Date.class),
                                parameter);

        Predicate filterPredicate = criteriaBuilder.and(statusPredicate, lastTxnDatePredicate);
        cq.where(filterPredicate);
        return this.entityManager.createQuery(cq).setParameter(parameter, txnDate)
                .getResultList();
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

}
