package com.backbase.stream.portfolio.service.impl;

import java.util.List;
import com.backbase.stream.portfolio.configuration.PortfolioSagaProperties;
import com.backbase.stream.portfolio.model.AllocationBundle;
import com.backbase.stream.portfolio.model.HierarchyBundle;
import com.backbase.stream.portfolio.model.Portfolio;
import com.backbase.stream.portfolio.model.SubPortfolioBundle;
import com.backbase.stream.portfolio.model.TransactionCategory;
import com.backbase.stream.portfolio.model.ValuationsBundle;
import com.backbase.stream.portfolio.service.PortfolioIngestionService;
import com.backbase.stream.portfolio.service.PortfolioIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive implementation of {@code InstrumentIngestionService}.
 * 
 * @author Vladimir Kirchev
 *
 */
@Slf4j
@RequiredArgsConstructor
public class PortfolioIngestionReactiveService implements PortfolioIngestionService {
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

    @Override
    public Flux<Portfolio> ingestWealthPortfolios(Flux<Portfolio> portfolio) {
        return portfolio
                .flatMap(portfolioIntegrationService::upsertPortfolio, portfolioSagaProperties.getTaskExecutors())
                .doOnNext(actual -> log.info("Finished Ingestion of portfolio, name: {}", actual.getName()));
    }

    @Override
    public Flux<ValuationsBundle> ingestValuationsBundles(Flux<ValuationsBundle> valuationsBundles) {
        return valuationsBundles.flatMap(this::upsertValuationsBundle, portfolioSagaProperties.getTaskExecutors())
                .doOnNext(actual -> log.info("Finished Ingestion of Valuations Bundle, portfolioCode: {}",
                        actual.getPortfolioCode()));
    }

    private Mono<ValuationsBundle> upsertValuationsBundle(ValuationsBundle valuationsBundle) {
        return portfolioIntegrationService
                .upsertPortfolioValuations(valuationsBundle.getValuations(), valuationsBundle.getPortfolioCode())
                .map(m -> valuationsBundle);
    }

    @Override
    public Flux<SubPortfolioBundle> ingestWealthSubPortfolios(Flux<SubPortfolioBundle> subPortfolioBundles) {
        return subPortfolioBundles.flatMap(this::upsertSubPortfolios, portfolioSagaProperties.getTaskExecutors())
                .doOnNext(actual -> log.info("Finished Ingestion of subPortfolio, portfolioCode: {}",
                        actual.getPortfolioCode()));
    }

    private Mono<SubPortfolioBundle> upsertSubPortfolios(SubPortfolioBundle subPortfolioBundle) {
        return portfolioIntegrationService
                .upsertSubPortfolios(subPortfolioBundle.getSubPortfolios(), subPortfolioBundle.getPortfolioCode())
                .map(m -> subPortfolioBundle);
    }

    @Override
    public Flux<TransactionCategory> ingestTransactionCategories(Flux<TransactionCategory> transactionCategories) {
        return transactionCategories.map(List::of)
                .flatMap(portfolioIntegrationService::upsertTransactionCategories,
                        portfolioSagaProperties.getTaskExecutors())
                .flatMapIterable(i -> i)
                .doOnNext(actual -> log.info("Finished Ingestion of Transaction Category, key: {}", actual.getKey()));
    }
    
    @Override
    public Flux<HierarchyBundle> ingestHierarchyBundles(Flux<HierarchyBundle> hierarchyBundles) {
        return hierarchyBundles.flatMap(this::upsertHierarchyBundle, portfolioSagaProperties.getTaskExecutors())
                .doOnNext(actual -> log.info("Finished Ingestion of hierarchy bundle, portfolioCode: {}",
                                             actual.getPortfolioCode()));
    }

    private Mono<HierarchyBundle> upsertHierarchyBundle(HierarchyBundle hierarchyBundle) {
        return portfolioIntegrationService
                .upsertHierarchies(hierarchyBundle.getHierarchies(), hierarchyBundle.getPortfolioCode())
                .map(m -> hierarchyBundle);
    }

}
