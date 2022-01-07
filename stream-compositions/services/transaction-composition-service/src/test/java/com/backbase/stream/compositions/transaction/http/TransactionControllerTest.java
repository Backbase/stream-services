package com.backbase.stream.compositions.transaction.http;

import com.backbase.stream.compositions.transaction.core.mapper.TransactionMapper;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestResponse;
import com.backbase.stream.compositions.transaction.core.service.TransactionIngestionService;
import com.backbase.stream.compositions.transaction.model.IngestionResponse;
import com.backbase.stream.compositions.transaction.model.PullIngestionRequest;
import com.backbase.stream.compositions.transaction.model.PushIngestionRequest;
import com.backbase.stream.compositions.transaction.model.TransactionsPostResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {
    @Mock
    TransactionMapper mapper;

    @Mock
    TransactionIngestionService productIngestionService;

    TransactionController controller;

    @BeforeEach
    void setUp() {
        controller = new TransactionController(
                productIngestionService,
                mapper);

        lenient().when(mapper.mapCompositionToStream(any())).thenReturn(new com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostRequestBody());
        lenient().when(mapper.mapStreamToComposition(any())).thenReturn(new TransactionsPostResponseBody());
    }

    @Test
    void testPullIngestion_Success() {
        Mono<PullIngestionRequest> requestMono = Mono.just(
                new PullIngestionRequest().withExternalArrangementIds("externalId"));

        doAnswer(invocation -> {
            Mono mono = invocation.getArgument(0);
            mono.block();

            return Mono.just(TransactionIngestResponse.builder()
                    .transactions(new ArrayList<>())
                    .build());
        }).when(productIngestionService).ingestPull(any());

        ResponseEntity<IngestionResponse> responseEntity = controller.pullIngestTransactions(requestMono, null).block();
        IngestionResponse ingestionResponse = responseEntity.getBody();
        assertNotNull(ingestionResponse);
        assertNotNull(ingestionResponse.getTransactions());
        verify(productIngestionService).ingestPull(any());
    }

    @Test
    void testPushIngestion_Success() {
        Mono<PushIngestionRequest> requestMono = Mono.just(
                new PushIngestionRequest().withTransactions(new ArrayList<>()));

        doAnswer(invocation -> {
            Mono mono = invocation.getArgument(0);
            mono.block();

            return Mono.just(TransactionIngestResponse.builder()
                    .transactions(
                            new ArrayList<>())
                    .build());
        }).when(productIngestionService).ingestPush(any());

        ResponseEntity<IngestionResponse> responseEntity = controller.pushIngestTransactions(requestMono, null).block();
        IngestionResponse ingestionResponse = responseEntity.getBody();
        assertNotNull(ingestionResponse);
        assertNotNull(ingestionResponse.getTransactions());
        verify(productIngestionService).ingestPush(any());
    }
}

