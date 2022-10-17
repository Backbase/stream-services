package com.backbase.stream.portfolio.service.impl;

import com.backbase.stream.portfolio.configuration.PortfolioSagaProperties;
import com.backbase.stream.portfolio.model.AllocationBundle;
import com.backbase.stream.portfolio.saga.wealth.allocation.WealthPortfolioAllocationsSaga;
import com.backbase.stream.portfolio.saga.wealth.allocation.WealthPortfolioAllocationsTask;
import com.backbase.stream.portfolio.service.WealthPortfolioAllocationsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * Reactive impelemntation of {@code WealthPortfolioAllocationsService}.
 * 
 * @author Vladimir Kirchev
 *
 */
@Slf4j
@RequiredArgsConstructor
public class WealthPortfolioAllocationsReactiveService implements WealthPortfolioAllocationsService {

    private final PortfolioSagaProperties portfolioSagaProperties;
    private final WealthPortfolioAllocationsSaga wealthPortfolioAllocationsSaga;

    @Override
    public Flux<AllocationBundle> ingestPortfolioAllocationBundles(Flux<AllocationBundle> allocationBundle) {
        return allocationBundle.map(WealthPortfolioAllocationsTask::new)
                .flatMap(wealthPortfolioAllocationsSaga::executeTask, portfolioSagaProperties.getTaskExecutors())
                .map(WealthPortfolioAllocationsTask::getData)
                .doOnNext(actual -> log.info("Finished Ingestion of portfolio allocations, portfolioCode: {}",
                        actual.getPortfolioCode()));
    }

}
