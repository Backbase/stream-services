package com.backbase.stream.compositions.product.core.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.dbs.transaction.api.service.v3.TransactionPresentationServiceApi;
import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties;
import com.backbase.stream.compositions.transaction.client.model.TransactionPullIngestionRequest;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.StepVerifier.Step;

@ExtendWith(MockitoExtension.class)
public class TransactionIngestionServiceImplTest {

    @Mock
    private TransactionPresentationServiceApi transactionManagerApi;
    @Spy
    private ProductConfigurationProperties properties;
    @InjectMocks
    private TransactionIngestionServiceImpl target;

    @BeforeEach
    void init() {
        properties.getChains().getTransactionManager().setEnabled(true);
        when(transactionManagerApi.postRefresh(any(), any(), any(), any())).thenReturn(Mono.empty());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ingestTransactionsViaTransactionManagerTest(boolean splitPerArrangement) {
        properties.getChains().getTransactionManager().setSplitPerArrangement(splitPerArrangement);

        var request1 = new TransactionPullIngestionRequest()
            .externalArrangementId("id1");
        var request2 = new TransactionPullIngestionRequest()
            .externalArrangementId("id2");

        StepVerifier.create(target.ingestTransactions(Flux.fromIterable(List.of(request1, request2))))
            .verifyComplete();

        assertExecutions(splitPerArrangement);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ingestTransactionsAsyncViaTransactionManagerTest(boolean splitPerArrangement) {
        properties.getChains().getTransactionManager().setSplitPerArrangement(splitPerArrangement);

        var request1 = new TransactionPullIngestionRequest()
            .externalArrangementId("id1");
        var request2 = new TransactionPullIngestionRequest()
            .externalArrangementId("id2");

        Step<?> step = StepVerifier.create(
                target.ingestTransactionsAsync(Flux.fromIterable(List.of(request1, request2))))
            .assertNext(Assertions::assertNotNull);

        if (splitPerArrangement) {
            step.assertNext(Assertions::assertNotNull).verifyComplete();
        } else {
            step.verifyComplete();
        }

        assertExecutions(splitPerArrangement);
    }

    private void assertExecutions(boolean splitPerArrangement) {
        if (splitPerArrangement) {
            verify(transactionManagerApi, times(2))
                .postRefresh(argThat(a -> a.size() == 1), any(), any(), any());
        } else {
            verify(transactionManagerApi, times(1))
                .postRefresh(argThat(a -> a.size() == 2), any(), any(), any());
        }
    }

}
