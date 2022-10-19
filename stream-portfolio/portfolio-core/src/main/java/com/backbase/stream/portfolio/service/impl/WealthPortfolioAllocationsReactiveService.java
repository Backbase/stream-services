package com.backbase.stream.portfolio.service.impl;

import com.backbase.stream.portfolio.configuration.PortfolioSagaProperties;
import com.backbase.stream.portfolio.model.AllocationBundle;
import com.backbase.stream.portfolio.service.PortfolioIntegrationService;
import com.backbase.stream.portfolio.service.WealthPortfolioAllocationsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    private final PortfolioIntegrationService portfolioIntegrationService;

    @Override
    public Flux<AllocationBundle> ingestPortfolioAllocationBundles(Flux<AllocationBundle> allocationBundle) {
        return allocationBundle.flatMap(this::upsertAllocations, portfolioSagaProperties.getTaskExecutors())
                .doOnNext(actual -> log.info("Finished Ingestion of portfolio allocations, portfolioCode: {}",
                        actual.getPortfolioCode()));
    }

    private Mono<AllocationBundle> upsertAllocations(AllocationBundle allocationBundle) {
        return portfolioIntegrationService
                .upsertAllocations(allocationBundle.getAllocations(), allocationBundle.getPortfolioCode())
                .map(m -> allocationBundle);
    }

}
