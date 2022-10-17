package com.backbase.stream.portfolio.saga.wealth.allocation;

import static com.backbase.stream.LambdaAssertions.assertEqualsTo;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import com.backbase.stream.portfolio.model.Allocation;
import com.backbase.stream.portfolio.model.AllocationBundle;
import com.backbase.stream.portfolio.model.WealthPortfolioAllocationsBundle;
import com.backbase.stream.portfolio.service.PortfolioIntegrationService;
import com.backbase.stream.portfolio.util.PortfolioTestUtil;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * WealthPortfolioAllocationsSaga Test.
 * 
 * @author Vladimir Kirchev
 *
 */
@ExtendWith(MockitoExtension.class)
class WealthPortfolioAllocationsSagaTest {

    @Mock
    private PortfolioIntegrationService portfolioIntegrationService;;

    @InjectMocks
    private WealthPortfolioAllocationsSaga wealthPortfolioAllocationsSaga;

    @Test
    void shouldExecuteTask() throws Exception {
        WealthPortfolioAllocationsBundle wealthPortfolioAllocationsBundle =
                PortfolioTestUtil.getWealthPortfolioAllocationsBundle();
        List<AllocationBundle> batchPortfolioAllocations =
                wealthPortfolioAllocationsBundle.getBatchPortfolioAllocations();
        AllocationBundle allocationBundle = batchPortfolioAllocations.get(0);
        WealthPortfolioAllocationsTask wealthPortfolioAllocationsTask =
                new WealthPortfolioAllocationsTask(allocationBundle);
        List<Allocation> allocations = allocationBundle.getAllocations();
        String portfolioCode = allocationBundle.getPortfolioCode();

        Mockito.when(portfolioIntegrationService.upsertAllocations(allocations, portfolioCode))
                .thenReturn(Mono.just(allocations));

        Mono<WealthPortfolioAllocationsTask> task = wealthPortfolioAllocationsSaga.executeTask(wealthPortfolioAllocationsTask);

        Assertions.assertNotNull(task);

        StepVerifier.create(task).assertNext(assertEqualsTo(wealthPortfolioAllocationsTask)).verifyComplete();
    }

    @Test
    void shouldRollBack() throws Exception {
        WealthPortfolioAllocationsBundle wealthPortfolioAllocationsBundle =
                PortfolioTestUtil.getWealthPortfolioAllocationsBundle();
        List<AllocationBundle> batchPortfolioAllocations =
                wealthPortfolioAllocationsBundle.getBatchPortfolioAllocations();
        AllocationBundle allocationBundle = batchPortfolioAllocations.get(0);
        WealthPortfolioAllocationsTask wealthPortfolioAllocationsTask =
                new WealthPortfolioAllocationsTask(allocationBundle);

        Mono<WealthPortfolioAllocationsTask> mono = wealthPortfolioAllocationsSaga.rollBack(wealthPortfolioAllocationsTask);

        Assertions.assertNotNull(mono);

        StepVerifier.create(mono).assertNext(assertEqualsTo(wealthPortfolioAllocationsTask)).verifyComplete();
    }
}
