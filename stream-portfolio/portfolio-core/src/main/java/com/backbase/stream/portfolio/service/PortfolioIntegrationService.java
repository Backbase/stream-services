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
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioGetResponse;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioPositionsHierarchyPutRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioTransactionsPostItem;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioTransactionsPostRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioValuationsItem.GranularityEnum;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioValuationsPutRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PositionGetResponse;
import com.backbase.portfolio.api.service.integration.v1.model.SubPortfolioGetResponse;
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
import com.backbase.stream.portfolio.model.PortfolioCumulativePerformances;
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
        return postAggregatePortfolios(aggregatePortfolios)
                .onErrorResume(WebClientResponseException.Conflict.class,
                        throwable -> putAggregatePortfolio(aggregatePortfolios))
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
        String portfolioId = positionBundle.getPortfolioId();
        String subPortfolioId = positionBundle.getSubPortfolioId();
        Position position = positionBundle.getPosition();
        String positionId = position.getExternalId();

        // add check for instrument external id
        return getPositionById(positionId)
                .onErrorResume(WebClientResponseException.NotFound.class, throwable -> Mono.empty())
                .flatMap(r -> putPosition(position).then(Mono.just(position)))
                .switchIfEmpty(
                        Mono.defer(() -> postPositions(portfolioId, subPortfolioId, position).map(o -> position)))
                .thenMany(upsertTransactionCategory(positionBundle.getTransactionCategories()))
                .then(upsertTransactions(positionBundle.getTransactions(), portfolioId, positionId))
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

        return getPositionById(positionId)
                .onErrorResume(WebClientResponseException.NotFound.class, throwable -> Mono.empty())
                .flatMap(r -> putPosition(position).then(Mono.just(position)))
                .switchIfEmpty(Mono.defer(() -> postPositions(position).map(o -> position)))
                .map(at -> position)
                .doOnError(WebClientResponseException.class, ReactiveStreamHandler::handleWebClientResponseException)
                .onErrorResume(WebClientResponseException.class,
                        ReactiveStreamHandler.error(position, "Failed to create Position"))
                .onErrorStop();
    }

    /**
     * Create PositionTransactions and all reference models that are related.
     * 
     * @param transactions
     * @param portfolioId
     * @param positionId
     * @return
     */
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

        return getPortfolio(portfolioCode)
                .onErrorResume(WebClientResponseException.NotFound.class, throwable -> Mono.empty())
                .flatMap(r -> putPortfolio(portfolio).then(Mono.just(portfolio)))
                .switchIfEmpty(Mono.defer(() -> postPortfolios(portfolio)
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
        return getPortfolio(portfolio.getCode())
                .onErrorResume(WebClientResponseException.NotFound.class, throwable -> Mono.empty())
                .flatMap(r -> putPortfolio(portfolio).then(Mono.just(portfolio)))
                .switchIfEmpty(Mono.defer(() -> postPortfolios(portfolio)
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
                .flatMap(trs -> deletePositionTransactions(positionId)
                        .then(Mono.defer(() -> postPortfolioTransactions(trs, portfolioId, positionId))
                                .onErrorResume(WebClientResponseException.class,
                                        ReactiveStreamHandler.error(trs, "Failed to create Transactions"))
                                .onErrorStop()));
    }

    @NotNull
    private Flux<Void> upsertTransactionCategory(
            List<com.backbase.stream.portfolio.model.TransactionCategory> transactionCategories) {
        return getTransactionCategories().map(TransactionCategory::getKey)
                .collectList()
                .flatMapMany(tcs -> ReactiveStreamHandler.getFluxStream(transactionCategories)
                        .flatMap(tc -> (tcs.contains(tc.getKey()) ? putTransactionCategory(tc)
                                : postTransactionCategory(tc))
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
                .flatMap(sp -> getSubPortfolio(portfolioCode, sp.getCode())
                        .flatMap(r -> putSubPortfolio(portfolioCode, sp).then(Mono.just(sp)))
                        .onErrorResume(WebClientResponseException.NotFound.class, throwable -> Mono.empty())
                        .switchIfEmpty(Mono.defer(() -> postSubPortfolios(portfolioCode, sp).then(Mono.just(sp))))
                        .doOnError(WebClientResponseException.class,
                                ReactiveStreamHandler::handleWebClientResponseException)
                        .onErrorResume(WebClientResponseException.class,
                                ReactiveStreamHandler.error(sp, "Failed to create Sub Portfolio"))
                        .onErrorStop());
    }

    @NotNull
    private Mono<Void> upsertAllocation(List<Allocation> allocations, String portfolioCode) {
        return Mono.justOrEmpty(allocations)
                .flatMap(a -> putPortfolioAllocations(portfolioCode, a)
                        .doOnError(WebClientResponseException.class,
                                ReactiveStreamHandler::handleWebClientResponseException)
                        .onErrorResume(WebClientResponseException.class,
                                ReactiveStreamHandler.error(a, "Failed to create Portfolio Allocations"))
                        .onErrorStop());
    }

    @NotNull
    private Mono<Void> upsertHierarchy(List<PortfolioPositionsHierarchy> hierarchies, String portfolioCode) {
        return Mono.justOrEmpty(hierarchies)
                .flatMap(h -> putPortfolioPositionsHierarchy(portfolioCode, h)
                        .doOnError(WebClientResponseException.class,
                                ReactiveStreamHandler::handleWebClientResponseException)
                        .onErrorResume(WebClientResponseException.class,
                                ReactiveStreamHandler.error(h, "Failed to create Portfolio Position Hierarchies"))
                        .onErrorStop());
    }

    @NotNull
    private Mono<Void> upsertPerformance(PortfolioBundle portfolioBundle, Portfolio portfolio) {
        return Mono.justOrEmpty(portfolioBundle.getCumulativePerformances())
                .flatMap(cumulativePerformances -> putPortfolioCumulativePerformance(portfolio.getCode(),
                        cumulativePerformances)
                                .doOnError(WebClientResponseException.class,
                                        ReactiveStreamHandler::handleWebClientResponseException)
                                .onErrorResume(WebClientResponseException.class,
                                        ReactiveStreamHandler.error(cumulativePerformances,
                                                "Failed to create Portfolio Cumulative Performances"))
                                .onErrorStop());
    }

    @NotNull
    private Mono<Void> upsertBenchmark(PortfolioBundle portfolioBundle, Portfolio portfolio) {
        return getPortfolioBenchmarks(0, Integer.MAX_VALUE).map(PortfolioBenchmarksGetResponse::getBenchmarks)
                .switchIfEmpty(Mono.defer(() -> Mono.just(Collections.emptyList())))
                .flatMap(existBenchmarks ->
                {
                    String name = portfolioBundle.getBenchmark().getName();
                    return existBenchmarks.stream()
                            .filter(b -> b.getName().equalsIgnoreCase(name))
                            .findAny()
                            .map(existBenchmark -> putPortfolioBenchmark(existBenchmark.getId(), name))
                            .or(() -> Optional.of(postPortfolioBenchmark(portfolio.getCode(), name)))
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
                .flatMap(g -> deletePortfolioValuations(portfolioCode, g.getValue()).map(r -> g))
                .collectList()
                .then(Mono.justOrEmpty(valuations)
                        .flatMap(v -> putPortfolioValuations(portfolioCode, v)
                                .doOnError(WebClientResponseException.class,
                                        ReactiveStreamHandler::handleWebClientResponseException)
                                .onErrorResume(WebClientResponseException.class,
                                        ReactiveStreamHandler.error(v, "Failed to create Portfolio Valuations"))
                                .onErrorStop()));
    }

    private Mono<Void> postAggregatePortfolios(AggregatePortfolio aggregatePortfolio) {
        try {
            return Optional
                    .ofNullable(aggregatePortfolioManagementApi
                            .postAggregatePortfolios(portfolioMapper.mapAggregate(aggregatePortfolio)))
                    .orElseGet(Mono::empty);
        } catch (WebClientResponseException ex) {
            log.warn("Failed creating Aggregate Portfolio", ex);

            return Mono.error(ex);
        }
    }

    private Mono<Void> putAggregatePortfolio(AggregatePortfolio aggregatePortfolio) {
        try {
            return Optional.ofNullable(aggregatePortfolioManagementApi.putAggregatePortfolio(aggregatePortfolio.getId(),
                    portfolioMapper.mapPutAggregate(aggregatePortfolio))).orElseGet(Mono::empty);
        } catch (WebClientResponseException ex) {
            log.warn("Failed updating Aggregate Portfolio", ex);

            return Mono.error(ex);
        }
    }

    private Mono<PositionGetResponse> getPositionById(String positionId) {
        try {
            return Optional.ofNullable(positionManagementApi.getPositionById(positionId)).orElseGet(Mono::empty);
        } catch (WebClientResponseException ex) {
            log.warn("Failed getting position by id, positionId: {}", positionId, ex);

            return Mono.error(ex);
        }
    }

    private Mono<Void> putPosition(Position position) {
        String positionId = position.getExternalId();

        try {
            return Optional
                    .ofNullable(positionManagementApi.putPosition(positionId, portfolioMapper.mapPutPosition(position)))
                    .orElseGet(Mono::empty);
        } catch (WebClientResponseException ex) {
            log.warn("Failed updating position, positionId: {}", positionId, ex);

            return Mono.error(ex);
        }
    }

    private Mono<Void> postPositions(String portfolioId, String subPortfolioId, Position position) {
        try {
            return Optional
                    .ofNullable(positionManagementApi
                            .postPositions(portfolioMapper.mapPosition(portfolioId, subPortfolioId, position)))
                    .orElseGet(Mono::empty);
        } catch (WebClientResponseException ex) {
            log.warn("Failed creating position, positionId: {}", position.getExternalId(), ex);

            return Mono.error(ex);
        }
    }

    private Mono<Void> postPositions(Position position) {
        try {
            return Optional.ofNullable(positionManagementApi.postPositions(portfolioMapper.mapPostPosition(position)))
                    .orElseGet(Mono::empty);
        } catch (WebClientResponseException ex) {
            log.warn("Failed creating position, positionId: {}", position.getExternalId(), ex);

            return Mono.error(ex);
        }
    }

    private Mono<PortfolioGetResponse> getPortfolio(String portfolioCode) {
        try {
            return Optional.ofNullable(portfolioManagementApi.getPortfolio(portfolioCode)).orElseGet(Mono::empty);
        } catch (WebClientResponseException ex) {
            log.warn("Failed getting portfolio, portfolioCode: {}", portfolioCode, ex);

            return Mono.error(ex);
        }
    }

    private Mono<Void> putPortfolio(Portfolio portfolio) {
        String portfolioCode = portfolio.getCode();

        try {
            return Optional
                    .ofNullable(portfolioManagementApi.putPortfolio(portfolioCode,
                            portfolioMapper.mapPutPortfolio(portfolio)))
                    .orElseGet(Mono::empty);
        } catch (WebClientResponseException ex) {
            log.warn("Failed updating portfolio, portfolioCode: {}", portfolioCode, ex);

            return Mono.error(ex);
        }
    }

    private Mono<Void> postPortfolios(Portfolio portfolio) {
        String portfolioCode = portfolio.getCode();

        try {
            return Optional.ofNullable(portfolioManagementApi.postPortfolios(portfolioMapper.mapPortfolio(portfolio)))
                    .orElseGet(Mono::empty);
        } catch (WebClientResponseException ex) {
            log.warn("Failed creating portfolio, portfolioCode: {}", portfolioCode, ex);

            return Mono.error(ex);
        }
    }

    private Mono<Void> deletePositionTransactions(String positionId) {
        try {
            return Optional.ofNullable(transactionManagementApi.deletePositionTransactions(positionId))
                    .orElseGet(Mono::empty);
        } catch (WebClientResponseException ex) {
            log.warn("Failed deleting postion trasactions, positionId: {}", positionId, ex);

            return Mono.error(ex);
        }
    }

    private Mono<Void> postPortfolioTransactions(List<PositionTransaction> transactions, String portfolioId,
            String positionId) {
        try {
            return Optional
                    .ofNullable(transactionManagementApi.postPortfolioTransactions(portfolioId,
                            new PortfolioTransactionsPostRequest()
                                    .transactions(List.of(new PortfolioTransactionsPostItem().positionId(positionId)
                                            .transactions(portfolioMapper.mapTransaction(transactions))))))
                    .orElseGet(Mono::empty);
        } catch (WebClientResponseException ex) {
            log.warn("Failed creating postion trasactions, portfolioId: {}, positionId: {}", portfolioId, positionId,
                    ex);

            return Mono.error(ex);
        }
    }

    private Flux<TransactionCategory> getTransactionCategories() {
        try {
            return Optional.ofNullable(transactionCategoryManagementApi.getTransactionCategories())
                    .orElseGet(Flux::empty);
        } catch (WebClientResponseException ex) {
            log.warn("Failed getting transaction categories", ex);

            return Flux.error(ex);
        }
    }

    private Mono<Void>
            putTransactionCategory(com.backbase.stream.portfolio.model.TransactionCategory transactionCategory) {
        String key = transactionCategory.getKey();

        try {
            return Optional.ofNullable(transactionCategoryManagementApi.putTransactionCategory(key,
                    portfolioMapper.mapPutTransactionCategory(transactionCategory))).orElseGet(Mono::empty);
        } catch (WebClientResponseException ex) {
            log.warn("Failed updating Transaction Category, key: {}", key, ex);

            return Mono.error(ex);
        }
    }

    private Mono<Void>
            postTransactionCategory(com.backbase.stream.portfolio.model.TransactionCategory transactionCategory) {
        try {
            return Optional
                    .ofNullable(transactionCategoryManagementApi
                            .postTransactionCategory(portfolioMapper.mapTransactionCategory(transactionCategory)))
                    .orElseGet(Mono::empty);
        } catch (WebClientResponseException ex) {
            log.warn("Failed creating Transaction Category, key: {}", transactionCategory.getKey(), ex);

            return Mono.error(ex);
        }
    }

    private Mono<SubPortfolioGetResponse> getSubPortfolio(String portfolioCode, String subPortfolioCode) {
        try {
            return Optional.ofNullable(subPortfolioManagementApi.getSubPortfolio(portfolioCode, subPortfolioCode))
                    .orElseGet(Mono::empty);
        } catch (WebClientResponseException ex) {
            log.warn("Failed getting subPortfolio, portfolioCode: {}, subPortfolioCode: {}", portfolioCode,
                    subPortfolioCode, ex);

            return Mono.error(ex);
        }
    }

    private Mono<Void> putSubPortfolio(String portfolioCode, SubPortfolio subPortfolio) {
        String subPortfolioCode = subPortfolio.getCode();

        try {
            return Optional.ofNullable(subPortfolioManagementApi.putSubPortfolio(portfolioCode, subPortfolioCode,
                    portfolioMapper.mapPutSubPortfolio(subPortfolio))).orElseGet(Mono::empty);
        } catch (WebClientResponseException ex) {
            log.warn("Failed updating subPortfolio, portfolioCode: {}, subPortfolioCode: {}", portfolioCode,
                    subPortfolioCode, ex);

            return Mono.error(ex);
        }
    }

    private Mono<Void> postSubPortfolios(String portfolioCode, SubPortfolio subPortfolio) {
        String subPortfolioCode = subPortfolio.getCode();

        try {
            return Optional.ofNullable(subPortfolioManagementApi.postSubPortfolios(portfolioCode,
                    portfolioMapper.mapSubPortfolio(subPortfolio))).orElseGet(Mono::empty);
        } catch (WebClientResponseException ex) {
            log.warn("Failed creating subPortfolio, portfolioCode: {}, subPortfolioCode: {}", portfolioCode,
                    subPortfolioCode, ex);

            return Mono.error(ex);
        }
    }

    private Mono<Void> putPortfolioAllocations(String portfolioCode, List<Allocation> allocations) {
        try {
            return Optional
                    .ofNullable(
                            portfolioManagementApi.putPortfolioAllocations(portfolioCode,
                                    new PortfolioAllocationsPutRequest()
                                            .allocations(portfolioMapper.mapAllocations(allocations))))
                    .orElseGet(Mono::empty);
        } catch (WebClientResponseException ex) {
            log.warn("Failed updating Portfolio Allocations, portfolioCode: {}", portfolioCode, ex);

            return Mono.error(ex);
        }
    }

    private Mono<Void> putPortfolioPositionsHierarchy(String portfolioCode,
            List<PortfolioPositionsHierarchy> hierarchies) {
        try {
            return Optional
                    .ofNullable(
                            portfolioPositionsHierarchyManagementApi.putPortfolioPositionsHierarchy(portfolioCode,
                                    new PortfolioPositionsHierarchyPutRequest()
                                            .items(portfolioMapper.mapHierarchies(hierarchies))))
                    .orElseGet(Mono::empty);
        } catch (WebClientResponseException ex) {
            log.warn("Failed updating Portfolio Positions Hierarchy, portfolioCode: {}", portfolioCode, ex);

            return Mono.error(ex);
        }
    }

    private Mono<Void> putPortfolioCumulativePerformance(String portfolioCode,
            List<PortfolioCumulativePerformances> cumulativePerformances) {
        try {
            return Optional
                    .ofNullable(
                            portfolioCumulativePerformanceManagementApi.putPortfolioCumulativePerformance(portfolioCode,
                                    new PortfolioCumulativePerformancesPutRequest().cumulativePerformance(
                                            portfolioMapper.mapCumulativePerformances(cumulativePerformances))))
                    .orElseGet(Mono::empty);
        } catch (WebClientResponseException ex) {
            log.warn("Failed updating Portfolio Cumulative Performance, portfolioCode: {}", portfolioCode, ex);

            return Mono.error(ex);
        }
    }

    private Mono<PortfolioBenchmarksGetResponse> getPortfolioBenchmarks(Integer from, Integer size) {
        try {
            return Optional.ofNullable(portfolioBenchmarksManagementApi.getPortfolioBenchmarks(from, size))
                    .orElseGet(Mono::empty);
        } catch (WebClientResponseException ex) {
            log.warn("Failed getting Portfolio Benchmarks", ex);

            return Mono.error(ex);
        }
    }

    private Mono<Void> putPortfolioBenchmark(String benchmarkId, String name) {
        try {
            return Optional.ofNullable(portfolioBenchmarksManagementApi.putPortfolioBenchmark(benchmarkId,
                    portfolioMapper.mapBenchmark(name))).orElseGet(Mono::empty);
        } catch (WebClientResponseException ex) {
            log.warn("Failed updating Portfolio Benchmark, benchmarkId: {}, name: {}", benchmarkId, name, ex);

            return Mono.error(ex);
        }
    }

    private Mono<Void> postPortfolioBenchmark(String benchmarkId, String name) {
        try {
            return Optional
                    .ofNullable(portfolioBenchmarksManagementApi
                            .postPortfolioBenchmark(portfolioMapper.mapBenchmark(benchmarkId, name)))
                    .orElseGet(Mono::empty);
        } catch (WebClientResponseException ex) {
            log.warn("Failed creating Portfolio Benchmark, benchmarkId: {}, name: {}", benchmarkId, name, ex);

            return Mono.error(ex);
        }
    }

    private Mono<Void> deletePortfolioValuations(String portfolioCode, String granularity) {
        try {
            return Optional
                    .ofNullable(portfolioValuationManagementApi.deletePortfolioValuations(portfolioCode, granularity))
                    .orElseGet(Mono::empty);
        } catch (WebClientResponseException ex) {
            log.warn("Failed deleting Portfolio Valuations, String portfolioCode: {}, granularity: {}", portfolioCode,
                    granularity, ex);

            return Mono.error(ex);
        }
    }

    private Mono<Void> putPortfolioValuations(String portfolioCode, List<PortfolioValuation> valuations) {
        try {
            return Optional
                    .ofNullable(portfolioValuationManagementApi.putPortfolioValuations(portfolioCode,
                            new PortfolioValuationsPutRequest().valuations(portfolioMapper.mapValuations(valuations))))
                    .orElseGet(Mono::empty);
        } catch (WebClientResponseException ex) {
            log.warn("Failed updating Portfolio Valuations, String portfolioCode: {}", portfolioCode, ex);

            return Mono.error(ex);
        }
    }
}
