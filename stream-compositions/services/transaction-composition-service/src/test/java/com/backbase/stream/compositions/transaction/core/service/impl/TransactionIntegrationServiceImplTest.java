package com.backbase.stream.compositions.transaction.core.service.impl;

import com.backbase.stream.compositions.integration.transaction.api.TransactionIntegrationApi;
import com.backbase.stream.compositions.integration.transaction.model.PullTransactionsResponse;
import com.backbase.stream.compositions.integration.transaction.model.TransactionsPostRequestBody;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestPullRequest;
import com.backbase.stream.compositions.transaction.core.service.TransactionIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionIntegrationServiceImplTest {
    @Mock
    private TransactionIntegrationApi transactionIntegrationApi;

    private TransactionIntegrationService transactionIntegrationService;

    @BeforeEach
    void setUp() {
        transactionIntegrationService = new TransactionIntegrationServiceImpl(transactionIntegrationApi);
    }

    @Test
    void callIntegrationService_Success() throws UnsupportedOperationException {
        List<TransactionsPostRequestBody> transactions = new ArrayList<>();
        PullTransactionsResponse getTransactionsResponse = new PullTransactionsResponse().
                transactions(transactions);

        when(transactionIntegrationApi.pullTransactions(any(), any()))
                .thenReturn(Mono.just(getTransactionsResponse));

        TransactionIngestPullRequest request = TransactionIngestPullRequest.builder().externalArrangementIds("externalId").build();

        List<TransactionsPostRequestBody> response = transactionIntegrationService.pullTransactions(request).collectList().block();
        assertEquals(transactions, response);
    }

    @Test
    void callIntegrationService_EmptyTransactionList() throws UnsupportedOperationException {
        when(transactionIntegrationApi.pullTransactions(any(), any())).thenReturn(Mono.empty());

        TransactionIngestPullRequest request = TransactionIngestPullRequest.builder().externalArrangementIds("externalId").build();
        List<TransactionsPostRequestBody> transactions = transactionIntegrationService.pullTransactions(request).collectList().block();
        assertTrue(transactions.isEmpty());
    }
}
