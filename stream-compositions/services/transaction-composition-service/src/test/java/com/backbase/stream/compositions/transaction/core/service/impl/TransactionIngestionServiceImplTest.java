package com.backbase.stream.compositions.transaction.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostResponseBody;
import com.backbase.stream.TransactionService;
import com.backbase.stream.compositions.transaction.core.config.TransactionConfigurationProperties;
import com.backbase.stream.compositions.transaction.core.config.TransactionConfigurationProperties.Cursor;
import com.backbase.stream.compositions.transaction.core.config.TransactionConfigurationProperties.Events;
import com.backbase.stream.compositions.transaction.core.mapper.TransactionMapper;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestPullRequest;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestPushRequest;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestResponse;
import com.backbase.stream.compositions.transaction.core.service.TransactionIngestionService;
import com.backbase.stream.compositions.transaction.core.service.TransactionIntegrationService;
import com.backbase.stream.compositions.transaction.core.service.TransactionPostIngestionService;
import com.backbase.stream.compositions.transaction.cursor.client.TransactionCursorApi;
import com.backbase.stream.compositions.transaction.cursor.client.model.TransactionCursor;
import com.backbase.stream.compositions.transaction.cursor.client.model.TransactionCursor.StatusEnum;
import com.backbase.stream.compositions.transaction.cursor.client.model.TransactionCursorResponse;
import com.backbase.stream.compositions.transaction.cursor.client.model.TransactionCursorUpsertResponse;
import com.backbase.stream.compositions.transaction.integration.client.model.TransactionsPostRequestBody;
import com.backbase.stream.transaction.TransactionTask;
import com.backbase.stream.worker.model.UnitOfWork;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class TransactionIngestionServiceImplTest {

    private TransactionIngestionService transactionIngestionService;

    @Mock
    private TransactionIntegrationService transactionIntegrationService;

    TransactionPostIngestionService transactionPostIngestionService;

    TransactionConfigurationProperties config = new TransactionConfigurationProperties();

    @Mock
    EventBus eventBus;

    @Mock
    TransactionService transactionService;
    @Mock
    TransactionCursorApi transactionCursorApi;

    TransactionMapper mapper = Mappers.getMapper(TransactionMapper.class);

    @BeforeEach
    void setUp() {
        transactionPostIngestionService = new TransactionPostIngestionServiceImpl(eventBus, config);

        transactionIngestionService = new TransactionIngestionServiceImpl(mapper,
                transactionService, transactionIntegrationService, transactionPostIngestionService,
                transactionCursorApi, config);

    }

    void mockConfigForTransaction() {

        config.setDefaultStartOffsetInDays(30);
        Events events = new Events();
        events.setEnableCompleted(Boolean.TRUE);
        events.setEnableFailed(Boolean.TRUE);
        config.setEvents(events);
        Cursor cursor = new Cursor();
        cursor.setEnabled(Boolean.TRUE);
        cursor.setTransactionIdsFilterEnabled(Boolean.TRUE);
        config.setCursor(cursor);

    }

    void mockCursorApiForTransactions(TransactionCursorResponse transactionCursorResponse,
                                      boolean isNewCursor) {
        if (isNewCursor) {
            when(transactionCursorApi.getByArrangementId(any()))
                    .thenReturn(Mono.empty());
            when(transactionCursorApi.getById(any()))
                    .thenReturn(Mono.just(mockTransactionCursorResponse()));
        } else {
            when(transactionCursorApi.getByArrangementId(any()))
                    .thenReturn(Mono.just(transactionCursorResponse));
        }
        when(transactionCursorApi.upsertCursor(any()))
                .thenReturn(Mono.just(
                        new TransactionCursorUpsertResponse().withId("7337f8cc-d66d-41b3-a00e-f71ff15d93cg")));
        when(transactionCursorApi.patchByArrangementId(anyString(), any())).thenReturn(Mono.empty());
    }

    TransactionIngestPullRequest mockTransactionIngestPullRequest() {
        return
                TransactionIngestPullRequest.builder()
                        .arrangementId("4337f8cc-d66d-41b3-a00e-f71ff15d93cg")
                        .billingCycles(3)
                        .externalArrangementId("externalArrangementId")
                        .legalEntityInternalId("leInternalId")
                        .lastIngestedExternalIds(List.of("ext1", "ext2"))
                        .build();
    }

    TransactionCursorResponse mockTransactionCursorResponse() {
        return
                new TransactionCursorResponse()
                        .withCursor(new TransactionCursor().withId("1").withStatus(StatusEnum.IN_PROGRESS)
                                .withArrangementId("4337f8cc-d66d-41b3-a00e-f71ff15d93cg")
                                .withLegalEntityId("leInternalId"));
    }

    void mockTransactionService() {
        List<TransactionsPostResponseBody> transactionsPostResponses =
                List.of(new TransactionsPostResponseBody().id("1")
                        .externalId("externalId").additions(Map.of()));

        TransactionTask dbsResTask = new TransactionTask("id", null);
        dbsResTask.setResponse(transactionsPostResponses);

        when(transactionService.processTransactions(any())).thenReturn(
                Flux.just(UnitOfWork.from("id", dbsResTask)));
    }

    @Test
    void ingestionInPullMode_Success() {
        mockConfigForTransaction();
        TransactionCursorResponse transactionCursorResponse = mockTransactionCursorResponse();
        mockCursorApiForTransactions(transactionCursorResponse, false);
        mockTransactionService();
        TransactionIngestPullRequest transactionIngestPullRequest = mockTransactionIngestPullRequest();

        when(transactionIntegrationService.pullTransactions(transactionIngestPullRequest))
                .thenReturn(Flux.just(new TransactionsPostRequestBody().withType("type1").
                        withArrangementId("1234").withReference("ref")
                        .withExternalArrangementId("externalArrId")));

        Mono<TransactionIngestResponse> productIngestResponse = transactionIngestionService
                .ingestPull(transactionIngestPullRequest);
        StepVerifier.create(productIngestResponse)
                .assertNext(Assertions::assertNotNull).verifyComplete();

    }

    @Test
    void ingestionInPullMode_Failure() {
        mockConfigForTransaction();
        mockCursorApiForTransactions(new TransactionCursorResponse(), false);
        Mono<TransactionIngestResponse> productIngestResponse = transactionIngestionService
                .ingestPull(mockTransactionIngestPullRequest());
        StepVerifier.create(productIngestResponse)
                .verifyComplete();
    }

    @Test
    void ingestionInPullModePatchCursor_Success() {

        mockConfigForTransaction();
        mockTransactionService();
        when(transactionCursorApi.patchByArrangementId(anyString(), any())).thenReturn(Mono.empty());

        TransactionIngestPullRequest transactionIngestPullRequest = mockTransactionIngestPullRequest();
        transactionIngestPullRequest.setDateRangeStart(OffsetDateTime.now());

        when(transactionIntegrationService.pullTransactions(transactionIngestPullRequest))
                .thenReturn(Flux.just(new TransactionsPostRequestBody().withType("type1").
                        withArrangementId("1234").withReference("ref")
                        .withExternalArrangementId("externalArrId")));

        Mono<TransactionIngestResponse> productIngestResponse = transactionIngestionService
                .ingestPull(transactionIngestPullRequest);
        StepVerifier.create(productIngestResponse)
                .assertNext(Assertions::assertNotNull).verifyComplete();
    }

    @Test
    void ingestionInPullModeCursorWithDates_Success() {

        mockConfigForTransaction();
        mockTransactionService();
        TransactionCursorResponse transactionCursorResponse = mockTransactionCursorResponse();
        transactionCursorResponse.getCursor().withLastTxnDate("2022-05-24T03:18:59+01:00")
                .withLastTxnIds(List.of("123", "345"));
        mockCursorApiForTransactions(transactionCursorResponse, false);
        TransactionIngestPullRequest transactionIngestPullRequest = mockTransactionIngestPullRequest();

        when(transactionIntegrationService.pullTransactions(transactionIngestPullRequest))
                .thenReturn(Flux.just(new TransactionsPostRequestBody().withType("type1").
                        withArrangementId("1234").withReference("ref")
                        .withExternalArrangementId("externalArrId")));

        Mono<TransactionIngestResponse> productIngestResponse = transactionIngestionService
                .ingestPull(transactionIngestPullRequest);
        StepVerifier.create(productIngestResponse)
                .assertNext(Assertions::assertNotNull).verifyComplete();

    }

    @Test
    void ingestionInPullModeUpsertCursor_Success() {
        mockConfigForTransaction();
        mockTransactionService();
        TransactionCursorResponse transactionCursorResponse = mockTransactionCursorResponse();
        mockCursorApiForTransactions(transactionCursorResponse, true);
        TransactionIngestPullRequest transactionIngestPullRequest = mockTransactionIngestPullRequest();

        when(transactionIntegrationService.pullTransactions(transactionIngestPullRequest))
                .thenReturn(Flux.just(new TransactionsPostRequestBody().withType("type1").
                        withArrangementId("1234").withReference("ref")
                        .withExternalArrangementId("externalArrId")));

        Mono<TransactionIngestResponse> productIngestResponse = transactionIngestionService
                .ingestPull(transactionIngestPullRequest);
        StepVerifier.create(productIngestResponse)
                .assertNext(Assertions::assertNotNull).verifyComplete();
    }

    @Test
    void ingestionInPushMode_Unsupported() {
        TransactionIngestPushRequest request = TransactionIngestPushRequest.builder().build();
        assertThrows(UnsupportedOperationException.class, () -> {
            transactionIngestionService.ingestPush(request);
        });
    }
}
