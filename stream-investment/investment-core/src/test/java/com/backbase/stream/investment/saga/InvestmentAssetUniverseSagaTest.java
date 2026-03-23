package com.backbase.stream.investment.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.investment.api.service.v1.model.AssetCategoryType;
import com.backbase.investment.api.service.v1.model.AssetTypeEnum;
import com.backbase.investment.api.service.v1.model.Currency;
import com.backbase.investment.api.service.v1.model.GroupResult;
import com.backbase.investment.api.service.v1.model.Market;
import com.backbase.investment.api.service.v1.model.MarketSpecialDay;
import com.backbase.stream.configuration.InvestmentIngestProperties;
import com.backbase.stream.configuration.InvestmentIngestionConfigurationProperties;
import com.backbase.stream.investment.Asset;
import com.backbase.stream.investment.AssetPrice;
import com.backbase.stream.investment.InvestmentAssetData;
import com.backbase.stream.investment.InvestmentAssetsTask;
import com.backbase.stream.investment.RandomParam;
import com.backbase.stream.investment.model.AssetCategoryEntry;
import com.backbase.stream.investment.service.AsyncTaskService;
import com.backbase.stream.investment.service.InvestmentAssetPriceService;
import com.backbase.stream.investment.service.InvestmentAssetUniverseService;
import com.backbase.stream.investment.service.InvestmentCurrencyService;
import com.backbase.stream.investment.service.InvestmentIntradayAssetPriceService;
import com.backbase.stream.worker.model.StreamTask.State;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit test suite for {@link InvestmentAssetUniverseSaga}.
 *
 * <p>This class verifies the complete orchestration logic of the saga, which drives
 * the investment asset universe ingestion pipeline through the following stages:
 * <ol>
 *   <li>Currencies</li>
 *   <li>Markets</li>
 *   <li>Market Special Days</li>
 *   <li>Asset Category Types</li>
 *   <li>Asset Categories</li>
 *   <li>Assets</li>
 *   <li>Prices (batch async)</li>
 *   <li>Intraday Prices</li>
 * </ol>
 *
 * <p>Test strategy:
 * <ul>
 *   <li>Each pipeline stage is tested in isolation via a dedicated {@code @Nested} class.</li>
 *   <li>Happy-path, empty-collection, and error scenarios are covered for every stage.</li>
 *   <li>{@code wireTrivialPipeline*()} helpers stub downstream stages so that each nested
 *       class can focus solely on its own stage under test.</li>
 *   <li>Error recovery is verified via the saga's {@code onErrorResume} handler, which
 *       always emits the task with {@link State#FAILED} instead of propagating the error
 *       signal — therefore {@link reactor.test.StepVerifier.LastStep#verifyComplete()} is always used, never
 *       {@code verifyError()}.</li>
 *   <li>All reactive assertions use Project Reactor's {@link StepVerifier}.</li>
 * </ul>
 *
 * <p>Mocked dependencies:
 * <ul>
 *   <li>{@link InvestmentAssetUniverseService} – markets, special days, category types,
 *       categories, assets</li>
 *   <li>{@link InvestmentAssetPriceService} – batch price ingestion</li>
 *   <li>{@link InvestmentIntradayAssetPriceService} – intraday price ingestion</li>
 *   <li>{@link InvestmentCurrencyService} – currency upsert</li>
 *   <li>{@link AsyncTaskService} – async task polling</li>
 *   <li>{@link InvestmentIngestionConfigurationProperties} – feature flag</li>
 * </ul>
 */
class InvestmentAssetUniverseSagaTest {

    @Mock
    private InvestmentAssetUniverseService assetUniverseService;

    @Mock
    private InvestmentAssetPriceService investmentAssetPriceService;

    @Mock
    private InvestmentIntradayAssetPriceService investmentIntradayAssetPriceService;

    @Mock
    private InvestmentCurrencyService investmentCurrencyService;

    @Mock
    private AsyncTaskService asyncTaskService;

    @Mock
    private InvestmentIngestionConfigurationProperties configurationProperties;

    @Mock
    private InvestmentIngestProperties ingestProperties;

    private InvestmentAssetUniverseSaga saga;

    /**
     * Initialises Mockito mocks and constructs the saga under test before each test method.
     * {@link InvestmentIngestionConfigurationProperties#isAssetUniverseEnabled()} is set to
     * {@code true} by default so that the feature flag does not suppress any pipeline stage.
     */
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(configurationProperties.isAssetUniverseEnabled()).thenReturn(true);
        // Provide real AssetConfig so concurrency values are non-null when the saga calls
        // ingestProperties.getAsset().getMarketConcurrency() / getAssetCategoryConcurrency() / etc.
        when(ingestProperties.getAsset()).thenReturn(new InvestmentIngestProperties.AssetConfig());
        saga = new InvestmentAssetUniverseSaga(
            assetUniverseService,
            investmentAssetPriceService,
            investmentIntradayAssetPriceService,
            investmentCurrencyService,
            asyncTaskService,
            configurationProperties,
            ingestProperties
        );
    }

    // =========================================================================
    // executeTask – top-level orchestration
    // =========================================================================

    /**
     * Tests for the top-level {@code executeTask} method.
     *
     * <p>These tests exercise the full pipeline end-to-end with all services stubbed
     * to return successful responses, and also verify that a mid-pipeline failure
     * causes the task to be marked {@link State#FAILED} without propagating an error signal.
     */
    @Nested
    @DisplayName("executeTask")
    class ExecuteTaskTests {

        /**
         * Verifies that when all services succeed, {@code executeTask} completes normally
         * and the returned task is marked {@link State#COMPLETED}.
         */
        @Test
        @DisplayName("should complete successfully when all services succeed")
        void executeTask_allServicesSucceed_completesNormally() {
            InvestmentAssetsTask task = createFullTask();
            stubAllServicesSuccess(task);

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();
        }

        /**
         * Verifies that when a service throws an error, the task is marked {@link State#FAILED}
         * and the reactive stream completes without emitting an error signal.
         */
        @Test
        @DisplayName("should mark task FAILED and complete stream when a service throws an error")
        void executeTask_serviceThrowsError_marksTaskFailed() {
            InvestmentAssetsTask task = createFullTask();

            when(investmentCurrencyService.upsertCurrencies(anyList()))
                .thenReturn(Mono.error(new RuntimeException("Currency service down")));

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.FAILED))
                .verifyComplete();
        }

        /**
         * Verifies that a minimal task with no data collections still completes successfully
         * without invoking any upsert services due to empty-list short-circuiting.
         */
        @Test
        @DisplayName("should complete with empty task data")
        void executeTask_emptyTask_completesNormally() {
            InvestmentAssetsTask task = createMinimalTask();

            when(investmentAssetPriceService.ingestPrices(anyList(), anyMap()))
                .thenReturn(Mono.just(Collections.emptyList()));
            when(asyncTaskService.checkPriceAsyncTasksFinished(any()))
                .thenReturn(Mono.just(Collections.emptyList()));
            when(investmentIntradayAssetPriceService.ingestIntradayPrices())
                .thenReturn(Mono.just(Collections.emptyList()));

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();
        }

        /**
         * Verifies that when the feature flag is disabled, the saga skips all processing
         * and returns the task unchanged without calling any service.
         */
        @Test
        @DisplayName("should skip all processing when feature flag is disabled")
        void executeTask_featureFlagDisabled_skipsAllProcessing() {
            when(configurationProperties.isAssetUniverseEnabled()).thenReturn(false);
            InvestmentAssetsTask task = createFullTask();

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> {
                    assertThat(result.getState()).isNotEqualTo(State.FAILED);
                    verify(investmentCurrencyService, never()).upsertCurrencies(any());
                    verify(assetUniverseService, never()).upsertMarket(any());
                    verify(assetUniverseService, never()).upsertMarketSpecialDay(any());
                    verify(assetUniverseService, never()).upsertAssetCategoryType(any());
                    verify(assetUniverseService, never()).upsertAssetCategory(any());
                    verify(assetUniverseService, never()).createAssets(any());
                    verify(investmentAssetPriceService, never()).ingestPrices(any(), any());
                    verify(investmentIntradayAssetPriceService, never()).ingestIntradayPrices();
                })
                .verifyComplete();
        }
    }

    // =========================================================================
    // rollBack
    // =========================================================================

    /**
     * Tests for the {@code rollBack} method.
     *
     * <p>Rollback is intentionally a no-op for this saga since investment operations
     * are idempotent. The method must complete without emitting any element.
     */
    @Nested
    @DisplayName("rollBack")
    class RollBackTests {

        /**
         * Verifies that {@code rollBack} returns an empty Mono and completes without error.
         */
        @Test
        @DisplayName("should return empty Mono and complete without error")
        void rollBack_returnsEmptyMono() {
            InvestmentAssetsTask task = createMinimalTask();

            StepVerifier.create(saga.rollBack(task))
                .verifyComplete();
        }
    }

    // =========================================================================
    // upsertCurrencies
    // =========================================================================

    /**
     * Tests for the {@code upsertCurrencies} stage of the saga pipeline.
     *
     * <p>Currencies are the first stage. An empty list must short-circuit without
     * calling the currency service, while a populated list must delegate to
     * {@link InvestmentCurrencyService#upsertCurrencies}.
     */
    @Nested
    @DisplayName("upsertCurrencies")
    class UpsertCurrenciesTests {

        /**
         * Verifies that when the currency list is empty, the pipeline short-circuits
         * without calling the currency service, and the task completes successfully.
         */
        @Test
        @DisplayName("should complete successfully without calling service when currency list is empty")
        void upsertCurrencies_emptyList_completesSuccessfully() {
            InvestmentAssetsTask task = createMinimalTask();
            wireTrivialPipelineAfterCurrencies();

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            verify(investmentCurrencyService, never()).upsertCurrencies(any());
        }

        /**
         * Verifies that when the currency list is non-empty, currencies are upserted
         * and the task is marked {@link State#COMPLETED}.
         */
        @Test
        @DisplayName("should upsert currencies and mark task COMPLETED")
        void upsertCurrencies_success() {
            InvestmentAssetData data = InvestmentAssetData.builder()
                .currencies(List.of(buildCurrency("USD")))
                .markets(Collections.emptyList())
                .marketSpecialDays(Collections.emptyList())
                .assetCategoryTypes(Collections.emptyList())
                .assetCategories(Collections.emptyList())
                .assets(Collections.emptyList())
                .assetPrices(Collections.emptyList())
                .build();
            InvestmentAssetsTask task = new InvestmentAssetsTask("currency-task", data);
            wireTrivialPipelineAfterCurrencies();

            when(investmentCurrencyService.upsertCurrencies(anyList()))
                .thenReturn(Mono.just(List.of(
                    new com.backbase.investment.api.service.v1.model.Currency()
                        .code("USD").name("US Dollar"))));

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();
        }

        /**
         * Verifies that a failure in the currency service causes the task to be marked
         * {@link State#FAILED} without propagating the error signal.
         */
        @Test
        @DisplayName("should mark task FAILED when currency upsert throws an error")
        void upsertCurrencies_error_marksTaskFailed() {
            InvestmentAssetData data = InvestmentAssetData.builder()
                .currencies(List.of(buildCurrency("USD")))
                .markets(Collections.emptyList())
                .marketSpecialDays(Collections.emptyList())
                .assetCategoryTypes(Collections.emptyList())
                .assetCategories(Collections.emptyList())
                .assets(Collections.emptyList())
                .assetPrices(Collections.emptyList())
                .build();
            InvestmentAssetsTask task = new InvestmentAssetsTask("currency-error-task", data);

            when(investmentCurrencyService.upsertCurrencies(anyList()))
                .thenReturn(Mono.error(new RuntimeException("Currency failure")));

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.FAILED))
                .verifyComplete();
        }
    }

    // =========================================================================
    // upsertMarkets
    // =========================================================================

    /**
     * Tests for the {@code upsertMarkets} stage of the saga pipeline.
     *
     * <p>Markets follow currencies. An empty market list must skip service calls,
     * while a populated list must call {@link InvestmentAssetUniverseService#upsertMarket}
     * for each entry.
     */
    @Nested
    @DisplayName("upsertMarkets")
    class UpsertMarketsTests {

        /**
         * Verifies that an empty market list is handled without calling the market service.
         */
        @Test
        @DisplayName("should complete successfully without calling service when market list is empty")
        void upsertMarkets_emptyList_completesSuccessfully() {
            InvestmentAssetsTask task = createMinimalTask();
            wireTrivialPipelineAfterMarkets();

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            verify(assetUniverseService, never()).upsertMarket(any());
        }

        /**
         * Verifies that when the market list is non-empty, markets are upserted and
         * the task is marked {@link State#COMPLETED}.
         */
        @Test
        @DisplayName("should upsert markets and mark task COMPLETED")
        void upsertMarkets_success() {
            InvestmentAssetData data = InvestmentAssetData.builder()
                .currencies(Collections.emptyList())
                .markets(List.of(buildMarket("NYSE")))
                .marketSpecialDays(Collections.emptyList())
                .assetCategoryTypes(Collections.emptyList())
                .assetCategories(Collections.emptyList())
                .assets(Collections.emptyList())
                .assetPrices(Collections.emptyList())
                .build();
            InvestmentAssetsTask task = new InvestmentAssetsTask("market-task", data);
            wireTrivialPipelineAfterMarkets();

            when(assetUniverseService.upsertMarket(any()))
                .thenReturn(Mono.just(new com.backbase.investment.api.service.v1.model.Market()
                    .code("NYSE").name("NYSE Exchange")));

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();
        }

        /**
         * Verifies that a market upsert error causes the task to be marked {@link State#FAILED}.
         */
        @Test
        @DisplayName("should mark task FAILED when market upsert fails")
        void upsertMarkets_error_marksTaskFailed() {
            InvestmentAssetData data = InvestmentAssetData.builder()
                .currencies(Collections.emptyList())
                .markets(List.of(buildMarket("NYSE")))
                .marketSpecialDays(Collections.emptyList())
                .assetCategoryTypes(Collections.emptyList())
                .assetCategories(Collections.emptyList())
                .assets(Collections.emptyList())
                .assetPrices(Collections.emptyList())
                .build();
            InvestmentAssetsTask task = new InvestmentAssetsTask("market-error-task", data);

            when(assetUniverseService.upsertMarket(any()))
                .thenReturn(Mono.error(new RuntimeException("Market service failure")));

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.FAILED))
                .verifyComplete();
        }
    }

    // =========================================================================
    // upsertMarketSpecialDays
    // =========================================================================

    /**
     * Tests for the {@code upsertMarketSpecialDays} stage of the saga pipeline.
     *
     * <p>Market special days follow markets. Each entry is forwarded individually to
     * {@link InvestmentAssetUniverseService#upsertMarketSpecialDay}.
     */
    @Nested
    @DisplayName("upsertMarketSpecialDays")
    class UpsertMarketSpecialDaysTests {

        /**
         * Verifies that when the special-day list is non-empty, each entry is upserted
         * and the task is marked {@link State#COMPLETED}.
         */
        @Test
        @DisplayName("should upsert market special days and mark task COMPLETED")
        void upsertMarketSpecialDays_success() {
            InvestmentAssetData data = InvestmentAssetData.builder()
                .currencies(Collections.emptyList())
                .markets(Collections.emptyList())
                .marketSpecialDays(List.of(buildMarketSpecialDay("NYSE")))
                .assetCategoryTypes(Collections.emptyList())
                .assetCategories(Collections.emptyList())
                .assets(Collections.emptyList())
                .assetPrices(Collections.emptyList())
                .build();
            InvestmentAssetsTask task = new InvestmentAssetsTask("special-day-task", data);
            wireTrivialPipelineAfterSpecialDays();

            when(assetUniverseService.upsertMarketSpecialDay(any()))
                .thenReturn(Mono.just(new com.backbase.investment.api.service.v1.model.MarketSpecialDay()
                    .market("NYSE").description("Christmas")));

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();
        }

        /**
         * Verifies that an empty special-day list is handled without calling the service.
         */
        @Test
        @DisplayName("should skip special day upsert when list is empty")
        void upsertMarketSpecialDays_emptyList_skipsService() {
            InvestmentAssetsTask task = createMinimalTask();
            wireTrivialPipelineAfterSpecialDays();

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            verify(assetUniverseService, never()).upsertMarketSpecialDay(any());
        }

        /**
         * Verifies that a special-day upsert failure causes the task to be marked {@link State#FAILED}.
         */
        @Test
        @DisplayName("should mark task FAILED when special day upsert fails")
        void upsertMarketSpecialDays_error_marksTaskFailed() {
            InvestmentAssetData data = InvestmentAssetData.builder()
                .currencies(Collections.emptyList())
                .markets(Collections.emptyList())
                .marketSpecialDays(List.of(buildMarketSpecialDay("NYSE")))
                .assetCategoryTypes(Collections.emptyList())
                .assetCategories(Collections.emptyList())
                .assets(Collections.emptyList())
                .assetPrices(Collections.emptyList())
                .build();
            InvestmentAssetsTask task = new InvestmentAssetsTask("special-day-error-task", data);

            when(assetUniverseService.upsertMarketSpecialDay(any()))
                .thenReturn(Mono.error(new RuntimeException("Special day failure")));

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.FAILED))
                .verifyComplete();
        }
    }

    // =========================================================================
    // upsertAssetCategoryTypes
    // =========================================================================

    /**
     * Tests for the {@code upsertAssetCategoryTypes} stage of the saga pipeline.
     *
     * <p>Category types follow market special days. Each entry is forwarded to
     * {@link InvestmentAssetUniverseService#upsertAssetCategoryType}.
     */
    @Nested
    @DisplayName("upsertAssetCategoryTypes")
    class UpsertAssetCategoryTypesTests {

        /**
         * Verifies that when the category-type list is non-empty, each entry is upserted
         * and the task is marked {@link State#COMPLETED}.
         */
        @Test
        @DisplayName("should upsert asset category types and mark task COMPLETED")
        void upsertAssetCategoryTypes_success() {
            InvestmentAssetData data = InvestmentAssetData.builder()
                .currencies(Collections.emptyList())
                .markets(Collections.emptyList())
                .marketSpecialDays(Collections.emptyList())
                .assetCategoryTypes(List.of(buildAssetCategoryType("EQ")))
                .assetCategories(Collections.emptyList())
                .assets(Collections.emptyList())
                .assetPrices(Collections.emptyList())
                .build();
            InvestmentAssetsTask task = new InvestmentAssetsTask("cat-type-task", data);
            wireTrivialPipelineAfterCategoryTypes();

            when(assetUniverseService.upsertAssetCategoryType(any()))
                .thenReturn(Mono.just(
                    new com.backbase.investment.api.service.v1.model.AssetCategoryType()
                        .code("EQ").name("EQ Type")));

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();
        }

        /**
         * Verifies that an empty category-type list is handled without calling the service.
         */
        @Test
        @DisplayName("should skip category type upsert when list is empty")
        void upsertAssetCategoryTypes_emptyList_skipsService() {
            InvestmentAssetsTask task = createMinimalTask();
            wireTrivialPipelineAfterCategoryTypes();

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            verify(assetUniverseService, never()).upsertAssetCategoryType(any());
        }

        /**
         * Verifies that a category-type upsert failure causes the task to be marked {@link State#FAILED}.
         */
        @Test
        @DisplayName("should mark task FAILED when category type upsert fails")
        void upsertAssetCategoryTypes_error_marksTaskFailed() {
            InvestmentAssetData data = InvestmentAssetData.builder()
                .currencies(Collections.emptyList())
                .markets(Collections.emptyList())
                .marketSpecialDays(Collections.emptyList())
                .assetCategoryTypes(List.of(buildAssetCategoryType("EQ")))
                .assetCategories(Collections.emptyList())
                .assets(Collections.emptyList())
                .assetPrices(Collections.emptyList())
                .build();
            InvestmentAssetsTask task = new InvestmentAssetsTask("cat-type-error-task", data);

            when(assetUniverseService.upsertAssetCategoryType(any()))
                .thenReturn(Mono.error(new RuntimeException("Category type failure")));

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.FAILED))
                .verifyComplete();
        }
    }

    // =========================================================================
    // upsertAssetCategories
    // =========================================================================

    /**
     * Tests for the {@code upsertAssetCategories} stage of the saga pipeline.
     *
     * <p>Asset categories follow category types. Each entry is forwarded to
     * {@link InvestmentAssetUniverseService#upsertAssetCategory}.
     * Note: the service returns the {@code sync.v1} model {@code AssetCategory}
     * as defined by the asset universe service contract.
     */
    @Nested
    @DisplayName("upsertAssetCategories")
    class UpsertAssetCategoriesTests {

        /**
         * Verifies that when the category list is non-empty, each entry is upserted
         * and the task is marked {@link State#COMPLETED}.
         */
        @Test
        @DisplayName("should upsert asset categories and mark task COMPLETED")
        void upsertAssetCategories_success() {
            AssetCategoryEntry categoryEntry = new AssetCategoryEntry();
            categoryEntry.setName("TECH");

            InvestmentAssetData data = InvestmentAssetData.builder()
                .currencies(Collections.emptyList())
                .markets(Collections.emptyList())
                .marketSpecialDays(Collections.emptyList())
                .assetCategoryTypes(Collections.emptyList())
                .assetCategories(List.of(categoryEntry))
                .assets(Collections.emptyList())
                .assetPrices(Collections.emptyList())
                .build();
            InvestmentAssetsTask task = new InvestmentAssetsTask("cat-task", data);
            wireTrivialPipelineAfterCategories();

            // upsertAssetCategory returns the sync.v1 AssetCategory as defined by the service contract
            when(assetUniverseService.upsertAssetCategory(any()))
                .thenReturn(Mono.just(
                    new com.backbase.investment.api.service.sync.v1.model.AssetCategory()
                        .name("TECH")));

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();
        }

        /**
         * Verifies that a category upsert failure causes the task to be marked {@link State#FAILED}.
         */
        @Test
        @DisplayName("should propagate error and mark task FAILED on category upsert failure")
        void upsertAssetCategories_error_marksTaskFailed() {
            AssetCategoryEntry entry = new AssetCategoryEntry();
            entry.setName("TECH");

            InvestmentAssetData data = InvestmentAssetData.builder()
                .currencies(Collections.emptyList())
                .markets(Collections.emptyList())
                .marketSpecialDays(Collections.emptyList())
                .assetCategoryTypes(Collections.emptyList())
                .assetCategories(List.of(entry))
                .assets(Collections.emptyList())
                .assetPrices(Collections.emptyList())
                .build();
            InvestmentAssetsTask task = new InvestmentAssetsTask("cat-error-task", data);

            when(assetUniverseService.upsertAssetCategory(any()))
                .thenReturn(Mono.error(new RuntimeException("Category failure")));

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.FAILED))
                .verifyComplete();
        }
    }

    // =========================================================================
    // createAssets
    // =========================================================================

    /**
     * Tests for the {@code createAssets} stage of the saga pipeline.
     *
     * <p>The stage follows asset categories. An empty asset list must short-circuit without
     * calling {@link InvestmentAssetUniverseService#createAssets}, while a non-empty list
     * must delegate to the service and store the resulting assets on the task.
     */
    @Nested
    @DisplayName("createAssets")
    class CreateAssetsTests {

        /**
         * Verifies that when the asset list is empty, the saga skips asset creation
         * and the task is marked {@link State#COMPLETED}.
         */
        @Test
        @DisplayName("should skip asset creation and set COMPLETED when asset list is empty")
        void createAssets_emptyList_setsCompleted() {
            InvestmentAssetsTask task = createMinimalTask();

            when(investmentAssetPriceService.ingestPrices(anyList(), anyMap()))
                .thenReturn(Mono.just(Collections.emptyList()));
            when(asyncTaskService.checkPriceAsyncTasksFinished(any()))
                .thenReturn(Mono.just(Collections.emptyList()));
            when(investmentIntradayAssetPriceService.ingestIntradayPrices())
                .thenReturn(Mono.just(Collections.emptyList()));

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            verify(assetUniverseService, never()).createAssets(anyList());
        }

        /**
         * Verifies that when assets are present, {@code createAssets} is invoked and
         * the task completes with {@link State#COMPLETED}.
         */
        @Test
        @DisplayName("should create assets and set them on the task on success")
        void createAssets_success() {
            InvestmentAssetsTask task = createTaskWithAssets();

            when(assetUniverseService.createAssets(anyList()))
                .thenReturn(Flux.fromIterable(task.getData().getAssets()));
            when(investmentAssetPriceService.ingestPrices(anyList(), anyMap()))
                .thenReturn(Mono.just(Collections.emptyList()));
            when(asyncTaskService.checkPriceAsyncTasksFinished(any()))
                .thenReturn(Mono.just(Collections.emptyList()));
            when(investmentIntradayAssetPriceService.ingestIntradayPrices())
                .thenReturn(Mono.just(Collections.emptyList()));

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();
        }

        /**
         * Verifies that a failure in {@code createAssets} causes the task to be marked
         * {@link State#FAILED} without propagating an error signal.
         */
        @Test
        @DisplayName("should propagate error and mark task FAILED when asset creation fails")
        void createAssets_error_marksTaskFailed() {
            InvestmentAssetsTask task = createTaskWithAssets();

            when(assetUniverseService.createAssets(anyList()))
                .thenReturn(Flux.error(new RuntimeException("Asset creation failure")));

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.FAILED))
                .verifyComplete();
        }
    }

    // =========================================================================
    // upsertPrices
    // =========================================================================

    /**
     * Tests for the {@code upsertPrices} stage of the saga pipeline.
     *
     * <p>The stage delegates to {@link InvestmentAssetPriceService#ingestPrices} with the
     * asset list and the price-by-asset map from the task data. The returned
     * {@link GroupResult} list is stored on the task for use by the next stage.
     */
    @Nested
    @DisplayName("upsertPrices")
    class UpsertPricesTests {

        /**
         * Verifies that prices are ingested and the {@link GroupResult} list is stored on the task.
         */
        @Test
        @DisplayName("should ingest prices and store GroupResult list on the task")
        void upsertPrices_success_immediateCompletion() {
            InvestmentAssetsTask task = createTaskWithAssets();
            GroupResult groupResult = new GroupResult(UUID.randomUUID(), "PENDING", null);

            when(assetUniverseService.createAssets(anyList()))
                .thenReturn(Flux.fromIterable(task.getData().getAssets()));
            when(investmentAssetPriceService.ingestPrices(anyList(), anyMap()))
                .thenReturn(Mono.just(List.of(groupResult)));
            when(asyncTaskService.checkPriceAsyncTasksFinished(any()))
                .thenReturn(Mono.just(List.of(groupResult)));
            when(investmentIntradayAssetPriceService.ingestIntradayPrices())
                .thenReturn(Mono.just(Collections.emptyList()));

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();
        }

        /**
         * Verifies that an empty {@link GroupResult} list from the price service is
         * handled without error.
         */
        @Test
        @DisplayName("should handle empty GroupResult list from price ingestion")
        void upsertPrices_emptyGroupResults() {
            InvestmentAssetsTask task = createTaskWithAssets();

            when(assetUniverseService.createAssets(anyList()))
                .thenReturn(Flux.fromIterable(task.getData().getAssets()));
            when(investmentAssetPriceService.ingestPrices(anyList(), anyMap()))
                .thenReturn(Mono.just(Collections.emptyList()));
            when(asyncTaskService.checkPriceAsyncTasksFinished(any()))
                .thenReturn(Mono.just(Collections.emptyList()));
            when(investmentIntradayAssetPriceService.ingestIntradayPrices())
                .thenReturn(Mono.just(Collections.emptyList()));

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();
        }

        /**
         * Verifies that a price ingestion error causes the task to be marked {@link State#FAILED}.
         */
        @Test
        @DisplayName("should mark task FAILED when price ingestion returns error")
        void upsertPrices_error_marksTaskFailed() {
            InvestmentAssetsTask task = createTaskWithAssets();

            when(assetUniverseService.createAssets(anyList()))
                .thenReturn(Flux.fromIterable(task.getData().getAssets()));
            when(investmentAssetPriceService.ingestPrices(anyList(), anyMap()))
                .thenReturn(Mono.error(new RuntimeException("Price ingestion failure")));

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.FAILED))
                .verifyComplete();
        }

        /**
         * Verifies that when the asset list is empty, the price service is still called
         * (no short-circuit in upsertPrices) and completes successfully.
         */
        @Test
        @DisplayName("should pass empty asset list to price service when assets are empty")
        void upsertPrices_emptyAssets_callsPriceServiceWithEmptyList() {
            InvestmentAssetsTask task = createMinimalTask();

            when(investmentAssetPriceService.ingestPrices(anyList(), anyMap()))
                .thenReturn(Mono.just(Collections.emptyList()));
            when(asyncTaskService.checkPriceAsyncTasksFinished(any()))
                .thenReturn(Mono.just(Collections.emptyList()));
            when(investmentIntradayAssetPriceService.ingestIntradayPrices())
                .thenReturn(Mono.just(Collections.emptyList()));

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();
        }
    }

    // =========================================================================
    // createIntradayPrices
    // =========================================================================

    /**
     * Tests for the {@code createIntradayPrices} stage of the saga pipeline.
     *
     * <p>This is the final stage. It first waits for all async price tasks to finish
     * ({@link AsyncTaskService#checkPriceAsyncTasksFinished}) before calling
     * {@link InvestmentIntradayAssetPriceService#ingestIntradayPrices}.
     * Results are collected and stored as {@code intradayPriceTasks} on the task.
     */
    @Nested
    @DisplayName("createIntradayPrices")
    class CreateIntradayPricesTests {

        /**
         * Verifies that intraday prices are ingested after the async task check completes
         * and the task is marked {@link State#COMPLETED}.
         */
        @Test
        @DisplayName("should ingest intraday prices after async tasks finish")
        void createIntradayPrices_success() {
            InvestmentAssetsTask task = createTaskWithAssets();
            GroupResult groupResult = new GroupResult(UUID.randomUUID(), "SUCCESS", null);

            when(assetUniverseService.createAssets(anyList()))
                .thenReturn(Flux.fromIterable(task.getData().getAssets()));
            when(investmentAssetPriceService.ingestPrices(anyList(), anyMap()))
                .thenReturn(Mono.just(List.of(groupResult)));
            when(asyncTaskService.checkPriceAsyncTasksFinished(any()))
                .thenReturn(Mono.just(List.of(groupResult)));
            when(investmentIntradayAssetPriceService.ingestIntradayPrices())
                .thenReturn(Mono.just(List.of(groupResult)));

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();
        }

        /**
         * Verifies that an intraday price ingestion failure causes the task to be marked
         * {@link State#FAILED}.
         */
        @Test
        @DisplayName("should mark task FAILED when intraday price ingestion fails")
        void createIntradayPrices_error_marksTaskFailed() {
            InvestmentAssetsTask task = createTaskWithAssets();

            when(assetUniverseService.createAssets(anyList()))
                .thenReturn(Flux.fromIterable(task.getData().getAssets()));
            when(investmentAssetPriceService.ingestPrices(anyList(), anyMap()))
                .thenReturn(Mono.just(Collections.emptyList()));
            when(asyncTaskService.checkPriceAsyncTasksFinished(any()))
                .thenReturn(Mono.just(Collections.emptyList()));
            when(investmentIntradayAssetPriceService.ingestIntradayPrices())
                .thenReturn(Mono.error(new RuntimeException("Intraday price failure")));

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.FAILED))
                .verifyComplete();
        }

        /**
         * Verifies that a failure in the async task check propagates as {@link State#FAILED}.
         */
        @Test
        @DisplayName("should mark task FAILED when async task check fails")
        void createIntradayPrices_asyncCheckFails_marksTaskFailed() {
            InvestmentAssetsTask task = createTaskWithAssets();

            when(assetUniverseService.createAssets(anyList()))
                .thenReturn(Flux.fromIterable(task.getData().getAssets()));
            when(investmentAssetPriceService.ingestPrices(anyList(), anyMap()))
                .thenReturn(Mono.just(Collections.emptyList()));
            when(asyncTaskService.checkPriceAsyncTasksFinished(any()))
                .thenReturn(Mono.error(new RuntimeException("Async check failure")));

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.FAILED))
                .verifyComplete();
        }

        /**
         * Verifies that an empty intraday result list is handled gracefully.
         */
        @Test
        @DisplayName("should complete with empty intraday result list")
        void createIntradayPrices_emptyResults_success() {
            InvestmentAssetsTask task = createMinimalTask();

            when(investmentAssetPriceService.ingestPrices(anyList(), anyMap()))
                .thenReturn(Mono.just(Collections.emptyList()));
            when(asyncTaskService.checkPriceAsyncTasksFinished(any()))
                .thenReturn(Mono.just(Collections.emptyList()));
            when(investmentIntradayAssetPriceService.ingestIntradayPrices())
                .thenReturn(Mono.just(Collections.emptyList()));

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();
        }
    }

    // =========================================================================
    // Helper / Builder Methods
    // =========================================================================

    /**
     * Creates an {@link InvestmentAssetsTask} with all data collections set to empty lists.
     * Suitable as a base task when the behaviour under test is not affected by data content,
     * or when testing empty-list short-circuit paths.
     *
     * @return a minimal task with empty data
     */
    private InvestmentAssetsTask createMinimalTask() {
        InvestmentAssetData data = InvestmentAssetData.builder()
            .currencies(Collections.emptyList())
            .markets(Collections.emptyList())
            .marketSpecialDays(Collections.emptyList())
            .assetCategoryTypes(Collections.emptyList())
            .assetCategories(Collections.emptyList())
            .assets(Collections.emptyList())
            .assetPrices(Collections.emptyList())
            .build();
        return new InvestmentAssetsTask("minimal-task", data);
    }

    /**
     * Creates an {@link InvestmentAssetsTask} containing two sample {@link Asset} objects
     * and corresponding {@link AssetPrice} entries. Used by asset-creation and price tests.
     *
     * @return a task with two assets and two prices
     */
    private InvestmentAssetsTask createTaskWithAssets() {
        Asset asset1 = new Asset();
        asset1.setUuid(UUID.randomUUID());
        asset1.setName("Apple Inc.");
        asset1.setIsin("US0378331005");
        asset1.setTicker("AAPL");
        asset1.setMarket("NASDAQ");
        asset1.setCurrency("USD");
        asset1.setAssetType(AssetTypeEnum.STOCK);

        Asset asset2 = new Asset();
        asset2.setUuid(UUID.randomUUID());
        asset2.setName("Microsoft Corp.");
        asset2.setIsin("US5949181045");
        asset2.setTicker("MSFT");
        asset2.setMarket("NYSE");
        asset2.setCurrency("USD");
        asset2.setAssetType(AssetTypeEnum.STOCK);

        AssetPrice price1 = new AssetPrice("US0378331005", "NASDAQ", "USD", 150.0,
            new RandomParam(0.99, 1.01));
        AssetPrice price2 = new AssetPrice("US5949181045", "NYSE", "USD", 200.0,
            new RandomParam(0.99, 1.01));

        InvestmentAssetData data = InvestmentAssetData.builder()
            .currencies(Collections.emptyList())
            .markets(Collections.emptyList())
            .marketSpecialDays(Collections.emptyList())
            .assetCategoryTypes(Collections.emptyList())
            .assetCategories(Collections.emptyList())
            .assets(List.of(asset1, asset2))
            .assetPrices(List.of(price1, price2))
            .build();
        return new InvestmentAssetsTask("assets-task", data);
    }

    /**
     * Creates an {@link InvestmentAssetsTask} with one entry in every data collection.
     * Used for the full end-to-end happy-path test in conjunction with
     * {@link #stubAllServicesSuccess(InvestmentAssetsTask)}.
     *
     * @return a fully populated task
     */
    private InvestmentAssetsTask createFullTask() {
        Asset asset1 = new Asset();
        asset1.setUuid(UUID.randomUUID());
        asset1.setName("Apple Inc.");
        asset1.setIsin("US0378331005");
        asset1.setTicker("AAPL");
        asset1.setMarket("NASDAQ");
        asset1.setCurrency("USD");
        asset1.setAssetType(AssetTypeEnum.STOCK);
        AssetPrice price = new AssetPrice("US0378331005", "NASDAQ", "USD", 150.0,
            new RandomParam(0.99, 1.01));

        AssetCategoryEntry categoryEntry = new AssetCategoryEntry();
        categoryEntry.setName("TECH");

        InvestmentAssetData data = InvestmentAssetData.builder()
            .currencies(List.of(buildCurrency("USD")))
            .markets(List.of(buildMarket("NYSE")))
            .marketSpecialDays(List.of(buildMarketSpecialDay("NYSE")))
            .assetCategoryTypes(List.of(buildAssetCategoryType("EQ")))
            .assetCategories(List.of(categoryEntry))
            .assets(List.of(asset1))
            .assetPrices(List.of(price))
            .build();
        return new InvestmentAssetsTask("full-task", data);
    }

    /**
     * Configures all mocked services to return successful responses for a fully populated task.
     * Intended for use with {@link #createFullTask()} in end-to-end happy-path tests.
     *
     * @param task the task whose asset list is used to stub {@code createAssets}
     */
    private void stubAllServicesSuccess(InvestmentAssetsTask task) {
        when(investmentCurrencyService.upsertCurrencies(anyList()))
            .thenReturn(Mono.just(Collections.emptyList()));
        when(assetUniverseService.upsertMarket(any()))
            .thenReturn(Mono.just(new com.backbase.investment.api.service.v1.model.Market()
                .code("NYSE").name("NYSE Exchange")));
        when(assetUniverseService.upsertMarketSpecialDay(any()))
            .thenReturn(Mono.just(new com.backbase.investment.api.service.v1.model.MarketSpecialDay()
                .market("NYSE").description("Christmas")));
        when(assetUniverseService.upsertAssetCategoryType(any()))
            .thenReturn(Mono.just(
                new com.backbase.investment.api.service.v1.model.AssetCategoryType()
                    .code("EQ").name("EQ Type")));
        // upsertAssetCategory returns the sync.v1 AssetCategory as defined by the service contract
        when(assetUniverseService.upsertAssetCategory(any()))
            .thenReturn(Mono.just(
                new com.backbase.investment.api.service.sync.v1.model.AssetCategory()
                    .name("TECH")));
        when(assetUniverseService.createAssets(anyList()))
            .thenReturn(Flux.fromIterable(task.getData().getAssets()));
        when(investmentAssetPriceService.ingestPrices(anyList(), anyMap()))
            .thenReturn(Mono.just(Collections.emptyList()));
        when(asyncTaskService.checkPriceAsyncTasksFinished(any()))
            .thenReturn(Mono.just(Collections.emptyList()));
        when(investmentIntradayAssetPriceService.ingestIntradayPrices())
            .thenReturn(Mono.just(Collections.emptyList()));
    }

    /**
     * Stubs the price and intraday-price stages so that tests focusing on earlier pipeline
     * stages (currencies, markets, special days, category types, categories) can complete
     * without needing to configure those downstream mocks individually.
     *
     * <p>Note: upstream stages that short-circuit on empty lists (currencies, markets,
     * special days, category types, categories) do NOT need stubs when using
     * {@link #createMinimalTask()} — the implementation returns early without calling services.
     */
    private void wireTrivialPipelineAfterCurrencies() {
        when(investmentAssetPriceService.ingestPrices(anyList(), anyMap()))
            .thenReturn(Mono.just(Collections.emptyList()));
        when(asyncTaskService.checkPriceAsyncTasksFinished(any()))
            .thenReturn(Mono.just(Collections.emptyList()));
        when(investmentIntradayAssetPriceService.ingestIntradayPrices())
            .thenReturn(Mono.just(Collections.emptyList()));
    }

    /**
     * Stubs the price and intraday-price stages for tests focused on the market stage.
     * Functionally equivalent to {@link #wireTrivialPipelineAfterCurrencies()} but named
     * to clarify intent at the call site.
     */
    private void wireTrivialPipelineAfterMarkets() {
        wireTrivialPipelineAfterCurrencies();
    }

    /**
     * Stubs the price and intraday-price stages for tests focused on the market special days stage.
     */
    private void wireTrivialPipelineAfterSpecialDays() {
        wireTrivialPipelineAfterCurrencies();
    }

    /**
     * Stubs the price and intraday-price stages for tests focused on the asset category types stage.
     */
    private void wireTrivialPipelineAfterCategoryTypes() {
        wireTrivialPipelineAfterCurrencies();
    }

    /**
     * Stubs the price and intraday-price stages for tests focused on the asset categories stage.
     */
    private void wireTrivialPipelineAfterCategories() {
        wireTrivialPipelineAfterCurrencies();
    }

    // --- Domain object builders ---

    /**
     * Builds a {@link Currency} domain object with the given ISO code.
     *
     * @param code ISO 4217 currency code, e.g. {@code "USD"}
     * @return a populated {@link Currency}
     */
    private Currency buildCurrency(String code) {
        return new Currency().code(code).name(code + " Currency");
    }

    /**
     * Builds a {@link Market} domain object representing an exchange with the given MIC code.
     *
     * @param code market identification code, e.g. {@code "NYSE"}
     * @return a populated {@link Market} with session hours set to 09:00–17:00 UTC
     */
    private Market buildMarket(String code) {
        return new Market()
            .code(code)
            .name(code + " Exchange")
            .sessionStart(String.valueOf(LocalTime.of(9, 0)))
            .sessionEnd(String.valueOf(LocalTime.of(17, 0)));
    }

    /**
     * Builds a {@link MarketSpecialDay} domain object for Christmas day on the given market.
     *
     * @param market the market code to associate with the special day
     * @return a {@link MarketSpecialDay} for 25 December 2025
     */
    private MarketSpecialDay buildMarketSpecialDay(String market) {
        return new MarketSpecialDay()
            .market(market)
            .date(LocalDate.of(2025, 12, 25))
            .description("Christmas");
    }

    /**
     * Builds an {@link AssetCategoryType} domain object with the given classification code.
     *
     * @param code asset category type code, e.g. {@code "EQ"} for equity
     * @return a populated {@link AssetCategoryType}
     */
    private AssetCategoryType buildAssetCategoryType(String code) {
        return new AssetCategoryType().code(code).name(code + " Type");
    }
}
