package com.backbase.stream.portfolio.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioAllocationsPutRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioBenchmarksGetResponse;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioCumulativePerformancesPutRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioPositionsHierarchyPutRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioTransactionsPostItem;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioTransactionsPostRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioValuationsItem.GranularityEnum;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioValuationsPutRequest;
import com.backbase.portfolio.api.service.integration.v1.model.TransactionCategory;
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
import com.backbase.stream.portfolio.model.Allocation;
import com.backbase.stream.portfolio.model.Portfolio;
import com.backbase.stream.portfolio.model.PortfolioBundle;
import com.backbase.stream.portfolio.model.PortfolioPositionsHierarchy;
import com.backbase.stream.portfolio.model.PortfolioValuation;
import com.backbase.stream.portfolio.model.Position;
import com.backbase.stream.portfolio.model.PositionBundle;
import com.backbase.stream.portfolio.model.PositionTransaction;
import com.backbase.stream.portfolio.model.SubPortfolio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     *
     * @param aggregatePortfolios {@see AggregatePortfolio}
     * @return Mono {@link AggregatePortfolio}
     */
    public Mono<AggregatePortfolio> createAggregatePortfolio(AggregatePortfolio aggregatePortfolios) {
        return aggregatePortfolioManagementApi
                .postAggregatePortfolios(portfolioMapper.mapAggregate(aggregatePortfolios))
                .onErrorResume(WebClientResponseException.Conflict.class,
                        throwable -> aggregatePortfolioManagementApi.putAggregatePortfolio(aggregatePortfolios.getId(),
                                portfolioMapper.mapPutAggregate(aggregatePortfolios)))
                .map(at -> aggregatePortfolios)
                .doOnError(WebClientResponseException.class, ReactiveStreamHandler::handleWebClientResponseException)
                .onErrorResume(WebClientResponseException.class,
                        ReactiveStreamHandler.error(aggregatePortfolios, "Failed to create Aggregate Portfolio"))
                .onErrorStop();
    }

    /**
     * Create Positions and all reference models that are related.
     *
     * @param positionBundle gang for positions
     * @return Mono {@link PositionBundle}
     */
    public Mono<PositionBundle> upsertPosition(PositionBundle positionBundle) {
        String subPortfolioId = positionBundle.getSubPortfolioId();
        String portfolioId = positionBundle.getPortfolioId();
        Position position = positionBundle.getPosition();
        // add check for instrument external id
        return Mono.defer(() -> positionManagementApi.getPositionById(position.getExternalId()))
                .onErrorResume(WebClientResponseException.NotFound.class, throwable -> Mono.empty())
                .flatMap(r -> positionManagementApi
                        .putPosition(position.getExternalId(), portfolioMapper.mapPutPosition(position))
                        .then(Mono.just(position)))
                .switchIfEmpty(Mono.defer(() -> positionManagementApi
                        .postPositions(portfolioMapper.mapPosition(portfolioId, subPortfolioId, position))
                        .map(o -> position)))
                .thenMany(upsertTransactionCategory(positionBundle.getTransactionCategories()))
                .then(upsertTransactions(positionBundle.getTransactions(), portfolioId, position.getExternalId()))
                .map(at -> positionBundle)
                .doOnError(WebClientResponseException.class, ReactiveStreamHandler::handleWebClientResponseException)
                .onErrorResume(WebClientResponseException.class,
                        ReactiveStreamHandler.error(positionBundle, "Failed to create Position gang"))
                .onErrorStop();
    }

    /**
     * Create Position and all reference models that are related.
     *
     * @param The {@code Position} to be created.
     * @return Mono of {@link Position}
     */
    public Mono<Position> upsertPosition(Position position) {
        String positionId = position.getExternalId();

        return Mono.defer(() -> positionManagementApi.getPositionById(positionId))
                .onErrorResume(WebClientResponseException.NotFound.class, throwable -> Mono.empty())
                .flatMap(r -> positionManagementApi.putPosition(positionId, portfolioMapper.mapPutPosition(position))
                        .then(Mono.just(position)))
                .switchIfEmpty(
                        Mono.defer(() -> positionManagementApi.postPositions(portfolioMapper.mapPostPosition(position))
                                .map(o -> position)))
                .map(at -> position)
                .doOnError(WebClientResponseException.class, ReactiveStreamHandler::handleWebClientResponseException)
                .onErrorResume(WebClientResponseException.class,
                        ReactiveStreamHandler.error(position, "Failed to create Position"))
                .onErrorStop();
    }

    public Mono<List<PositionTransaction>> upsertPositionTransactions(List<PositionTransaction> transactions,
            String portfolioId, String positionId) {
        return upsertTransactions(transactions, portfolioId, positionId).flatMap(o -> Mono.just(transactions));
    }

    /**
     * Create Portfolios and all reference models that are related.
     *
     * @param portfolioBundle gang for portfolio
     * @return Flux {@link PortfolioBundle}
     */
    public Flux<PortfolioBundle> upsertPortfolio(PortfolioBundle portfolioBundle) {
        Portfolio portfolio = portfolioBundle.getPortfolio();
        String portfolioCode = portfolio.getCode();

        return portfolioManagementApi.getPortfolio(portfolioCode)
                .onErrorResume(WebClientResponseException.NotFound.class, throwable -> Mono.empty())
                .flatMap(r -> portfolioManagementApi
                        .putPortfolio(portfolioCode, portfolioMapper.mapPutPortfolio(portfolio))
                        .then(Mono.just(portfolio)))
                .switchIfEmpty(
                        Mono.defer(() -> portfolioManagementApi.postPortfolios(portfolioMapper.mapPortfolio(portfolio))
                                // add update handle
                                .onErrorResume(WebClientResponseException.Conflict.class, throwable -> Mono.empty())
                                .map(r -> portfolio)))
                .thenMany(upsertSubPortfolio(portfolioBundle.getSubPortfolios(), portfolioCode))
                .thenMany(upsertAllocation(portfolioBundle.getAllocations(), portfolioCode))
                .thenMany(upsertHierarchy(portfolioBundle.getHierarchies(), portfolioCode))
                .thenMany(upsertPerformance(portfolioBundle, portfolio))
                .thenMany(upsertBenchmark(portfolioBundle, portfolio))
                .thenMany(upsertValuations(portfolioBundle.getValuations(), portfolioCode))
                .map(at -> portfolioBundle)
                .doOnError(WebClientResponseException.class, ReactiveStreamHandler::handleWebClientResponseException)
                .onErrorResume(WebClientResponseException.class,
                        ReactiveStreamHandler.error(portfolioBundle, "Failed to create Portfolio gang"))
                .onErrorStop();
    }

    /**
     * Create Portfolio.
     *
     * @param portfolio The {@code Portfolio} to be created.
     * @return Mono {@link Portfolio}.
     */
    public Mono<Portfolio> upsertPortfolio(Portfolio portfolio) {
        return portfolioManagementApi.getPortfolio(portfolio.getCode())
                .onErrorResume(WebClientResponseException.NotFound.class, throwable -> Mono.empty())
                .flatMap(r -> portfolioManagementApi
                        .putPortfolio(portfolio.getCode(), portfolioMapper.mapPutPortfolio(portfolio))
                        .then(Mono.just(portfolio)))
                .switchIfEmpty(
                        Mono.defer(() -> portfolioManagementApi.postPortfolios(portfolioMapper.mapPortfolio(portfolio))
                                // add update handle
                                .onErrorResume(WebClientResponseException.Conflict.class, throwable -> Mono.empty())
                                .map(r -> portfolio)))
                .map(at -> portfolio)
                .doOnError(WebClientResponseException.class, ReactiveStreamHandler::handleWebClientResponseException)
                .onErrorResume(WebClientResponseException.class,
                        ReactiveStreamHandler.error(portfolio, "Failed to create Portfolio"))
                .onErrorStop();
    }

    /**
     * Create Allocations.
     * 
     * @param allocations The allocaitons to be created.
     * @param portfolioCode The code of the root portfolio.
     * @return The created allocaitons.
     */
    public Mono<List<Allocation>> upsertAllocations(List<Allocation> allocations, String portfolioCode) {
        return upsertAllocation(allocations, portfolioCode).flatMap(o -> Mono.just(allocations));
    }

    /**
     * Create SubPortfolios.
     * 
     * @param subPortfolios The subPortfolios to be created.
     * @param portfolioCode The code of the root portfolio.
     * @return The created subPortfolios.
     */
    public Mono<List<SubPortfolio>> upsertSubPortfolios(List<SubPortfolio> subPortfolios, String portfolioCode) {
        return upsertSubPortfolio(subPortfolios, portfolioCode).collectList().flatMap(o -> Mono.just(subPortfolios));
    }

    /**
     * Create TransactionCategories.
     * 
     * @param transactionCategories The transactionCategories to be created.
     * @return The created transactionCategories.
     */
    public Mono<List<com.backbase.stream.portfolio.model.TransactionCategory>> upsertTransactionCategories(
            List<com.backbase.stream.portfolio.model.TransactionCategory> transactionCategories) {
        return upsertTransactionCategory(transactionCategories).collectList()
                .flatMap(o -> Mono.just(transactionCategories));
    }

    /**
     * Create Hierarchies.
     * 
     * @param hierarchies The hierarchies to be created.
     * @param portfolioCode The code of the root portfolio.
     * @return The created hierarchies.
     */
    public Mono<List<PortfolioPositionsHierarchy>> upsertHierarchies(List<PortfolioPositionsHierarchy> hierarchies,
            String portfolioCode) {
        return upsertHierarchy(hierarchies, portfolioCode).flatMap(o -> Mono.just(hierarchies));
    }

    /**
     * Create PortfolioValuations.
     * 
     * @param portfolioValuations The portfolioValuations to be created.
     * @param portfolioCode The code of the root portfolio.
     * @return The created subPortfolios.
     */
    public Mono<List<PortfolioValuation>> upsertPortfolioValuations(List<PortfolioValuation> portfolioValuations,
            String portfolioCode) {
        return upsertValuations(portfolioValuations, portfolioCode).flatMap(o -> Mono.just(portfolioValuations));
    }

    @NotNull
    private Mono<Void> upsertTransactions(List<PositionTransaction> transactions, String portfolioId,
            String positionId) {
        return Mono.justOrEmpty(transactions)
                .flatMap(trs -> transactionManagementApi.deletePositionTransactions(positionId)
                        .then(transactionManagementApi
                                .postPortfolioTransactions(portfolioId,
                                        new PortfolioTransactionsPostRequest().transactions(
                                                List.of(new PortfolioTransactionsPostItem().positionId(positionId)
                                                        .transactions(portfolioMapper.mapTransaction(trs)))))
                                .onErrorResume(WebClientResponseException.class,
                                        ReactiveStreamHandler.error(trs, "Failed to create Transactions"))
                                .onErrorStop()));
    }

    @NotNull
    private Flux<Void> upsertTransactionCategory(
            List<com.backbase.stream.portfolio.model.TransactionCategory> transactionCategories) {
        return transactionCategoryManagementApi.getTransactionCategories()
                .map(TransactionCategory::getKey)
                .collectList()
                .flatMapMany(tcs -> ReactiveStreamHandler.getFluxStream(transactionCategories)
                        .flatMap(tc -> (tcs.contains(tc.getKey())
                                ? transactionCategoryManagementApi.putTransactionCategory(tc.getKey(),
                                        portfolioMapper.mapPutTransactionCategory(tc))
                                : transactionCategoryManagementApi
                                        .postTransactionCategory(portfolioMapper.mapTransactionCategory(tc)))
                                                .doOnError(WebClientResponseException.class,
                                                        ReactiveStreamHandler::handleWebClientResponseException)
                                                .onErrorResume(WebClientResponseException.class,
                                                        ReactiveStreamHandler.error(tc,
                                                                "Failed to create Transaction Category"))
                                                .onErrorStop()));
    }

    @NotNull
    private Flux<SubPortfolio> upsertSubPortfolio(List<SubPortfolio> subPortfolios, String portfolioCode) {
        return ReactiveStreamHandler.getFluxStream(subPortfolios)
                .flatMap(sp -> subPortfolioManagementApi.getSubPortfolio(portfolioCode, sp.getCode())
                        .flatMap(r -> subPortfolioManagementApi
                                .putSubPortfolio(portfolioCode, sp.getCode(), portfolioMapper.mapPutSubPortfolio(sp))
                                .then(Mono.just(sp)))
                        .onErrorResume(WebClientResponseException.NotFound.class, throwable -> Mono.empty())
                        .switchIfEmpty(subPortfolioManagementApi
                                .postSubPortfolios(portfolioCode, portfolioMapper.mapSubPortfolio(sp))
                                .then(Mono.just(sp)))
                        .doOnError(WebClientResponseException.class,
                                ReactiveStreamHandler::handleWebClientResponseException)
                        .onErrorResume(WebClientResponseException.class,
                                ReactiveStreamHandler.error(sp, "Failed to create Sub Portfolio"))
                        .onErrorStop());
    }

    @NotNull
    private Mono<Void> upsertAllocation(List<Allocation> allocations, String portfolioCode) {
        return Mono.justOrEmpty(allocations)
                .flatMap(
                        a -> portfolioManagementApi
                                .putPortfolioAllocations(portfolioCode,
                                        new PortfolioAllocationsPutRequest()
                                                .allocations(portfolioMapper.mapAllocations(a)))
                                .doOnError(WebClientResponseException.class,
                                        ReactiveStreamHandler::handleWebClientResponseException)
                                .onErrorResume(WebClientResponseException.class,
                                        ReactiveStreamHandler.error(a, "Failed to create Portfolio Allocations"))
                                .onErrorStop());
    }

    @NotNull
    private Mono<Void> upsertHierarchy(List<PortfolioPositionsHierarchy> hierarchies, String portfolioCode) {
        return Mono.justOrEmpty(hierarchies)
                .flatMap(
                        h -> portfolioPositionsHierarchyManagementApi
                                .putPortfolioPositionsHierarchy(portfolioCode,
                                        new PortfolioPositionsHierarchyPutRequest()
                                                .items(portfolioMapper.mapHierarchies(h)))
                                .doOnError(WebClientResponseException.class,
                                        ReactiveStreamHandler::handleWebClientResponseException)
                                .onErrorResume(WebClientResponseException.class,
                                        ReactiveStreamHandler.error(h,
                                                "Failed to create Portfolio Position Hierarchies"))
                                .onErrorStop());
    }

    @NotNull
    private Mono<Void> upsertPerformance(PortfolioBundle portfolioBundle, Portfolio portfolio) {
        return Mono.justOrEmpty(portfolioBundle.getCumulativePerformances())
                .flatMap(cumulativePerformances -> portfolioCumulativePerformanceManagementApi
                        .putPortfolioCumulativePerformance(portfolio.getCode(),
                                new PortfolioCumulativePerformancesPutRequest().cumulativePerformance(
                                        portfolioMapper.mapCumulativePerformances(cumulativePerformances)))
                        .doOnError(WebClientResponseException.class,
                                ReactiveStreamHandler::handleWebClientResponseException)
                        .onErrorResume(WebClientResponseException.class,
                                ReactiveStreamHandler.error(cumulativePerformances,
                                        "Failed to create Portfolio Cumulative Performances"))
                        .onErrorStop());
    }

    @NotNull
    private Mono<Void> upsertBenchmark(PortfolioBundle portfolioBundle, Portfolio portfolio) {
        return portfolioBenchmarksManagementApi.getPortfolioBenchmarks(0, Integer.MAX_VALUE)
                .map(PortfolioBenchmarksGetResponse::getBenchmarks)
                .switchIfEmpty(Mono.defer(() -> Mono.just(Collections.emptyList())))
                .flatMap(existBenchmarks -> {
                    String name = portfolioBundle.getBenchmark().getName();
                    return existBenchmarks.stream()
                            .filter(b -> b.getName().equalsIgnoreCase(name))
                            .findAny()
                            .map(existBenchmark -> portfolioBenchmarksManagementApi
                                    .putPortfolioBenchmark(existBenchmark.getId(), portfolioMapper.mapBenchmark(name)))
                            .or(() -> Optional.of(portfolioBenchmarksManagementApi
                                    .postPortfolioBenchmark(portfolioMapper.mapBenchmark(portfolio.getCode(), name))))
                            .orElse(Mono.empty())
                            .doOnError(WebClientResponseException.class,
                                    ReactiveStreamHandler::handleWebClientResponseException)
                            .onErrorResume(WebClientResponseException.class,
                                    ReactiveStreamHandler.error(name, "Failed to create Portfolio Benchmark"))
                            .onErrorStop();
                });
    }

    @NotNull
    private Mono<Void> upsertValuations(List<PortfolioValuation> valuations, String portfolioCode) {
        return Flux.just(GranularityEnum.values())
                .flatMap(g -> portfolioValuationManagementApi.deletePortfolioValuations(portfolioCode, g.getValue())
                        .map(r -> g))
                .collectList()
                .then(Mono.justOrEmpty(valuations)
                        .flatMap(v -> portfolioValuationManagementApi
                                .putPortfolioValuations(portfolioCode,
                                        new PortfolioValuationsPutRequest()
                                                .valuations(portfolioMapper.mapValuations(v)))
                                .doOnError(WebClientResponseException.class,
                                        ReactiveStreamHandler::handleWebClientResponseException)
                                .onErrorResume(WebClientResponseException.class,
                                        ReactiveStreamHandler.error(v, "Failed to create Portfolio Valuations"))
                                .onErrorStop()));
    }

}
