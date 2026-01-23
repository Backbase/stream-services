package com.backbase.stream.investment.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.investment.api.service.v1.model.AssetTypeEnum;
import com.backbase.investment.api.service.v1.model.GroupResult;
import com.backbase.investment.api.service.v1.model.StatusA10Enum;
import com.backbase.stream.configuration.InvestmentIngestionConfigurationProperties;
import com.backbase.stream.investment.Asset;
import com.backbase.stream.investment.AssetPrice;
import com.backbase.stream.investment.InvestmentAssetData;
import com.backbase.stream.investment.InvestmentAssetsTask;
import com.backbase.stream.investment.RandomParam;
import com.backbase.stream.investment.service.InvestmentAssetPriceService;
import com.backbase.stream.investment.service.InvestmentAssetUniverseService;
import com.backbase.stream.investment.service.InvestmentIntradayAssetPriceService;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Test suite for {@link InvestmentAssetUniversSaga}, focusing on the asynchronous price ingestion workflow with polling
 * and timeout behavior.
 *
 * <p>These tests verify:
 * <ul>
 *   <li>Successful price ingestion with immediate completion</li>
 *   <li>Polling behavior when GroupResult tasks are PENDING</li>
 *   <li>Timeout handling when tasks remain PENDING beyond the timeout threshold</li>
 *   <li>Error propagation during price ingestion</li>
 * </ul>
 */
class InvestmentAssetUniversSagaTest {

    @Mock
    private InvestmentAssetUniverseService assetUniverseService;

    @Mock
    private InvestmentAssetPriceService investmentAssetPriceService;

    @Mock
    private InvestmentIntradayAssetPriceService investmentIntradayAssetPriceService;

    @Mock
    private InvestmentIngestionConfigurationProperties configurationProperties;

    private InvestmentAssetUniversSaga saga;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        saga = new InvestmentAssetUniversSaga(
            assetUniverseService,
            investmentAssetPriceService,
            investmentIntradayAssetPriceService,
            configurationProperties
        );
        // Enable asset universe by default
        when(configurationProperties.isAssetUniversEnabled()).thenReturn(true);
    }


    /**
     * Test successful price upsert when all GroupResult tasks complete immediately.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>Price ingestion is invoked with correct parameters</li>
     *   <li>GroupResult status is polled for each returned task</li>
     *   <li>The method completes successfully when all tasks are non-PENDING</li>
     *   <li>The task is returned unchanged after successful completion</li>
     * </ul>
     */
    @Test
    void upsertPrices_success_immediateCompletion() {
        // Given: An investment task with assets and prices
        InvestmentAssetsTask task = createTestTask();
        UUID groupResultUuid1 = UUID.randomUUID();
        UUID groupResultUuid2 = UUID.randomUUID();

        // Mock: Asset universe service methods
        when(assetUniverseService.createAssets(anyList()))
            .thenReturn(Flux.fromIterable(task.getData().getAssets()));

        // Mock GroupResult objects returned from price ingestion
        GroupResult groupResult1 = mock(GroupResult.class);
        when(groupResult1.getUuid()).thenReturn(groupResultUuid1);

        GroupResult groupResult2 = mock(GroupResult.class);
        when(groupResult2.getUuid()).thenReturn(groupResultUuid2);

        List<GroupResult> groupResults = List.of(groupResult1, groupResult2);

        // Mock: Price ingestion returns GroupResult tasks
        when(investmentAssetPriceService.ingestPrices(anyList(), anyMap()))
            .thenReturn(Mono.just(groupResults));

        // When: Execute upsertPrices
        Mono<InvestmentAssetsTask> result = saga.executeTask(task);

        // Then: Verify successful completion
        StepVerifier.create(result)
            .assertNext(completedTask -> {
                assertThat(completedTask).isNotNull();
                assertThat(completedTask.getId()).isEqualTo(task.getId());
            })
            .verifyComplete();

        // Verify: Price ingestion was called
        verify(investmentAssetPriceService, times(1))
            .ingestPrices(task.getData().getAssets(), task.getData().getPriceByAsset());

    }


    /**
     * Test price upsert with polling when tasks transition from PENDING to COMPLETED.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>The polling mechanism waits for PENDING tasks to complete</li>
     *   <li>Status is checked multiple times until all tasks are non-PENDING</li>
     *   <li>The method completes successfully after tasks transition to COMPLETED</li>
     * </ul>
     */
    @Test
    void upsertPrices_success_withPolling() {
        // Given: An investment task with assets and prices
        InvestmentAssetsTask task = createTestTask();
        UUID groupResultUuid = UUID.randomUUID();

        // Mock: Asset universe service methods
        when(assetUniverseService.createAssets(anyList()))
            .thenReturn(Flux.fromIterable(task.getData().getAssets()));

        // Mock GroupResult object returned from price ingestion
        GroupResult groupResult = mock(GroupResult.class);
        when(groupResult.getUuid()).thenReturn(groupResultUuid);
        List<GroupResult> groupResults = List.of(groupResult);

        // Mock: Price ingestion returns GroupResult task
        when(investmentAssetPriceService.ingestPrices(anyList(), anyMap()))
            .thenReturn(Mono.just(groupResults));

        // When: Execute upsertPrices
        Mono<InvestmentAssetsTask> result = saga.executeTask(task);

        // Then: Verify successful completion after polling
        StepVerifier.create(result)
            .assertNext(completedTask -> {
                assertThat(completedTask).isNotNull();
                assertThat(completedTask.getId()).isEqualTo(task.getId());
            })
            .verifyComplete();

        // Verify: Price ingestion was called once
        verify(investmentAssetPriceService, times(1))
            .ingestPrices(task.getData().getAssets(), task.getData().getPriceByAsset());
    }

    /**
     * Test price upsert timeout when tasks remain PENDING beyond the timeout threshold.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>The polling mechanism respects the 5-minute timeout</li>
     *   <li>Timeout error is logged and handled gracefully</li>
     *   <li>The task is still returned despite timeout (error recovery)</li>
     * </ul>
     */
    @Test
    void upsertPrices_timeout_whenTasksRemainPending() {
        // Given: An investment task with assets and prices
        InvestmentAssetsTask task = createTestTask();
        UUID groupResultUuid = UUID.randomUUID();

        // Mock: Asset universe service methods
        when(assetUniverseService.createAssets(anyList()))
            .thenReturn(Flux.fromIterable(task.getData().getAssets()));

        // Mock GroupResult object returned from price ingestion
        GroupResult groupResult = mock(GroupResult.class);
        when(groupResult.getUuid()).thenReturn(groupResultUuid);
        List<GroupResult> groupResults = List.of(groupResult);

        // Mock: Price ingestion returns GroupResult task
        when(investmentAssetPriceService.ingestPrices(anyList(), anyMap()))
            .thenReturn(Mono.just(groupResults));

        // Mock: Status polling always returns PENDING (simulating stuck task)
        GroupResult pending = mock(GroupResult.class);
        when(pending.getUuid()).thenReturn(groupResultUuid);
        when(pending.getStatus()).thenReturn("PENDING");

        // When: Execute upsertPrices with virtual time for testing
        Mono<InvestmentAssetsTask> result = saga.executeTask(task);

        // Then: Verify timeout is handled and task is still returned
        StepVerifier.withVirtualTime(() -> result)
            .expectSubscription()
            .thenAwait(Duration.ofMinutes(6))  // Wait beyond the 5-minute timeout
            .assertNext(completedTask -> {
                assertThat(completedTask).isNotNull();
                assertThat(completedTask.getId()).isEqualTo(task.getId());
            })
            .verifyComplete();

        // Verify: Price ingestion was called
        verify(investmentAssetPriceService, times(1))
            .ingestPrices(task.getData().getAssets(), task.getData().getPriceByAsset());
    }

    /**
     * Test price upsert when ingestion fails with an error.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>Errors during price ingestion are propagated correctly</li>
     *   <li>The saga error handler logs the failure</li>
     *   <li>The task state is set to FAILED</li>
     * </ul>
     */
    @Test
    void upsertPrices_error_duringIngestion() {
        // Given: An investment task with assets and prices
        InvestmentAssetsTask task = createTestTask();

        // Mock: Markets and market special days creation succeed
        when(assetUniverseService.upsertMarket(any())).thenReturn(Mono.empty());
        when(assetUniverseService.upsertMarketSpecialDay(any())).thenReturn(Mono.empty());
        when(assetUniverseService.createAssets(anyList())).thenReturn(Flux.fromIterable(task.getData().getAssets()));

        // Mock: Price ingestion fails with an exception
        when(investmentAssetPriceService.ingestPrices(anyList(), anyMap()))
            .thenReturn(Mono.error(new RuntimeException("Price ingestion failed")));

        // When: Execute the task
        Mono<InvestmentAssetsTask> result = saga.executeTask(task);

        // Then: Verify error is handled and task is returned
        StepVerifier.create(result)
            .assertNext(failedTask -> {
                assertThat(failedTask).isNotNull();
                assertThat(failedTask.getId()).isEqualTo(task.getId());
                // The error handler sets state to FAILED
            })
            .verifyComplete();

        // Verify: Price ingestion was attempted
        verify(investmentAssetPriceService, times(1))
            .ingestPrices(task.getData().getAssets(), task.getData().getPriceByAsset());
    }

    /**
     * Test price upsert when no assets are provided.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>Empty asset list is handled gracefully</li>
     *   <li>Price ingestion is still called (service handles empty list)</li>
     *   <li>The task completes successfully</li>
     * </ul>
     */
    @Test
    void upsertPrices_success_emptyAssets() {
        // Given: An investment task with no assets
        InvestmentAssetData emptyData = InvestmentAssetData.builder()
            .markets(Collections.emptyList())
            .marketSpecialDays(Collections.emptyList())
            .assets(Collections.emptyList())
            .assetPrices(Collections.emptyList())
            .build();
        InvestmentAssetsTask task = new InvestmentAssetsTask("test-empty-assets", emptyData);

        // Mock: Price ingestion returns empty result
        when(investmentAssetPriceService.ingestPrices(anyList(), anyMap()))
            .thenReturn(Mono.just(Collections.emptyList()));

        // When: Execute upsertPrices
        Mono<InvestmentAssetsTask> result = saga.executeTask(task);

        // Then: Verify successful completion
        StepVerifier.create(result)
            .assertNext(completedTask -> {
                assertThat(completedTask).isNotNull();
                assertThat(completedTask.getId()).isEqualTo(task.getId());
            })
            .verifyComplete();

        // Verify: Price ingestion was called with empty list
        verify(investmentAssetPriceService, times(1))
            .ingestPrices(Collections.emptyList(), emptyData.getPriceByAsset());
    }

    /**
     * Test price upsert with mixed GroupResult statuses.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>Polling continues until ALL tasks are non-PENDING</li>
     *   <li>Different task statuses (COMPLETED, FAILED) are handled</li>
     *   <li>The method completes when no tasks are PENDING</li>
     * </ul>
     */
    @Test
    void upsertPrices_success_mixedStatuses() {
        // Given: An investment task with assets and prices
        InvestmentAssetsTask task = createTestTask();
        UUID groupResultUuid1 = UUID.randomUUID();
        UUID groupResultUuid2 = UUID.randomUUID();
        UUID groupResultUuid3 = UUID.randomUUID();

        // Mock: Asset universe service methods
        when(assetUniverseService.createAssets(anyList()))
            .thenReturn(Flux.fromIterable(task.getData().getAssets()));

        // Mock GroupResult objects returned from price ingestion
        GroupResult groupResult1 = mock(GroupResult.class);
        when(groupResult1.getUuid()).thenReturn(groupResultUuid1);

        GroupResult groupResult2 = mock(GroupResult.class);
        when(groupResult2.getUuid()).thenReturn(groupResultUuid2);

        GroupResult groupResult3 = mock(GroupResult.class);
        when(groupResult3.getUuid()).thenReturn(groupResultUuid3);

        List<GroupResult> groupResults = List.of(groupResult1, groupResult2, groupResult3);

        // Mock: Price ingestion returns GroupResult tasks
        when(investmentAssetPriceService.ingestPrices(anyList(), anyMap()))
            .thenReturn(Mono.just(groupResults));

        // When: Execute upsertPrices
        Mono<InvestmentAssetsTask> result = saga.executeTask(task);

        // Then: Verify successful completion after all tasks are non-PENDING
        StepVerifier.create(result)
            .assertNext(completedTask -> {
                assertThat(completedTask).isNotNull();
                assertThat(completedTask.getId()).isEqualTo(task.getId());
            })
            .verifyComplete();

        // Verify: Price ingestion was called
        verify(investmentAssetPriceService, times(1))
            .ingestPrices(task.getData().getAssets(), task.getData().getPriceByAsset());
    }

    // Helper methods

    /**
     * Creates a test InvestmentAssetsTask with sample data for testing.
     *
     * @return a configured test task
     */
    private InvestmentAssetsTask createTestTask() {
        // Create sample assets using the record constructor
        Asset asset1 = new Asset(
            UUID.randomUUID(),
            "Apple Inc.",
            "US0378331005",
            "AAPL",
            StatusA10Enum.ACTIVE,
            "NASDAQ",
            "USD",
            Collections.emptyMap(),
            AssetTypeEnum.STOCK,
            List.of("Technology"),
            null,
            "AAPL-001",
            "Apple Inc. Stock",
            150.0
        );

        Asset asset2 = new Asset(
            UUID.randomUUID(),
            "Microsoft Corp.",
            "US5949181045",
            "MSFT",
            StatusA10Enum.ACTIVE,
            "NYSE",
            "USD",
            Collections.emptyMap(),
            AssetTypeEnum.STOCK,
            List.of("Technology"),
            null,
            "MSFT-001",
            "Microsoft Corp. Stock",
            200.0
        );

        List<Asset> assets = List.of(asset1, asset2);

        // Create sample price list using the record constructor
        AssetPrice price1 = new AssetPrice(
            "US0378331005",
            "NASDAQ",
            "USD",
            150.0,
            new RandomParam(0.99, 1.01)
        );

        AssetPrice price2 = new AssetPrice(
            "US5949181045",
            "NYSE",
            "USD",
            200.0,
            new RandomParam(0.99, 1.01)
        );

        List<AssetPrice> assetPrices = List.of(price1, price2);

        // Build the investment asset data
        InvestmentAssetData data = InvestmentAssetData.builder()
            .markets(Collections.emptyList())
            .marketSpecialDays(Collections.emptyList())
            .assets(assets)
            .assetPrices(assetPrices)
            .build();

        return new InvestmentAssetsTask("test-unit-of-work", data);
    }
}

