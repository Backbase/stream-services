package com.backbase.stream.investment.saga;

import com.backbase.investment.api.service.v1.model.MarketRequest;
import com.backbase.investment.api.service.v1.model.MarketSpecialDayRequest;
import com.backbase.stream.configuration.InvestmentIngestionConfigurationProperties;
import com.backbase.stream.investment.InvestmentAssetData;
import com.backbase.stream.investment.InvestmentAssetsTask;
import com.backbase.stream.investment.InvestmentTask;
import com.backbase.stream.investment.service.InvestmentAssetPriceService;
import com.backbase.stream.investment.service.InvestmentAssetUniverseService;
import com.backbase.stream.investment.service.InvestmentClientService;
import com.backbase.stream.investment.service.InvestmentPortfolioService;
import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.model.StreamTask;
import com.backbase.stream.worker.model.StreamTask.State;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Saga orchestrating the complete investment client ingestion workflow.
 *
 * <p>This saga implements a multi-step process for ingesting investment data:
 * <ol>
 *   <li>Upsert investment clients - Creates or updates client records</li>
 *   <li>Upsert investment products - Creates or updates portfolio products</li>
 *   <li>Upsert investment portfolios - Creates or updates portfolios with client associations</li>
 * </ol>
 *
 * <p>The saga uses idempotent operations to ensure safe re-execution and writes progress
 * to the {@link StreamTask} history for observability. Each step builds upon the previous
 * step's results, creating a complete investment setup.
 *
 * <p>Design notes:
 * <ul>
 *   <li>All operations are idempotent (safe to retry)</li>
 *   <li>Progress is tracked via StreamTask state and history</li>
 *   <li>Failures are logged with complete context for debugging</li>
 *   <li>All reactive operations include proper success and error handlers</li>
 * </ul>
 *
 * @see InvestmentClientService
 * @see InvestmentPortfolioService
 * @see StreamTaskExecutor
 */
@Slf4j
@RequiredArgsConstructor
public class InvestmentAssetUniversSaga implements StreamTaskExecutor<InvestmentAssetsTask> {

    public static final String INVESTMENT = "investment-client";
    public static final String OP_UPSERT = "upsert";
    public static final String OP_CREATE = "create";
    public static final String RESULT_CREATED = "created";
    public static final String RESULT_FAILED = "failed";

    private static final String PROCESSING_PREFIX = "Processing ";

    private final InvestmentAssetUniverseService assetUniverseService;
    private final InvestmentAssetPriceService investmentAssetPriceService;
    private final InvestmentIngestionConfigurationProperties coreConfigurationProperties;

    @Override
    public Mono<InvestmentAssetsTask> executeTask(InvestmentAssetsTask streamTask) {
        if (!coreConfigurationProperties.isAssetUniversEnabled()) {
            log.warn("Skip investment asset univers saga execution: taskId={}, taskName={}",
                streamTask.getId(), streamTask.getName());
            return Mono.just(streamTask);
        }
        log.info("Starting investment saga execution: taskId={}, taskName={}",
            streamTask.getId(), streamTask.getName());
        return createMarkets(streamTask)
            .flatMap(this::createMarketSpecialDays)
            .flatMap(this::createAssets)
            .flatMap(this::upsertPrices)
            .doOnSuccess(completedTask -> log.info(
                "Successfully completed investment saga: taskId={}, taskName={}, state={}",
                completedTask.getId(), completedTask.getName(), completedTask.getState()))
            .doOnError(throwable -> {
                log.error("Failed to execute investment saga: taskId={}, taskName={}",
                    streamTask.getId(), streamTask.getName(), throwable);
                streamTask.error(INVESTMENT, OP_UPSERT, RESULT_FAILED,
                    streamTask.getName(), streamTask.getId(),
                    "Investment saga failed: " + throwable.getMessage());
                streamTask.setState(State.FAILED);
            })
            .onErrorResume(throwable -> Mono.just(streamTask));
    }

    private Mono<InvestmentAssetsTask> upsertPrices(InvestmentAssetsTask investmentTask) {
        return investmentAssetPriceService.ingestPrices(investmentTask.getData().getAssets(), investmentTask.getData()
                .getPriceByAsset())
            .map(r -> investmentTask);
    }

    /**
     * Rollback is not implemented for investment saga.
     *
     * <p>Investment operations are idempotent and designed to be retried safely.
     * Manual cleanup should be performed if necessary through the Investment Service API.
     *
     * @param streamTask the task to rollback
     * @return null - rollback not implemented
     */
    @Override
    public Mono<InvestmentAssetsTask> rollBack(InvestmentAssetsTask streamTask) {
        log.warn("Rollback requested for investment saga but not implemented: taskId={}, taskName={}",
            streamTask.getId(), streamTask.getName());
        return Mono.empty();
    }

    private Mono<InvestmentTask> upsertAssetCategory(InvestmentTask investmentTask) {
        return null;
    }

    public Mono<InvestmentAssetsTask> createMarkets(InvestmentAssetsTask investmentTask) {
        InvestmentAssetData investmentData = investmentTask.getData();
        int marketCount = investmentData.getMarkets() != null ? investmentData.getMarkets().size() : 0;
        log.info("Starting investment market creation: taskId={}, marketCount={}",
            investmentTask.getId(), marketCount);
        // Log the start of market creation and set task state to IN_PROGRESS
        investmentTask.info(INVESTMENT, OP_CREATE, null, investmentTask.getName(), investmentTask.getId(),
            PROCESSING_PREFIX + marketCount + " investment markets");
        investmentTask.setState(State.IN_PROGRESS);

        if (marketCount == 0) {
            log.warn("No markets to create for taskId={}", investmentTask.getId());
            investmentTask.setState(State.COMPLETED);
            return Mono.just(investmentTask);
        }

        // Process each market: create or get from asset universe service
        return Flux.fromIterable(investmentData.getMarkets())
            .flatMap(market -> assetUniverseService.getOrCreateMarket(
                new MarketRequest()
                    .code(market.getCode())
                    .name(market.getName())
                    .sessionStart(market.getSessionStart())
                    .sessionEnd(market.getSessionEnd())
                    .timeZone(market.getTimeZone())
            ))
            .collectList() // Collect all created/retrieved markets into a list
            .map(markets -> {
                // Update the task with the created markets
                investmentTask.setMarkets(markets);
                // Log completion and set task state to COMPLETED
                investmentTask.info(INVESTMENT, OP_CREATE, RESULT_CREATED, investmentTask.getName(),
                    investmentTask.getId(),
                    RESULT_CREATED + " " + markets.size() + " Investment Markets");
                investmentTask.setState(State.COMPLETED);
                log.info("Successfully created all markets: taskId={}, marketCount={}",
                    investmentTask.getId(), markets.size());
                return investmentTask;
            })
            .doOnError(throwable -> {
                log.error("Failed to create/upsert investment markets: taskId={}, marketCount={}",
                    investmentTask.getId(), marketCount, throwable);
                investmentTask.error(INVESTMENT, OP_CREATE, RESULT_FAILED, investmentTask.getName(),
                    investmentTask.getId(),
                    "Failed to create investment markets: " + throwable.getMessage());
            });
    }

    /**
     * Creates or upserts market special days for the investment task.
     *
     * <p>This method processes each market special day in the task data by invoking
     * the asset universe service. It updates the task state and logs progress for observability.
     *
     * @param investmentTask the investment task containing market special day data
     * @return Mono emitting the updated investment task with market special days set
     */
    public Mono<InvestmentAssetsTask> createMarketSpecialDays(InvestmentAssetsTask investmentTask) {
        InvestmentAssetData investmentData = investmentTask.getData();
        int marketSpecialDayCount =
            investmentData.getMarketSpecialDays() != null ? investmentData.getMarketSpecialDays().size() : 0;
        log.info("Starting investment market special days creation: taskId={}, marketSpecialDayCount={}",
            investmentTask.getId(), marketSpecialDayCount);
        // Log the start of market special days creation and set task state to IN_PROGRESS
        investmentTask.info(INVESTMENT, OP_CREATE, null, investmentTask.getName(), investmentTask.getId(),
            PROCESSING_PREFIX + marketSpecialDayCount + " investment markets special day");
        investmentTask.setState(State.IN_PROGRESS);

        if (marketSpecialDayCount == 0) {
            log.warn("No market special days to create for taskId={}", investmentTask.getId());
            investmentTask.setState(State.COMPLETED);
            return Mono.just(investmentTask);
        }

        // Process each market  special day: create or get from asset universe service
        return Flux.fromIterable(investmentData.getMarketSpecialDays())
            .flatMap(marketSpecialDay -> assetUniverseService.getOrCreateMarketSpecialDay(
                new MarketSpecialDayRequest()
                    .date(marketSpecialDay.getDate())
                    .market(marketSpecialDay.getMarket())
                    .sessionStart(marketSpecialDay.getSessionStart())
                    .sessionEnd(marketSpecialDay.getSessionEnd())
                    .description(marketSpecialDay.getDescription())
            ))
            .collectList() // Collect all created/retrieved market special days into a list
            .map(marketSpecialDays -> {
                // Update the task with the created market special days
                investmentTask.setMarketSpecialDays(marketSpecialDays);
                // Log completion and set task state to COMPLETED
                investmentTask.info(INVESTMENT, OP_CREATE, RESULT_CREATED, investmentTask.getName(),
                    investmentTask.getId(),
                    RESULT_CREATED + " " + marketSpecialDays.size() + " Investment Markets Special Days");
                investmentTask.setState(State.COMPLETED);
                log.info("Successfully created all market special days: taskId={}, marketSpecialDayCount={}",
                    investmentTask.getId(), marketSpecialDays.size());
                return investmentTask;
            })
            .doOnError(throwable -> {
                log.error("Failed to create/upsert investment market special days: taskId={}, marketSpecialDayCount={}",
                    investmentTask.getId(), marketSpecialDayCount, throwable);
                investmentTask.error(INVESTMENT, OP_CREATE, RESULT_FAILED, investmentTask.getName(),
                    investmentTask.getId(),
                    "Failed to create investment market special days: " + throwable.getMessage());
            });
    }

    /**
     * Creates investment assets by invoking the asset universe service for each asset in the task data. Updates the
     * task state and logs progress for observability.
     *
     * @param investmentTask the investment task containing asset data
     * @return Mono<InvestmentTask> with updated assets and state
     */
    public Mono<InvestmentAssetsTask> createAssets(InvestmentAssetsTask investmentTask) {
        InvestmentAssetData investmentData = investmentTask.getData();
        int assetCount = investmentData.getAssets() != null ? investmentData.getAssets().size() : 0;

        log.info("Starting investment asset creation: taskId={}, assetCount={}",
            investmentTask.getId(), assetCount);

        // Log the start of asset creation and set task state to IN_PROGRESS
        investmentTask.info(INVESTMENT, OP_CREATE, null, investmentTask.getName(), investmentTask.getId(),
            "Create Investment Assets");
        investmentTask.setState(State.IN_PROGRESS);

        if (assetCount == 0) {
            log.warn("No assets to create for taskId={}", investmentTask.getId());
            investmentTask.setState(State.COMPLETED);
            return Mono.just(investmentTask);
        }
        // Process each asset: create or get from asset universe service
        return assetUniverseService.createAssets(investmentData.getAssets())
            .collectList()
            .doOnSuccess(assets -> {
                investmentTask.setAssets(assets);
                investmentTask.info(INVESTMENT, OP_CREATE, RESULT_CREATED, investmentTask.getName(),
                    investmentTask.getId(),
                    RESULT_CREATED + " " + assets.size() + " Investment Assets");
                investmentTask.setState(State.COMPLETED);
                log.info("Successfully created all assets: taskId={}, assetCount={}",
                    investmentTask.getId(), assets.size());

            })
            .doOnError(throwable -> {
                log.error("Failed to create investment assets: taskId={}, assetCount={}",
                    investmentTask.getId(), assetCount, throwable);
                investmentTask.error(INVESTMENT, OP_CREATE, RESULT_FAILED, investmentTask.getName(),
                    investmentTask.getId(),
                    "Failed to create investment assets: " + throwable.getMessage());
            })
            .map(a -> investmentTask);
    }

}

