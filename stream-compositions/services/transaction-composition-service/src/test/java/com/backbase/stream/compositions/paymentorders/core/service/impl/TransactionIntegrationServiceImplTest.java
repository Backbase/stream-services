package com.backbase.stream.compositions.paymentorders.core.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.backbase.stream.compositions.paymentorders.core.mapper.TransactionMapper;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestPullRequest;
import com.backbase.stream.compositions.paymentorders.integration.client.TransactionIntegrationApi;
import com.backbase.stream.compositions.paymentorders.integration.client.model.PullTransactionsResponse;
import com.backbase.stream.compositions.paymentorders.integration.client.model.TransactionsPostRequestBody;
import java.util.List;
import java.util.Map;

import com.backbase.stream.compositions.transaction.core.service.impl.TransactionIntegrationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class TransactionIntegrationServiceImplTest {

  @Mock
  private TransactionIntegrationApi transactionIntegrationApi;

  @Mock
  private TransactionMapper transactionMapper;

  private TransactionIntegrationServiceImpl transactionIntegrationService;

  @BeforeEach
  void setUp() {
    transactionIntegrationService = new TransactionIntegrationServiceImpl(transactionIntegrationApi,
        transactionMapper);
  }

  @Test
  void callIntegrationService_Success() {

    TransactionsPostRequestBody transactionsPostRequestBody =
        new TransactionsPostRequestBody().withArrangementId("1234");

    when(transactionIntegrationApi.pullTransactions(any()))
        .thenReturn(Mono.just(new PullTransactionsResponse()
            .withTransactions(List.of(new TransactionsPostRequestBody()
                .withArrangementId("1234")))));

    TransactionIngestPullRequest transactionIngestPullRequest =
        new TransactionIngestPullRequest("1234",
            "", "", Map.of(), null, null, 3, null);

    StepVerifier
        .create(transactionIntegrationService.pullTransactions(transactionIngestPullRequest))
        .expectNext(transactionsPostRequestBody)
        .expectComplete();

  }
}
