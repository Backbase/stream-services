package com.backbase.stream.compositions.transaction.core.service.impl;

import com.backbase.stream.TransactionService;
import com.backbase.stream.compositions.integration.transaction.model.TransactionsPostRequestBody;
import com.backbase.stream.compositions.transaction.core.mapper.TransactionMapper;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestPullRequest;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestPushRequest;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestResponse;
import com.backbase.stream.compositions.transaction.core.service.TransactionIngestionService;
import com.backbase.stream.compositions.transaction.core.service.TransactionIntegrationService;
import com.backbase.stream.transaction.TransactionTask;
import com.backbase.stream.worker.model.UnitOfWork;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionIngestionServiceImplTest {
    private TransactionIngestionService transactionIngestionService;

    @Mock
    private TransactionIntegrationService transactionIntegrationService;

    @Mock
    TransactionMapper mapper;

    @Mock
    TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionIngestionService = new TransactionIngestionServiceImpl(
                mapper,
                transactionService,
                transactionIntegrationService);
    }

    @Test
    @Disabled
    void ingestionInPullMode_Success() {
        Mono<TransactionIngestPullRequest> transactionIngestPullRequest = Mono.just(TransactionIngestPullRequest.builder()
                .externalArrangementIds("externalId1,externalId2")
                .build());
        TransactionsPostRequestBody transaction = new TransactionsPostRequestBody();

        when(transactionIntegrationService.pullTransactions(transactionIngestPullRequest.block()))
                .thenReturn(Flux.just(transaction));

        when(mapper.mapIntegrationToStream(transaction))
                .thenReturn(new com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostRequestBody());

        UnitOfWork<TransactionTask> unitOfWork = UnitOfWork.from("id1", Arrays.asList(new TransactionTask(null, null)));

        when(transactionService.processTransactions(any(Flux.class)))
                .thenReturn(Flux.just(unitOfWork));

        TransactionIngestResponse productIngestResponse = transactionIngestionService.ingestPull(transactionIngestPullRequest).block();
        assertNotNull(productIngestResponse);
        assertNotNull(productIngestResponse.getTransactions());
    }

    @Test
    void ingestionInPushMode_Unsupported() {
        Mono<TransactionIngestPushRequest> request = Mono.just(TransactionIngestPushRequest.builder().build());
        assertThrows(UnsupportedOperationException.class, () -> {
            transactionIngestionService.ingestPush(request);
        });
    }
}
