package com.backbase.stream.portfolio.service.impl;

import static com.backbase.stream.LambdaAssertions.assertEqualsTo;
import static org.mockito.ArgumentMatchers.any;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import com.backbase.stream.portfolio.configuration.PortfolioSagaProperties;
import com.backbase.stream.portfolio.model.AllocationBundle;
import com.backbase.stream.portfolio.model.WealthPortfolioAllocationsBundle;
import com.backbase.stream.portfolio.saga.wealth.allocation.WealthPortfolioAllocationsSaga;
import com.backbase.stream.portfolio.util.PortfolioTestUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * WealthPortfolioAllocationsReactiveService Test.
 * 
 * @author Vladimir Kirchev
 *
 */
@ExtendWith(MockitoExtension.class)
class WealthPortfolioAllocationsReactiveServiceTest {
    @Mock
    private PortfolioSagaProperties portfolioSagaProperties;

    @Mock
    private WealthPortfolioAllocationsSaga wealthPortfolioAllocationsSaga;

    @InjectMocks
    private WealthPortfolioAllocationsReactiveService wealthPortfolioAllocationsReactiveService;

    @Test
    void shouldIngestPortfolioAllocationBundles() throws Exception {
        WealthPortfolioAllocationsBundle wealthPortfolioAllocationsBundle =
                PortfolioTestUtil.getWealthPortfolioAllocationsBundle();
        List<AllocationBundle> batchPortfolioAllocations =
                wealthPortfolioAllocationsBundle.getBatchPortfolioAllocations();

        AllocationBundle allocationBundle0 = batchPortfolioAllocations.get(0);
        AllocationBundle allocationBundle1 = batchPortfolioAllocations.get(1);

        Mockito.when(portfolioSagaProperties.getTaskExecutors()).thenReturn(1);
        Mockito.when(wealthPortfolioAllocationsSaga.executeTask(any())).thenAnswer(i -> Mono.just(i.getArgument(0)));

        Flux<AllocationBundle> ingestedPortfolioAllocationBundles = wealthPortfolioAllocationsReactiveService
                .ingestPortfolioAllocationBundles(Flux.fromIterable(batchPortfolioAllocations));

        Assertions.assertNotNull(ingestedPortfolioAllocationBundles);

        StepVerifier.create(ingestedPortfolioAllocationBundles).assertNext(assertEqualsTo(allocationBundle0))
                .assertNext(assertEqualsTo(allocationBundle1)).verifyComplete();
    }
}
