package com.backbase.stream.compositions.transaction.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostResponseBody;
import com.backbase.stream.compositions.transaction.api.model.TransactionPullIngestionRequest;
import com.backbase.stream.compositions.transaction.api.model.TransactionPushIngestionRequest;
import com.backbase.stream.compositions.transaction.api.model.TransactionsPostRequestBody;
import com.backbase.stream.compositions.transaction.core.mapper.TransactionMapper;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestResponse;
import com.backbase.stream.compositions.transaction.core.service.TransactionIngestionService;

import java.util.List;
import java.util.Map;

import com.backbase.stream.compositions.transaction.http.TransactionController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    TransactionMapper mapper = Mappers.getMapper(TransactionMapper.class);

    @Mock
    TransactionIngestionService transactionIngestionService;

    TransactionController transactionController;

    @BeforeEach
    void setUp() {
        transactionController = new TransactionController(transactionIngestionService, mapper);
    }

    @Test
    void testPullIngestion_Success() {

        Mono<TransactionPullIngestionRequest> requestMono = Mono
                .just(new TransactionPullIngestionRequest().arrangementId("arrangementId")
                        .billingCycles(3).externalArrangementId("extArrangementId")
                        .legalEntityInternalId("legalEntityId"));

        when(transactionIngestionService.ingestPull(any())).thenReturn(Mono.just(
                TransactionIngestResponse.builder()
                        .transactions(List.of(new TransactionsPostResponseBody().id("1")
                                .externalId("externalId").additions(Map.of()))).build()));

        transactionController.pullTransactions(requestMono, null).block();
        verify(transactionIngestionService).ingestPull(any());
    }

    @Test
    void testPushIngestion_Success() {
        Mono<TransactionPushIngestionRequest> requestMono = Mono.just(
                new TransactionPushIngestionRequest()
                        .transactions(List.of(new TransactionsPostRequestBody()
                                .reference("ref").type("type").arrangementId("arrangementId"))));

        when(transactionIngestionService.ingestPush(any())).thenReturn(
                Mono.just(TransactionIngestResponse.builder()
                        .transactions(List.of(new TransactionsPostResponseBody().id("1")
                                .externalId("externalId").additions(Map.of()))).build()));

        transactionController.pushIngestTransactions(requestMono, null).block();
        verify(transactionIngestionService).ingestPush(any());
    }
}
