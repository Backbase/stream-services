package com.backbase.stream.compositions.transaction.cursor.core.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.stream.compositions.transaction.cursor.core.domain.TransactionCursorEntity;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursor.StatusEnum;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorFilterRequest;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorPatchRequest;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("local")
class TransactionCursorRepositoryImplTest {

  @Spy
  TransactionCursorCustomRepository transactionCursorCustomRepository;

  @Mock
  EntityManager entityManager;

  @Mock
  CriteriaBuilder criteriaBuilder;

  @Mock
  CriteriaQuery<TransactionCursorEntity> criteriaQuery;

  @Mock
  CriteriaUpdate<TransactionCursorEntity> criteriaUpdateQuery;

  @Spy
  Root<TransactionCursorEntity> transactionCursor;

  @Mock
  TypedQuery<TransactionCursorEntity> query;

  @MockBean
  TransactionCursorEntity transactionCursorEntity;

  @Mock
  ParameterExpression<Date> parameterExpression;

  @Mock
  Predicate predicate;

  @Mock
  Path<Object> path;

  @Mock
  TypedQuery<TransactionCursorEntity> typedQuery;

  @BeforeEach
  void setUp() {
    transactionCursorCustomRepository = new TransactionCursorRepositoryImpl(entityManager);
  }

  @Test
  void patchByArrangementId_success() {
    when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
    when(criteriaBuilder.createCriteriaUpdate(TransactionCursorEntity.class))
        .thenReturn(criteriaUpdateQuery);
    when(criteriaUpdateQuery.from(TransactionCursorEntity.class)).thenReturn(transactionCursor);
    when(entityManager.createQuery(criteriaUpdateQuery)).thenReturn(query);
    when(entityManager.createQuery(criteriaUpdateQuery).executeUpdate()).thenReturn(1);

    int result = transactionCursorCustomRepository
        .patchByArrangementId("123", new TransactionCursorPatchRequest()
            .withStatus(StatusEnum.IN_PROGRESS.getValue()).withLastTxnIds("11,12,13,14")
            .withLastTxnDate("2022-06-02 03:18:19"));
    assertEquals(1, result);
    verify(entityManager, times(1)).getCriteriaBuilder();
    verify(criteriaBuilder, times(1)).createCriteriaUpdate(TransactionCursorEntity.class);

    int resultWithOutTxnDate = transactionCursorCustomRepository
        .patchByArrangementId("123", new TransactionCursorPatchRequest()
            .withStatus(StatusEnum.IN_PROGRESS.getValue()).withLastTxnIds("11,12,13,14"));
    assertEquals(1, resultWithOutTxnDate);
  }

  @Test
  void patchByArrangementId_fail() {
    when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
    when(criteriaBuilder.createCriteriaUpdate(TransactionCursorEntity.class))
        .thenReturn(criteriaUpdateQuery);
    when(criteriaUpdateQuery.from(TransactionCursorEntity.class)).thenReturn(transactionCursor);
    try {
      transactionCursorCustomRepository
          .patchByArrangementId("123", new TransactionCursorPatchRequest()
              .withStatus(StatusEnum.IN_PROGRESS.getValue()).withLastTxnIds("11,12,13,14")
              .withLastTxnDate("2022-06 03:18:19"));
    } catch (Exception exception) {
      assertThat(exception instanceof ParseException);
    }
  }

  @Test
  void filterCursor_success() throws ParseException {
    when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
    when(criteriaBuilder.createQuery(TransactionCursorEntity.class)).thenReturn(criteriaQuery);
    when(criteriaQuery.from(TransactionCursorEntity.class)).thenReturn(transactionCursor);
    when(transactionCursor.get(anyString())).thenReturn(path);
    when(criteriaBuilder.equal(transactionCursor.get(anyString()), StatusEnum.SUCCESS.getValue()))
        .thenReturn(predicate);
    when(criteriaBuilder
        .lessThanOrEqualTo(transactionCursor.get(anyString()).as(java.util.Date.class),
            parameterExpression)).thenReturn(predicate);
    when(criteriaBuilder.and(predicate, predicate)).thenReturn(predicate);
    when(criteriaBuilder.parameter(java.util.Date.class)).thenReturn(parameterExpression);
    when(entityManager.createQuery(criteriaQuery)).thenReturn(query);
    when(query.setParameter(parameterExpression,
        new SimpleDateFormat("yyyy-MM-dd").parse("2022-05-24 03:18:59")))
        .thenReturn(typedQuery);
    when(typedQuery.getResultList()).thenReturn(List.of(getMockDomain()));
    List<TransactionCursorEntity> transactionCursorEntities = transactionCursorCustomRepository
        .filterCursor(new TransactionCursorFilterRequest().withStatus(StatusEnum.SUCCESS
            .getValue()).withLastTxnDate("2022-05-24 03:18:59"));
    assertNotNull(transactionCursorEntities);
    assertThat(transactionCursorEntities.size()).isEqualTo(1);
    assertThat(transactionCursorEntities.get(0).getArrangementId())
        .isEqualTo("4337f8cc-d66d-41b3-a00e-f71ff15d93cq");
    verify(entityManager, times(1)).getCriteriaBuilder();
    verify(criteriaBuilder, times(1)).createQuery(TransactionCursorEntity.class);

  }

  @Test
  void filterCursor_fail() {
    when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
    when(criteriaBuilder.createQuery(TransactionCursorEntity.class)).thenReturn(criteriaQuery);
    when(criteriaQuery.from(TransactionCursorEntity.class)).thenReturn(transactionCursor);
    when(transactionCursor.get(anyString())).thenReturn(path);
    when(criteriaBuilder.equal(transactionCursor.get(anyString()), StatusEnum.SUCCESS.getValue()))
        .thenReturn(predicate);
    try {
      transactionCursorCustomRepository
          .filterCursor(new TransactionCursorFilterRequest().withStatus(StatusEnum.SUCCESS
              .getValue()).withLastTxnDate("2022-12 03:18:59"));
    } catch (Exception exception) {
      assertThat(exception instanceof ParseException);
    }
  }

  @Test
  void filterCursorNullTxnDate_Success() {

  }

  private TransactionCursorEntity getMockDomain() {
    TransactionCursorEntity transactionCursorEntity = new TransactionCursorEntity();
    transactionCursorEntity.setId("3337f8cc-d66d-41b3-a00e-f71ff15d93cq");
    transactionCursorEntity.setArrangementId("4337f8cc-d66d-41b3-a00e-f71ff15d93cq");
    transactionCursorEntity.setExtArrangementId("5337f8cc-d66d-41b3-a00e-f71ff15d93cq");
    transactionCursorEntity.setLegalEntityId("test-ext-emp");
    transactionCursorEntity.setLastTxnDate(Timestamp.from(Instant.now()));
    transactionCursorEntity.setStatus(StatusEnum.IN_PROGRESS.getValue());
    transactionCursorEntity.setLastTxnIds("11,12,13,14");
    transactionCursorEntity.setAdditions("{\"key1\":\"val1\"}");
    return transactionCursorEntity;
  }

}
