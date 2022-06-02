package com.backbase.stream.compositions.transaction.cursor.core.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.stream.compositions.transaction.cursor.core.domain.TransactionCursorEntity;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorDeleteRequest;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorPatchRequest;
import java.text.ParseException;
import java.util.Optional;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

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
  void deleteCursor_success() {
    when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
    when(criteriaBuilder.createCriteriaDelete(TransactionCursorEntity.class))
        .thenReturn(criteriaDeleteQuery);
    when(criteriaDeleteQuery.from(TransactionCursorEntity.class)).thenReturn(transactionCursor);
    when(entityManager.createQuery(criteriaDeleteQuery)).thenReturn(query);

    int result = transactionCursorRepository.deleteCursor(new TransactionCursorDeleteRequest());
    assertEquals(0, result);
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

    int result = transactionCursorRepository
        .patchByArrangementId("123", new TransactionCursorPatchRequest());
    assertEquals(0, result);
    verify(entityManager, times(1)).getCriteriaBuilder();
    verify(criteriaBuilder, times(1)).createCriteriaUpdate(TransactionCursorEntity.class);


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
