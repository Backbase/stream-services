package com.backbase.stream.compositions.transaction.cursor.core.repository;

import com.backbase.stream.compositions.transaction.cursor.core.domain.TransactionCursorEntity;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursor.StatusEnum;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorPatchRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.text.ParseException;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("local")
class TransactionCursorRepositoryImplTest {

  @Autowired
  TransactionCursorRepository transactionCursorRepository;

  @Mock
  EntityManager entityManager;

  @Mock
  CriteriaBuilder criteriaBuilder;

  @Mock
  CriteriaQuery<TransactionCursorEntity> criteriaQuery;

  @Mock
  CriteriaDelete<TransactionCursorEntity> criteriaDeleteQuery;

  @Mock
  CriteriaUpdate<TransactionCursorEntity> criteriaUpdateQuery;

  @Mock
  Root<TransactionCursorEntity> transactionCursor;

  @Mock
  TypedQuery<TransactionCursorEntity> query;

  @Mock
  TransactionCursorEntity transactionCursorEntity;

  @BeforeEach
  void setUp() {
    transactionCursorRepository = new TransactionCursorRepositoryImpl(entityManager);
  }

  @Test
  void findByArrangementId_success() {
    when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
    createQueryMock();
    Optional<TransactionCursorEntity> transactionCursorEntity =
        transactionCursorRepository.findByArrangementId("123");
    assertCreateQuery(transactionCursorEntity);
  }

  @Test
  void findByArrangementId_notfound() {
    when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
    createQueryMock();
    Optional<TransactionCursorEntity> transactionCursorEntity =
        transactionCursorRepository.findByArrangementId("123");
    assertCreateQuery(transactionCursorEntity);
  }

  @Test
  void deleteCursor_success() {
    when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
    when(criteriaBuilder.createCriteriaDelete(TransactionCursorEntity.class))
        .thenReturn(criteriaDeleteQuery);
    when(criteriaDeleteQuery.from(TransactionCursorEntity.class)).thenReturn(transactionCursor);
    when(entityManager.createQuery(criteriaDeleteQuery)).thenReturn(query);
    when(entityManager.createQuery(criteriaDeleteQuery).executeUpdate()).thenReturn(1);

    int result = transactionCursorRepository.deleteCursor("");
    assertEquals(1, result);
    verify(entityManager, times(1)).getCriteriaBuilder();
    verify(criteriaBuilder, times(1)).createCriteriaDelete(TransactionCursorEntity.class);

  }

  @Test
  void findById_success() {
    when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
    createQueryMock();
    Optional<TransactionCursorEntity> transactionCursorEntity =
        transactionCursorRepository.findById("123");
    assertCreateQuery(transactionCursorEntity);
  }

  @Test
  void upsertCursor_success() {
    doAnswer((Answer<TransactionCursorEntity>) invocationOnMock -> {
      TransactionCursorEntity transactionCursorEntity = (TransactionCursorEntity) invocationOnMock
          .getArguments()[0];
      transactionCursorEntity.setId("1");
      return transactionCursorEntity;
    }).when(entityManager).merge(transactionCursorEntity);
    transactionCursorRepository.upsertCursor(transactionCursorEntity);
    verify(entityManager, times(1)).merge(transactionCursorEntity);
  }

  @Test
  void patchByArrangementId_success() throws ParseException {
    when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
    when(criteriaBuilder.createCriteriaUpdate(TransactionCursorEntity.class))
        .thenReturn(criteriaUpdateQuery);
    when(criteriaUpdateQuery.from(TransactionCursorEntity.class)).thenReturn(transactionCursor);
    when(entityManager.createQuery(criteriaUpdateQuery)).thenReturn(query);
    when(entityManager.createQuery(criteriaUpdateQuery).executeUpdate()).thenReturn(1);

    int result = transactionCursorRepository
        .patchByArrangementId("123", new TransactionCursorPatchRequest()
            .withStatus(StatusEnum.IN_PROGRESS.getValue()).withLastTxnIds("11,12,13,14")
            .withLastTxnDate("2022-06-02 03:18:19"));
    assertEquals(1, result);
    verify(entityManager, times(1)).getCriteriaBuilder();
    verify(criteriaBuilder, times(1)).createCriteriaUpdate(TransactionCursorEntity.class);

    int resultWithOutTxnDate = transactionCursorRepository
        .patchByArrangementId("123", new TransactionCursorPatchRequest()
            .withStatus(StatusEnum.IN_PROGRESS.getValue()).withLastTxnIds("11,12,13,14"));
    assertEquals(1, resultWithOutTxnDate);

  }


  void createQueryMock() {
    when(criteriaBuilder.createQuery(TransactionCursorEntity.class)).thenReturn(criteriaQuery);
    when(criteriaQuery.from(TransactionCursorEntity.class)).thenReturn(transactionCursor);
    when(criteriaQuery.select(transactionCursor)).thenReturn(criteriaQuery);
    when(entityManager.createQuery(criteriaQuery)).thenReturn(query);
    when(query.getResultStream()).thenReturn(Stream.of(new TransactionCursorEntity()));
  }

  void assertCreateQuery(Optional<TransactionCursorEntity> transactionCursorEntity) {
    assertNotNull(transactionCursorEntity);
    verify(entityManager, times(1)).getCriteriaBuilder();
    verify(criteriaBuilder, times(1)).createQuery(TransactionCursorEntity.class);
  }
}
