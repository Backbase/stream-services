package com.backbase.stream;

import com.backbase.dbs.transaction.api.service.v3.TransactionPresentationServiceApi;
import com.backbase.dbs.transaction.api.service.v3.model.TransactionItem;
import com.backbase.stream.transaction.TransactionUnitOfWorkExecutor;
import com.backbase.stream.transaction.TransactionsQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Mock
    private TransactionPresentationServiceApi transactionPresentationServiceApi;

    @Mock
    private TransactionUnitOfWorkExecutor transactionUnitOfWorkExecutor;

    @Test
    void getLatestTransactions_Success() {
        // Given
        String arrangementId = "arrangement-123";
        int size = 10;
        String bookingDateGreaterThan = "2024-01-01T00:00:00Z";
        String bookingDateLessThan = "2024-12-31T23:59:59Z";

        TransactionItem transactionItem1 = new TransactionItem()
            .id("txn-1")
            .arrangementId(arrangementId)
            .description("Transaction 1");

        TransactionItem transactionItem2 = new TransactionItem()
            .id("txn-2")
            .arrangementId(arrangementId)
            .description("Transaction 2");

        when(transactionPresentationServiceApi.getTransactions(
            eq(bookingDateGreaterThan),
            eq(bookingDateLessThan),
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            eq(arrangementId),
            any(), any(), any(), any(), any(), any(), any(), eq(Integer.valueOf(size)), any(), any(), any()
        )).thenReturn(Flux.just(transactionItem1, transactionItem2));

        // When
        Flux<TransactionItem> result = transactionService.getLatestTransactions(
            arrangementId, size, bookingDateGreaterThan, bookingDateLessThan);

        // Then
        StepVerifier.create(result)
            .expectNext(transactionItem1)
            .expectNext(transactionItem2)
            .verifyComplete();
    }

    @Test
    void getLatestTransactions_NotFound_ReturnsEmpty() {
        // Given
        String arrangementId = "arrangement-123";
        int size = 10;
        String bookingDateGreaterThan = "2024-01-01T00:00:00Z";
        String bookingDateLessThan = "2024-12-31T23:59:59Z";

        WebClientResponseException notFoundException = 
            WebClientResponseException.NotFound.create(
                404, "Not Found", new HttpHeaders(), new byte[0], null);

        when(transactionPresentationServiceApi.getTransactions(
            eq(bookingDateGreaterThan),
            eq(bookingDateLessThan),
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            eq(arrangementId),
            any(), any(), any(), any(), any(), any(), any(), eq(Integer.valueOf(size)), any(), any(), any()
        )).thenReturn(Flux.error(notFoundException));

        // When
        Flux<TransactionItem> result = transactionService.getLatestTransactions(
            arrangementId, size, bookingDateGreaterThan, bookingDateLessThan);

        // Then
        StepVerifier.create(result)
            .verifyComplete();
    }

    @Test
    void getLatestTransactions_WhenErrorOccurs_PropagatesError() {
        // Given
        String arrangementId = "arrangement-123";
        int size = 10;
        String bookingDateGreaterThan = "2024-01-01T00:00:00Z";
        String bookingDateLessThan = "2024-12-31T23:59:59Z";

        RuntimeException runtimeException = new RuntimeException("Internal server error");

        when(transactionPresentationServiceApi.getTransactions(
            eq(bookingDateGreaterThan),
            eq(bookingDateLessThan),
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            eq(arrangementId),
            any(), any(), any(), any(), any(), any(), any(), eq(Integer.valueOf(size)), any(), any(), any()
        )).thenReturn(Flux.error(runtimeException));

        // When
        Flux<TransactionItem> result = transactionService.getLatestTransactions(
            arrangementId, size, bookingDateGreaterThan, bookingDateLessThan);

        // Then
        StepVerifier.create(result)
            .verifyError(RuntimeException.class);
    }

    @Test
    void getTransactions_Success() {
        // Given
        String bookingDateGreaterThan = "2024-01-01T00:00:00Z";
        String bookingDateLessThan = "2024-12-31T23:59:59Z";
        
        TransactionsQuery query = new TransactionsQuery();
        query.setArrangementId("arrangement-123");
        query.setSize(10);
        query.setBookingDateGreaterThan(bookingDateGreaterThan);
        query.setBookingDateLessThan(bookingDateLessThan);

        TransactionItem transactionItem = new TransactionItem()
            .id("txn-1")
            .arrangementId("arrangement-123");

        when(transactionPresentationServiceApi.getTransactions(
            eq(bookingDateGreaterThan),
            eq(bookingDateLessThan),
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            eq(query.getArrangementId()),
            any(), any(), any(), any(), any(), any(), any(), eq(query.getSize()), any(), any(), any()
        )).thenReturn(Flux.just(transactionItem));

        // When
        Flux<TransactionItem> result = transactionService.getTransactions(query);

        // Then
        StepVerifier.create(result)
            .expectNext(transactionItem)
            .verifyComplete();
    }
}

