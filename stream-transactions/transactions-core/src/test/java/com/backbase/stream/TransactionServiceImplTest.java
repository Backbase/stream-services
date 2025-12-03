package com.backbase.stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.dbs.transaction.api.service.v3.TransactionPresentationServiceApi;
import com.backbase.dbs.transaction.api.service.v3.model.ArrangementItem;
import com.backbase.dbs.transaction.api.service.v3.model.TransactionItem;
import com.backbase.dbs.transaction.api.service.v3.model.TransactionsDeleteRequestBody;
import com.backbase.dbs.transaction.api.service.v3.model.TransactionsPatchRequestBody;
import com.backbase.dbs.transaction.api.service.v3.model.TransactionsPostRequestBody;
import com.backbase.stream.transaction.TransactionTask;
import com.backbase.stream.transaction.TransactionUnitOfWorkExecutor;
import com.backbase.stream.transaction.TransactionsQuery;
import com.backbase.stream.worker.model.UnitOfWork;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionPresentationServiceApi transactionPresentationServiceApi;

    @Mock
    private TransactionUnitOfWorkExecutor transactionTaskExecutor;

    private TransactionServiceImpl transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionServiceImpl(
            transactionPresentationServiceApi,
            transactionTaskExecutor
        );
    }

    @Test
    void processTransactions_shouldPrepareAndExecuteUnitOfWork() {
        // Given
        TransactionsPostRequestBody transactionRequest = new TransactionsPostRequestBody();
        Flux<TransactionsPostRequestBody> transactions = Flux.just(transactionRequest);

        TransactionTask transactionTask = new TransactionTask("test-id", Collections.singletonList(transactionRequest));
        UnitOfWork<TransactionTask> unitOfWork = UnitOfWork.from("test-id", transactionTask);
        UnitOfWork<TransactionTask> executedUnitOfWork = UnitOfWork.from("test-id", transactionTask);

        when(transactionTaskExecutor.prepareUnitOfWork(transactions))
            .thenReturn(Flux.just(unitOfWork));
        when(transactionTaskExecutor.executeUnitOfWork(unitOfWork))
            .thenReturn(Mono.just(executedUnitOfWork));

        // When
        Flux<UnitOfWork<TransactionTask>> result = transactionService.processTransactions(transactions);

        // Then
        StepVerifier.create(result)
            .expectNext(executedUnitOfWork)
            .verifyComplete();

        verify(transactionTaskExecutor, times(1)).prepareUnitOfWork(transactions);
        verify(transactionTaskExecutor, times(1)).executeUnitOfWork(unitOfWork);
    }

    @Test
    void getLatestTransactions_shouldReturnTransactions() {
        // Given
        String arrangementId = "arrangement-123";
        int size = 10;

        TransactionItem transaction1 = new TransactionItem().id("tx-1");
        TransactionItem transaction2 = new TransactionItem().id("tx-2");

        when(transactionPresentationServiceApi.getTransactions(
            isNull(), isNull(), isNull(), isNull(), isNull(),
            isNull(), isNull(), isNull(), isNull(), isNull(),
            isNull(), isNull(), isNull(), isNull(), isNull(),
            isNull(), isNull(), isNull(), isNull(), isNull(),
            eq(arrangementId), isNull(), isNull(), isNull(), isNull(),
            isNull(), isNull(), isNull(), eq(size), isNull(),
            isNull(), isNull()
        )).thenReturn(Flux.just(transaction1, transaction2));

        // When
        Flux<TransactionItem> result = transactionService.getLatestTransactions(arrangementId, size);

        // Then
        StepVerifier.create(result)
            .expectNext(transaction1)
            .expectNext(transaction2)
            .verifyComplete();
    }

    @Test
    void getLatestTransactions_shouldHandleNotFoundError() {
        // Given
        String arrangementId = "arrangement-123";
        int size = 10;

        WebClientResponseException.NotFound notFoundException = mock(WebClientResponseException.NotFound.class);

        when(transactionPresentationServiceApi.getTransactions(
            isNull(), isNull(), isNull(), isNull(), isNull(),
            isNull(), isNull(), isNull(), isNull(), isNull(),
            isNull(), isNull(), isNull(), isNull(), isNull(),
            isNull(), isNull(), isNull(), isNull(), isNull(),
            eq(arrangementId), isNull(), isNull(), isNull(), isNull(),
            isNull(), isNull(), isNull(), eq(size), isNull(),
            isNull(), isNull()
        )).thenReturn(Flux.error(notFoundException));

        // When
        Flux<TransactionItem> result = transactionService.getLatestTransactions(arrangementId, size);

        // Then
        StepVerifier.create(result)
            .verifyComplete();
    }

    @Test
    void getLatestTransactions_shouldPropagateOtherErrors() {
        // Given
        String arrangementId = "arrangement-123";
        int size = 10;

        RuntimeException runtimeException = new RuntimeException("Unexpected error");

        when(transactionPresentationServiceApi.getTransactions(
            isNull(), isNull(), isNull(), isNull(), isNull(),
            isNull(), isNull(), isNull(), isNull(), isNull(),
            isNull(), isNull(), isNull(), isNull(), isNull(),
            isNull(), isNull(), isNull(), isNull(), isNull(),
            eq(arrangementId), isNull(), isNull(), isNull(), isNull(),
            isNull(), isNull(), isNull(), eq(size), isNull(),
            isNull(), isNull()
        )).thenReturn(Flux.error(runtimeException));

        // When
        Flux<TransactionItem> result = transactionService.getLatestTransactions(arrangementId, size);

        // Then
        StepVerifier.create(result)
            .expectError(RuntimeException.class)
            .verify();
    }

    @Test
    void deleteTransactions_shouldCallPostDelete() {
        // Given
        TransactionsDeleteRequestBody deleteRequest1 = new TransactionsDeleteRequestBody();
        TransactionsDeleteRequestBody deleteRequest2 = new TransactionsDeleteRequestBody();
        Flux<TransactionsDeleteRequestBody> deleteRequests = Flux.just(deleteRequest1, deleteRequest2);

        when(transactionPresentationServiceApi.postDelete(anyList(), isNull(), isNull(), isNull()))
            .thenReturn(Mono.empty());

        // When
        Mono<Void> result = transactionService.deleteTransactions(deleteRequests);

        // Then
        StepVerifier.create(result)
            .verifyComplete();

        verify(transactionPresentationServiceApi, times(1))
            .postDelete(eq(List.of(deleteRequest1, deleteRequest2)), isNull(), isNull(), isNull());
    }

    @Test
    void getTransactions_shouldCallGetTransactionsWithAllParameters() {
        // Given
        TransactionsQuery query = new TransactionsQuery();
        query.setArrangementId("arrangement-123");
        query.setSize(20);
        query.setBookingDateGreaterThan("2023-01-01");
        query.setBookingDateLessThan("2023-12-31");
        query.setAmountGreaterThan(new BigDecimal("100.00"));
        query.setAmountLessThan(new BigDecimal("1000.00"));

        TransactionItem transaction = new TransactionItem().id("tx-1");

        when(transactionPresentationServiceApi.getTransactions(
            eq("2023-01-01"), eq("2023-12-31"), isNull(), isNull(), isNull(),
            eq(new BigDecimal("100.00")), eq(new BigDecimal("1000.00")), isNull(), isNull(), isNull(),
            isNull(), isNull(), isNull(), isNull(), isNull(),
            isNull(), isNull(), isNull(), isNull(), isNull(),
            eq("arrangement-123"), isNull(), isNull(), isNull(), isNull(),
            isNull(), isNull(), isNull(), eq(20), isNull(),
            isNull(), isNull()
        )).thenReturn(Flux.just(transaction));

        // When
        Flux<TransactionItem> result = transactionService.getTransactions(query);

        // Then
        StepVerifier.create(result)
            .expectNext(transaction)
            .verifyComplete();
    }

    @Test
    void patchTransactions_shouldCallPatchTransactions() {
        // Given
        TransactionsPatchRequestBody patchRequest1 = new TransactionsPatchRequestBody();
        TransactionsPatchRequestBody patchRequest2 = new TransactionsPatchRequestBody();
        Flux<TransactionsPatchRequestBody> patchRequests = Flux.just(patchRequest1, patchRequest2);

        when(transactionPresentationServiceApi.patchTransactions(anyList(), isNull(), isNull(), isNull()))
            .thenReturn(Mono.empty());

        // When
        Mono<Void> result = transactionService.patchTransactions(patchRequests);

        // Then
        StepVerifier.create(result)
            .verifyComplete();

        verify(transactionPresentationServiceApi, times(1))
            .patchTransactions(eq(List.of(patchRequest1, patchRequest2)), isNull(), isNull(), isNull());
    }

    @Test
    void postRefresh_shouldCallPostRefresh() {
        // Given
        ArrangementItem arrangement1 = new ArrangementItem();
        arrangement1.setArrangementId("arr-1");
        ArrangementItem arrangement2 = new ArrangementItem();
        arrangement2.setArrangementId("arr-2");
        Flux<ArrangementItem> arrangements = Flux.just(arrangement1, arrangement2);

        when(transactionPresentationServiceApi.postRefresh(anyList(), isNull(), isNull(), isNull()))
            .thenReturn(Mono.empty());

        // When
        Mono<Void> result = transactionService.postRefresh(arrangements);

        // Then
        StepVerifier.create(result)
            .verifyComplete();

        verify(transactionPresentationServiceApi, times(1))
            .postRefresh(eq(List.of(arrangement1, arrangement2)), isNull(), isNull(), isNull());
    }

    @Test
    void processTransactions_shouldHandleMultipleTransactions() {
        // Given
        TransactionsPostRequestBody transaction1 = new TransactionsPostRequestBody();
        TransactionsPostRequestBody transaction2 = new TransactionsPostRequestBody();
        TransactionsPostRequestBody transaction3 = new TransactionsPostRequestBody();
        Flux<TransactionsPostRequestBody> transactions = Flux.just(transaction1, transaction2, transaction3);

        TransactionTask task1 = new TransactionTask("id-1", Collections.singletonList(transaction1));
        TransactionTask task2 = new TransactionTask("id-2", Collections.singletonList(transaction2));
        TransactionTask task3 = new TransactionTask("id-3", Collections.singletonList(transaction3));

        UnitOfWork<TransactionTask> unitOfWork1 = UnitOfWork.from("id-1", task1);
        UnitOfWork<TransactionTask> unitOfWork2 = UnitOfWork.from("id-2", task2);
        UnitOfWork<TransactionTask> unitOfWork3 = UnitOfWork.from("id-3", task3);

        when(transactionTaskExecutor.prepareUnitOfWork(transactions))
            .thenReturn(Flux.just(unitOfWork1, unitOfWork2, unitOfWork3));
        when(transactionTaskExecutor.executeUnitOfWork(any()))
            .thenReturn(Mono.just(unitOfWork1), Mono.just(unitOfWork2), Mono.just(unitOfWork3));

        // When
        Flux<UnitOfWork<TransactionTask>> result = transactionService.processTransactions(transactions);

        // Then
        StepVerifier.create(result)
            .expectNext(unitOfWork1)
            .expectNext(unitOfWork2)
            .expectNext(unitOfWork3)
            .verifyComplete();
    }

    @Test
    void deleteTransactions_shouldHandleEmptyFlux() {
        // Given
        Flux<TransactionsDeleteRequestBody> emptyFlux = Flux.empty();

        when(transactionPresentationServiceApi.postDelete(anyList(), isNull(), isNull(), isNull()))
            .thenReturn(Mono.empty());

        // When
        Mono<Void> result = transactionService.deleteTransactions(emptyFlux);

        // Then
        StepVerifier.create(result)
            .verifyComplete();

        verify(transactionPresentationServiceApi, times(1))
            .postDelete(eq(List.of()), isNull(), isNull(), isNull());
    }

    @Test
    void patchTransactions_shouldHandleEmptyFlux() {
        // Given
        Flux<TransactionsPatchRequestBody> emptyFlux = Flux.empty();

        when(transactionPresentationServiceApi.patchTransactions(anyList(), isNull(), isNull(), isNull()))
            .thenReturn(Mono.empty());

        // When
        Mono<Void> result = transactionService.patchTransactions(emptyFlux);

        // Then
        StepVerifier.create(result)
            .verifyComplete();

        verify(transactionPresentationServiceApi, times(1))
            .patchTransactions(eq(List.of()), isNull(), isNull(), isNull());
    }

    @Test
    void postRefresh_shouldHandleEmptyFlux() {
        // Given
        Flux<ArrangementItem> emptyFlux = Flux.empty();

        when(transactionPresentationServiceApi.postRefresh(anyList(), isNull(), isNull(), isNull()))
            .thenReturn(Mono.empty());

        // When
        Mono<Void> result = transactionService.postRefresh(emptyFlux);

        // Then
        StepVerifier.create(result)
            .verifyComplete();

        verify(transactionPresentationServiceApi, times(1))
            .postRefresh(eq(List.of()), isNull(), isNull(), isNull());
    }
}

