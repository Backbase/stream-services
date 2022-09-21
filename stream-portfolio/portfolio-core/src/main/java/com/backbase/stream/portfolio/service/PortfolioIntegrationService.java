package com.backbase.stream.portfolio.service;

import com.backbase.portfolio.api.service.integration.v1.model.PortfolioAllocationsPutRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioCumulativePerformancesPutRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioPositionsHierarchyPutRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioTransactionsPostItem;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioTransactionsPostRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioValuationsPutRequest;
import com.backbase.portfolio.integration.api.service.v1.AggregatePortfolioManagementApi;
import com.backbase.portfolio.integration.api.service.v1.PortfolioBenchmarksManagementApi;
import com.backbase.portfolio.integration.api.service.v1.PortfolioCumulativePerformanceManagementApi;
import com.backbase.portfolio.integration.api.service.v1.PortfolioManagementApi;
import com.backbase.portfolio.integration.api.service.v1.PortfolioPositionsHierarchyManagementApi;
import com.backbase.portfolio.integration.api.service.v1.PortfolioValuationManagementApi;
import com.backbase.portfolio.integration.api.service.v1.PositionManagementApi;
import com.backbase.portfolio.integration.api.service.v1.SubPortfolioManagementApi;
import com.backbase.portfolio.integration.api.service.v1.TransactionCategoryManagementApi;
import com.backbase.portfolio.integration.api.service.v1.TransactionManagementApi;
import com.backbase.stream.portfolio.mapper.PortfolioMapper;
import com.backbase.stream.portfolio.model.AggregatePortfolio;
import com.backbase.stream.portfolio.model.Portfolio;
import com.backbase.stream.portfolio.model.PortfolioBenchmark;
import com.backbase.stream.portfolio.model.PortfolioBundle;
import com.backbase.stream.portfolio.model.Position;
import com.backbase.stream.portfolio.model.PositionBundle;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@Service
public class PortfolioIntegrationService {

    private final PortfolioMapper portfolioMapper = Mappers.getMapper(PortfolioMapper.class);

    private final PortfolioManagementApi portfolioManagementApi;
    private final PositionManagementApi positionManagementApi;
    private final TransactionManagementApi transactionManagementApi;
    private final TransactionCategoryManagementApi transactionCategoryManagementApi;
    private final SubPortfolioManagementApi subPortfolioManagementApi;
    private final PortfolioPositionsHierarchyManagementApi portfolioPositionsHierarchyManagementApi;
    private final PortfolioCumulativePerformanceManagementApi portfolioCumulativePerformanceManagementApi;
    private final PortfolioBenchmarksManagementApi portfolioBenchmarksManagementApi;
    private final PortfolioValuationManagementApi portfolioValuationManagementApi;
    private final AggregatePortfolioManagementApi aggregatePortfolioManagementApi;

    /**
     * Create AggregatePortfolio.
     * @param aggregatePortfolios {@see AggregatePortfolio}
     * @return Mono {@link AggregatePortfolio}
     */
    public Mono<AggregatePortfolio> createAggregatePortfolio(AggregatePortfolio aggregatePortfolios) {
        return aggregatePortfolioManagementApi
            .postAggregatePortfolios(portfolioMapper.mapAggregate(aggregatePortfolios))
            .map(at -> aggregatePortfolios)
            .doOnError(WebClientResponseException.class, ReactiveStreamHandler::handleWebClientResponseException)
            .onErrorResume(WebClientResponseException.class,
                ReactiveStreamHandler.error(aggregatePortfolios, "Failed to create Aggregate Portfolio"))
            .onErrorStop();
    }

    /**
     * Create Positions and all reference models that are related.
     * @param positionBundle gang for positions
     * @return Mono {@link PositionBundle}
     */
    public Mono<PositionBundle> createPosition(PositionBundle positionBundle) {
        String subPortfolioId = positionBundle.getSubPortfolioId();
        String portfolioId = positionBundle.getPortfolioId();
        Position position = positionBundle.getPosition();
        //add check for instrument external id
        return positionManagementApi
            .postPositions(portfolioMapper.mapPosition(portfolioId, subPortfolioId, position))
            .thenMany(createTransactionCategory(positionBundle))
            .then(createTransactions(positionBundle, portfolioId, position))
            .map(at -> positionBundle)
            .doOnError(WebClientResponseException.class, ReactiveStreamHandler::handleWebClientResponseException)
            .onErrorResume(WebClientResponseException.class,
                ReactiveStreamHandler.error(positionBundle, "Failed to create Position gang"))
            .onErrorStop();
    }

    /**
     * Create Portfolios and all reference models that are related.
     * @param portfolioBundle gang for portfolio
     * @return Flux {@link PortfolioBundle}
     */
    public Flux<PortfolioBundle> createPortfolio(PortfolioBundle portfolioBundle) {
        Portfolio portfolio = portfolioBundle.getPortfolio();
        return portfolioManagementApi.postPortfolios(portfolioMapper.mapPortfolio(portfolio))
            .thenMany(createSubPortfolio(portfolioBundle, portfolio))
            .thenMany(createAllocation(portfolioBundle, portfolio))
            .thenMany(createHierarchy(portfolioBundle, portfolio))
            .thenMany(createPerformance(portfolioBundle, portfolio))
            .thenMany(createBenchmark(portfolioBundle, portfolio))
            .thenMany(createValuations(portfolioBundle, portfolio))
            .map(at -> portfolioBundle)
            .doOnError(WebClientResponseException.class, ReactiveStreamHandler::handleWebClientResponseException)
            .onErrorResume(WebClientResponseException.class,
                ReactiveStreamHandler.error(portfolioBundle, "Failed to create Portfolio gang"))
            .onErrorStop();
    }

    @NotNull
    private Mono<Void> createTransactions(PositionBundle positionBundle, String portfolioId, Position position) {
        return Mono.justOrEmpty(positionBundle.getTransactions())
            .flatMap(trs -> transactionManagementApi
                .postPortfolioTransactions(portfolioId, new PortfolioTransactionsPostRequest()
                    .transactions(List.of(new PortfolioTransactionsPostItem()
                        .positionId(position.getExternalId())
                        .transactions(portfolioMapper.mapTransaction(trs)))))
                .onErrorResume(WebClientResponseException.class,
                    ReactiveStreamHandler.error(trs, "Failed to create Transactions"))
                .onErrorStop()
            );
    }

    @NotNull
    private Flux<Void> createTransactionCategory(PositionBundle positionBundle) {
        return ReactiveStreamHandler.getFluxStream(positionBundle.getTransactionCategories())
            .flatMap(tc -> transactionCategoryManagementApi
                .postTransactionCategory(portfolioMapper.mapTransactionCategory(tc))
                .doOnError(WebClientResponseException.class, ReactiveStreamHandler::handleWebClientResponseException)
                .onErrorResume(WebClientResponseException.class,
                    ReactiveStreamHandler.error(tc, "Failed to create Transaction Category"))
                .onErrorStop()
            );
    }

    @NotNull
    private Flux<Void> createSubPortfolio(PortfolioBundle portfolioBundle, Portfolio portfolio) {
        return ReactiveStreamHandler.getFluxStream(portfolioBundle.getSubPortfolios())
            .flatMap(sp -> subPortfolioManagementApi
                .postSubPortfolios(portfolio.getCode(), portfolioMapper.mapSubPortfolio(sp))
                .doOnError(WebClientResponseException.class, ReactiveStreamHandler::handleWebClientResponseException)
                .onErrorResume(WebClientResponseException.class,
                    ReactiveStreamHandler.error(sp, "Failed to create Sub Portfolio"))
                .onErrorStop()
            );
    }

    @NotNull
    private Mono<Void> createAllocation(PortfolioBundle portfolioBundle, Portfolio portfolio) {
        return Mono.justOrEmpty(portfolioBundle.getAllocations())
            .flatMap(allocations -> portfolioManagementApi
                .putPortfolioAllocations(portfolio.getCode(), new PortfolioAllocationsPutRequest()
                    .allocations(portfolioMapper.mapAllocations(allocations)))
                .doOnError(WebClientResponseException.class, ReactiveStreamHandler::handleWebClientResponseException)
                .onErrorResume(WebClientResponseException.class,
                    ReactiveStreamHandler.error(allocations, "Failed to create Portfolio Allocations"))
                .onErrorStop()
            );
    }

    @NotNull
    private Mono<Void> createHierarchy(PortfolioBundle portfolioBundle,
        Portfolio portfolio) {
        return Mono.justOrEmpty(portfolioBundle.getHierarchies())
            .flatMap(hierarchies -> portfolioPositionsHierarchyManagementApi
                .putPortfolioPositionsHierarchy(portfolio.getCode(), new PortfolioPositionsHierarchyPutRequest()
                    .items(portfolioMapper.mapHierarchies(hierarchies)))
                .doOnError(WebClientResponseException.class, ReactiveStreamHandler::handleWebClientResponseException)
                .onErrorResume(WebClientResponseException.class,
                    ReactiveStreamHandler.error(hierarchies, "Failed to create Portfolio Position Hierarchies"))
                .onErrorStop()
            );
    }

    @NotNull
    private Mono<Void> createPerformance(PortfolioBundle portfolioBundle,
        Portfolio portfolio) {
        return Mono.justOrEmpty(portfolioBundle.getCumulativePerformances())
            .flatMap(cumulativePerformances -> portfolioCumulativePerformanceManagementApi
                .putPortfolioCumulativePerformance(portfolio.getCode(),
                    new PortfolioCumulativePerformancesPutRequest()
                        .cumulativePerformance(portfolioMapper.mapCumulativePerformances(cumulativePerformances)))
                .doOnError(WebClientResponseException.class, ReactiveStreamHandler::handleWebClientResponseException)
                .onErrorResume(WebClientResponseException.class,
                    ReactiveStreamHandler.error(cumulativePerformances,
                        "Failed to create Portfolio Cumulative Performances"))
                .onErrorStop()
            );
    }

    @NotNull
    private Mono<Void> createBenchmark(PortfolioBundle portfolioBundle, Portfolio portfolio) {
        return Mono.justOrEmpty(portfolioBundle.getBenchmark())
            .map(PortfolioBenchmark::getName)
            .flatMap(benchmarkName -> portfolioBenchmarksManagementApi
                .postPortfolioBenchmark(portfolioMapper.mapBenchmark(portfolio.getCode(), benchmarkName))
                .doOnError(WebClientResponseException.class, ReactiveStreamHandler::handleWebClientResponseException)
                .onErrorResume(WebClientResponseException.class,
                    ReactiveStreamHandler.error(benchmarkName, "Failed to create Portfolio Benchmark"))
                .onErrorStop()
            );
    }

    @NotNull
    private Mono<Void> createValuations(PortfolioBundle portfolioBundle, Portfolio portfolio) {
        return Mono.justOrEmpty(portfolioBundle.getValuations())
            .flatMap(valuations -> portfolioValuationManagementApi
                .putPortfolioValuations(portfolio.getCode(), new PortfolioValuationsPutRequest()
                    .valuations(portfolioMapper.mapValuations(valuations)))
                .doOnError(WebClientResponseException.class, ReactiveStreamHandler::handleWebClientResponseException)
                .onErrorResume(WebClientResponseException.class,
                    ReactiveStreamHandler.error(valuations, "Failed to create Portfolio Valuations"))
                .onErrorStop()
            );
    }

}
