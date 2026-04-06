package com.backbase.stream.investment.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.investment.api.service.v1.AllocationsApi;
import com.backbase.investment.api.service.v1.AssetUniverseApi;
import com.backbase.investment.api.service.v1.InvestmentApi;
import com.backbase.investment.api.service.v1.model.AssetModelPortfolio;
import com.backbase.investment.api.service.v1.model.Deposit;
import com.backbase.investment.api.service.v1.model.InvestorModelPortfolio;
import com.backbase.investment.api.service.v1.model.OASAllocationCreateRequest;
import com.backbase.investment.api.service.v1.model.OASAllocationPosition;
import com.backbase.investment.api.service.v1.model.OASOrder;
import com.backbase.investment.api.service.v1.model.OASPortfolioAllocation;
import com.backbase.investment.api.service.v1.model.OASPrice;
import com.backbase.investment.api.service.v1.model.PaginatedOASOrderList;
import com.backbase.investment.api.service.v1.model.PaginatedOASPortfolioAllocationList;
import com.backbase.investment.api.service.v1.model.PaginatedOASPriceList;
import com.backbase.investment.api.service.v1.model.PortfolioList;
import com.backbase.investment.api.service.v1.model.PortfolioProduct;
import com.backbase.investment.api.service.v1.model.RelatedAssetSerializerWithAssetCategories;
import com.backbase.stream.configuration.IngestConfigProperties;
import com.backbase.stream.investment.Allocation;
import com.backbase.stream.investment.Asset;
import com.backbase.stream.investment.InvestmentAssetData;
import com.backbase.stream.investment.ModelAsset;
import com.backbase.stream.investment.model.InvestmentPortfolio;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests for {@link InvestmentPortfolioAllocationService}.
 *
 * <p>Tests are grouped by method under {@link Nested} classes. Each nested class covers a single
 * public method, and each test covers a specific branch or edge case.
 *
 * <p>Conventions:
 * <ul>
 *   <li>All dependencies are mocked via Mockito</li>
 *   <li>Reactive assertions use {@link StepVerifier}</li>
 *   <li>Arrange-Act-Assert structure is followed throughout</li>
 *   <li>Helper methods at the bottom reduce boilerplate</li>
 * </ul>
 */
@SuppressWarnings("removal")
@DisplayName("InvestmentPortfolioAllocationService")
class InvestmentPortfolioAllocationServiceTest {

    private AllocationsApi allocationsApi;
    private AssetUniverseApi assetUniverseApi;
    private InvestmentApi investmentApi;
    private CustomIntegrationApiService customIntegrationApiService;
    private InvestmentPortfolioAllocationService service;
    private IngestConfigProperties ingestProperties;

    @BeforeEach
    void setUp() {
        allocationsApi = mock(AllocationsApi.class);
        assetUniverseApi = mock(AssetUniverseApi.class);
        investmentApi = mock(InvestmentApi.class);
        customIntegrationApiService = mock(CustomIntegrationApiService.class);
        ingestProperties = new IngestConfigProperties();
        service = new InvestmentPortfolioAllocationService(
            allocationsApi, assetUniverseApi, investmentApi, customIntegrationApiService, ingestProperties);
    }

    // =========================================================================
    // removeAllocations
    // =========================================================================

    /**
     * Tests for {@link InvestmentPortfolioAllocationService#removeAllocations(PortfolioList)}.
     *
     * <p>Covers:
     * <ul>
     *   <li>Existing allocations found → each allocation deleted by valuation date</li>
     *   <li>No existing allocations → delete never called</li>
     *   <li>API error listing allocations → error propagated</li>
     * </ul>
     */
    @Nested
    @DisplayName("removeAllocations")
    class RemoveAllocationsTests {

        @Test
        @DisplayName("existing allocations found — deletes each allocation and completes")
        void removeAllocations_existingAllocations_deletesEachAndCompletes() {
            // Arrange
            UUID portfolioUuid = UUID.randomUUID();
            PortfolioList portfolio = buildPortfolioList(portfolioUuid);

            LocalDate date1 = LocalDate.now().minusDays(2);
            LocalDate date2 = LocalDate.now().minusDays(1);

            OASPortfolioAllocation alloc1 = mock(OASPortfolioAllocation.class);
            when(alloc1.getValuationDate()).thenReturn(date1);
            OASPortfolioAllocation alloc2 = mock(OASPortfolioAllocation.class);
            when(alloc2.getValuationDate()).thenReturn(date2);

            PaginatedOASPortfolioAllocationList page = mock(PaginatedOASPortfolioAllocationList.class);
            when(page.getResults()).thenReturn(List.of(alloc1, alloc2));
            when(allocationsApi.listPortfolioAllocations(
                eq(portfolioUuid.toString()), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(page));
            when(allocationsApi.deletePortfolioAllocation(portfolioUuid.toString(), date1))
                .thenReturn(Mono.empty());
            when(allocationsApi.deletePortfolioAllocation(portfolioUuid.toString(), date2))
                .thenReturn(Mono.empty());

            // Act & Assert
            StepVerifier.create(service.removeAllocations(portfolio))
                .verifyComplete();

            verify(allocationsApi).deletePortfolioAllocation(portfolioUuid.toString(), date1);
            verify(allocationsApi).deletePortfolioAllocation(portfolioUuid.toString(), date2);
        }

        @Test
        @DisplayName("no existing allocations — delete is never called")
        void removeAllocations_noAllocations_deleteNeverCalled() {
            // Arrange
            UUID portfolioUuid = UUID.randomUUID();
            PortfolioList portfolio = buildPortfolioList(portfolioUuid);

            PaginatedOASPortfolioAllocationList emptyPage = mock(PaginatedOASPortfolioAllocationList.class);
            when(emptyPage.getResults()).thenReturn(List.of());
            when(allocationsApi.listPortfolioAllocations(
                eq(portfolioUuid.toString()), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(emptyPage));

            // Act & Assert
            StepVerifier.create(service.removeAllocations(portfolio))
                .verifyComplete();

            verify(allocationsApi, never()).deletePortfolioAllocation(any(), any());
        }

        @Test
        @DisplayName("API error listing allocations — error propagated to caller")
        void removeAllocations_apiErrorListing_propagatesError() {
            // Arrange
            UUID portfolioUuid = UUID.randomUUID();
            PortfolioList portfolio = buildPortfolioList(portfolioUuid);

            when(allocationsApi.listPortfolioAllocations(
                eq(portfolioUuid.toString()), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.error(new RuntimeException("API unavailable")));

            // Act & Assert
            StepVerifier.create(service.removeAllocations(portfolio))
                .expectErrorMatches(e -> e instanceof RuntimeException
                    && "API unavailable".equals(e.getMessage()))
                .verify();
        }
    }

    // =========================================================================
    // createDepositAllocation
    // =========================================================================

    /**
     * Tests for {@link InvestmentPortfolioAllocationService#createDepositAllocation(Deposit)}.
     *
     * <p>The service filter keeps the allocation list when at least one allocation has empty
     * positions. If filtered out (all non-empty), {@code switchIfEmpty} fires and a new
     * cash-active allocation is created.
     *
     * <p>Covers:
     * <ul>
     *   <li>At least one allocation has empty positions → filter passes, deposit returned</li>
     *   <li>All allocations have non-empty positions → filter fails, new allocation created</li>
     *   <li>No allocations returned → switchIfEmpty triggers, new allocation created</li>
     *   <li>createPortfolioAllocation fails → error swallowed, Mono completes empty</li>
     *   <li>listPortfolioAllocations fails → error swallowed, Mono completes empty</li>
     *   <li>deposit completedAt is null → today used as valuation date</li>
     * </ul>
     */
    @Nested
    @DisplayName("createDepositAllocation")
    class CreateDepositAllocationTests {

        @Test
        @DisplayName("allocation with empty positions found — filter passes, returns deposit without upsert")
        void createDepositAllocation_emptyPositionsFound_returnsDepositWithoutUpsert() {
            // Arrange
            UUID portfolioUuid = UUID.randomUUID();
            LocalDate completedAt = LocalDate.now().minusDays(1);
            Deposit deposit = buildDeposit(portfolioUuid, completedAt, 5_000d);

            OASPortfolioAllocation allocWithNoPositions = mock(OASPortfolioAllocation.class);
            doReturn(List.of()).when(allocWithNoPositions).getPositions();

            PaginatedOASPortfolioAllocationList page = mock(PaginatedOASPortfolioAllocationList.class);
            when(page.getResults()).thenReturn(List.of(allocWithNoPositions));
            when(allocationsApi.listPortfolioAllocations(
                eq(portfolioUuid.toString()), isNull(), isNull(), eq(10), isNull(), isNull(),
                eq(completedAt.minusDays(4)), eq(completedAt.plusDays(5))))
                .thenReturn(Mono.just(page));

            // Act & Assert
            StepVerifier.create(service.createDepositAllocation(deposit))
                .expectNextMatches(d -> d == deposit)
                .verifyComplete();

            verify(customIntegrationApiService, never())
                .createPortfolioAllocation(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("all allocations have non-empty positions — filter fails, creates cash-active allocation")
        void createDepositAllocation_allPositionsNonEmpty_createsCashAllocationAndReturnsDeposit() {
            // Arrange
            UUID portfolioUuid = UUID.randomUUID();
            LocalDate completedAt = LocalDate.now().minusDays(1);
            Deposit deposit = buildDeposit(portfolioUuid, completedAt, 5_000d);

            OASPortfolioAllocation allocWithPositions = mock(OASPortfolioAllocation.class);
            doReturn(List.of(mock(OASAllocationPosition.class))).when(allocWithPositions).getPositions();

            PaginatedOASPortfolioAllocationList page = mock(PaginatedOASPortfolioAllocationList.class);
            when(page.getResults()).thenReturn(List.of(allocWithPositions));
            when(allocationsApi.listPortfolioAllocations(
                eq(portfolioUuid.toString()), isNull(), isNull(), eq(10), isNull(), isNull(),
                eq(completedAt.minusDays(4)), eq(completedAt.plusDays(5))))
                .thenReturn(Mono.just(page));

            OASPortfolioAllocation created = mock(OASPortfolioAllocation.class);
            when(customIntegrationApiService.createPortfolioAllocation(
                eq(portfolioUuid.toString()), any(OASAllocationCreateRequest.class), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(created));

            // Act & Assert
            StepVerifier.create(service.createDepositAllocation(deposit))
                .expectNextMatches(d -> d == deposit)
                .verifyComplete();

            verify(customIntegrationApiService)
                .createPortfolioAllocation(eq(portfolioUuid.toString()),
                    any(OASAllocationCreateRequest.class), isNull(), isNull(), isNull());
        }

        @Test
        @DisplayName("no allocations returned — switchIfEmpty triggers, creates cash-active allocation")
        void createDepositAllocation_noAllocations_createsCashAllocationAndReturnsDeposit() {
            // Arrange
            UUID portfolioUuid = UUID.randomUUID();
            LocalDate completedAt = LocalDate.now().minusDays(1);
            Deposit deposit = buildDeposit(portfolioUuid, completedAt, 3_000d);

            PaginatedOASPortfolioAllocationList emptyPage = mock(PaginatedOASPortfolioAllocationList.class);
            when(emptyPage.getResults()).thenReturn(List.of());
            when(allocationsApi.listPortfolioAllocations(
                eq(portfolioUuid.toString()), isNull(), isNull(), eq(10), isNull(), isNull(),
                eq(completedAt.minusDays(4)), eq(completedAt.plusDays(5))))
                .thenReturn(Mono.just(emptyPage));

            OASPortfolioAllocation created = mock(OASPortfolioAllocation.class);
            when(customIntegrationApiService.createPortfolioAllocation(
                eq(portfolioUuid.toString()), any(OASAllocationCreateRequest.class), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(created));

            // Act & Assert
            StepVerifier.create(service.createDepositAllocation(deposit))
                .expectNextMatches(d -> d == deposit)
                .verifyComplete();

            verify(customIntegrationApiService)
                .createPortfolioAllocation(eq(portfolioUuid.toString()),
                    any(OASAllocationCreateRequest.class), isNull(), isNull(), isNull());
        }

        @Test
        @DisplayName("createPortfolioAllocation fails — onErrorResume swallows error, Mono completes empty")
        void createDepositAllocation_upsertFails_errorSwallowed_returnsEmpty() {
            // Arrange
            UUID portfolioUuid = UUID.randomUUID();
            LocalDate completedAt = LocalDate.now().minusDays(1);
            Deposit deposit = buildDeposit(portfolioUuid, completedAt, 5_000d);

            OASPortfolioAllocation allocWithPositions = mock(OASPortfolioAllocation.class);
            doReturn(List.of(mock(OASAllocationPosition.class))).when(allocWithPositions).getPositions();

            PaginatedOASPortfolioAllocationList page = mock(PaginatedOASPortfolioAllocationList.class);
            when(page.getResults()).thenReturn(List.of(allocWithPositions));
            when(allocationsApi.listPortfolioAllocations(
                eq(portfolioUuid.toString()), isNull(), isNull(), eq(10), isNull(), isNull(),
                eq(completedAt.minusDays(4)), eq(completedAt.plusDays(5))))
                .thenReturn(Mono.just(page));

            when(customIntegrationApiService.createPortfolioAllocation(
                eq(portfolioUuid.toString()), any(OASAllocationCreateRequest.class), isNull(), isNull(), isNull()))
                .thenReturn(Mono.error(new RuntimeException("downstream failure")));

            // Act & Assert
            StepVerifier.create(service.createDepositAllocation(deposit))
                .verifyComplete();
        }

        @Test
        @DisplayName("listPortfolioAllocations fails — top-level onErrorResume swallows, Mono completes empty")
        void createDepositAllocation_listFails_errorSwallowed_returnsEmpty() {
            // Arrange
            UUID portfolioUuid = UUID.randomUUID();
            LocalDate completedAt = LocalDate.now().minusDays(1);
            Deposit deposit = buildDeposit(portfolioUuid, completedAt, 5_000d);

            when(allocationsApi.listPortfolioAllocations(
                eq(portfolioUuid.toString()), isNull(), isNull(), eq(10), isNull(), isNull(),
                eq(completedAt.minusDays(4)), eq(completedAt.plusDays(5))))
                .thenReturn(Mono.error(new RuntimeException("API unavailable")));

            // Act & Assert
            StepVerifier.create(service.createDepositAllocation(deposit))
                .verifyComplete();

            verify(customIntegrationApiService, never())
                .createPortfolioAllocation(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("deposit completedAt is null — uses today as valuation date")
        void createDepositAllocation_nullCompletedAt_usesTodayAsValuationDate() {
            // Arrange
            UUID portfolioUuid = UUID.randomUUID();
            LocalDate today = LocalDate.now();

            Deposit deposit = mock(Deposit.class);
            when(deposit.getPortfolio()).thenReturn(portfolioUuid);
            when(deposit.getCompletedAt()).thenReturn(null);
            when(deposit.getAmount()).thenReturn(3_000d);

            OASPortfolioAllocation allocWithPositions = mock(OASPortfolioAllocation.class);
            doReturn(List.of(mock(OASAllocationPosition.class))).when(allocWithPositions).getPositions();

            PaginatedOASPortfolioAllocationList page = mock(PaginatedOASPortfolioAllocationList.class);
            when(page.getResults()).thenReturn(List.of(allocWithPositions));
            when(allocationsApi.listPortfolioAllocations(
                eq(portfolioUuid.toString()), isNull(), isNull(), eq(10), isNull(), isNull(),
                eq(today.minusDays(4)), eq(today.plusDays(5))))
                .thenReturn(Mono.just(page));

            OASPortfolioAllocation created = mock(OASPortfolioAllocation.class);
            when(customIntegrationApiService.createPortfolioAllocation(
                eq(portfolioUuid.toString()), any(OASAllocationCreateRequest.class), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(created));

            // Act & Assert
            StepVerifier.create(service.createDepositAllocation(deposit))
                .expectNextMatches(d -> d == deposit)
                .verifyComplete();
        }
    }

    // =========================================================================
    // generateAllocations
    // =========================================================================

    /**
     * Tests for
     * {@link InvestmentPortfolioAllocationService#generateAllocations(InvestmentPortfolio, List, InvestmentAssetData)}.
     *
     * <p>Covers:
     * <ul>
     *   <li>Any pipeline error → onErrorResume swallows and returns empty</li>
     *   <li>No existing allocations, prices available → creates allocations via orderPositions</li>
     *   <li>Existing allocations with valuation date = today → no pending days, returns empty list</li>
     *   <li>No matching portfolio product → falls back to default model from asset universe</li>
     * </ul>
     */
    @Nested
    @DisplayName("generateAllocations")
    class GenerateAllocationsTests {

        @Test
        @DisplayName("any error in allocation pipeline — onErrorResume swallows and returns empty Mono")
        void generateAllocations_pipelineError_returnsEmptyMono() {
            // Arrange
            UUID portfolioUuid = UUID.randomUUID();
            PortfolioList portfolio = buildPortfolioList(portfolioUuid);
            InvestmentPortfolio investmentPortfolio = InvestmentPortfolio.builder().portfolio(portfolio).build();
            when(portfolio.getProduct()).thenReturn(UUID.randomUUID());
            when(portfolio.getActivated()).thenReturn(OffsetDateTime.now().minusMonths(6));

            PortfolioProduct nonMatchingProduct = mock(PortfolioProduct.class);
            when(nonMatchingProduct.getUuid()).thenReturn(UUID.randomUUID());

            when(assetUniverseApi.listAssetClosePrices(any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any()))
                .thenReturn(Mono.error(new RuntimeException("price API unavailable")));

            InvestmentAssetData assetData = InvestmentAssetData.builder()
                .assets(List.of(buildAsset("ISIN1", "XNAS", "USD"), buildAsset("ISIN2", "XNAS", "EUR")))
                .build();

            // Act & Assert
            StepVerifier.create(service.generateAllocations(investmentPortfolio, List.of(nonMatchingProduct), assetData))
                .verifyComplete();
        }

        @Test
        @DisplayName("no existing allocations, prices available — creates new allocations via orderPositions")
        void generateAllocations_noExistingAllocations_createsNewAllocations() {
            // Arrange
            UUID portfolioUuid = UUID.randomUUID();
            UUID productUuid = UUID.randomUUID();
            UUID modelUuid = UUID.randomUUID();

            PortfolioList portfolio = buildPortfolioList(portfolioUuid);
            InvestmentPortfolio investmentPortfolio = InvestmentPortfolio.builder().portfolio(portfolio).build();
            when(portfolio.getProduct()).thenReturn(productUuid);
            when(portfolio.getActivated()).thenReturn(OffsetDateTime.now().minusMonths(2));

            PortfolioProduct portfolioProduct = buildPortfolioProductWithModel(
                productUuid, modelUuid,
                List.of(new Allocation(new ModelAsset("ISIN1", "XNAS", "USD"), 0.8)), 0.2);

            // Nearest weekday at or before now()-10, avoiding weekend flakiness.
            LocalDate priceDay = Stream.iterate(LocalDate.now().minusDays(10), d -> d.minusDays(1))
                .filter(d -> d.getDayOfWeek().getValue() <= 5)
                .findFirst().orElseThrow();
            OASPrice price = mock(OASPrice.class);
            when(price.getAmount()).thenReturn(100.0);
            when(price.getDatetime()).thenReturn(priceDay.atTime(0, 0).atOffset(ZoneOffset.UTC));

            PaginatedOASPriceList priceList = mock(PaginatedOASPriceList.class);
            when(priceList.getResults()).thenReturn(List.of(price));
            when(assetUniverseApi.listAssetClosePrices(
                eq("USD"), any(), any(), isNull(), isNull(), isNull(), isNull(),
                eq("ISIN1"), isNull(), eq("XNAS"), isNull(), isNull()))
                .thenReturn(Mono.just(priceList));

            PaginatedOASPortfolioAllocationList emptyAllocPage = mock(PaginatedOASPortfolioAllocationList.class);
            when(emptyAllocPage.getResults()).thenReturn(List.of());
            when(allocationsApi.listPortfolioAllocations(
                eq(portfolioUuid.toString()), isNull(), isNull(), eq(1), isNull(), isNull(), any(), any()))
                .thenReturn(Mono.just(emptyAllocPage));

            PaginatedOASOrderList emptyOrderList = mock(PaginatedOASOrderList.class);
            when(emptyOrderList.getResults()).thenReturn(List.of());
            when(investmentApi.listOrders(isNull(), any(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), eq(portfolioUuid.toString()), isNull()))
                .thenReturn(Mono.just(emptyOrderList));

            when(investmentApi.createOrder(any(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(mock(OASOrder.class)));

            OASPortfolioAllocation createdAlloc = mock(OASPortfolioAllocation.class);
            when(customIntegrationApiService.createPortfolioAllocation(
                eq(portfolioUuid.toString()), any(OASAllocationCreateRequest.class), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(createdAlloc));

            InvestmentAssetData assetData = InvestmentAssetData.builder()
                .assets(List.of(buildAsset("ISIN1", "XNAS", "USD")))
                .build();

            // Act & Assert — one allocation created per work day from priceDay to today
            StepVerifier.create(service.generateAllocations(investmentPortfolio, List.of(portfolioProduct), assetData))
                .expectNextMatches(result -> !result.isEmpty())
                .verifyComplete();

            verify(customIntegrationApiService, atLeastOnce())
                .createPortfolioAllocation(eq(portfolioUuid.toString()), any(), isNull(), isNull(), isNull());
        }

        @Test
        @DisplayName("existing allocations with valuation date = today — no pending days, returns empty list")
        void generateAllocations_lastValuationIsToday_noPendingDays_returnsEmptyList() {
            // Arrange
            UUID portfolioUuid = UUID.randomUUID();
            UUID productUuid = UUID.randomUUID();
            UUID modelUuid = UUID.randomUUID();

            PortfolioList portfolio = buildPortfolioList(portfolioUuid);
            InvestmentPortfolio investmentPortfolio = InvestmentPortfolio.builder().portfolio(portfolio).build();
            when(portfolio.getProduct()).thenReturn(productUuid);
            when(portfolio.getActivated()).thenReturn(OffsetDateTime.now().minusMonths(2));

            PortfolioProduct portfolioProduct = buildPortfolioProductWithModel(
                productUuid, modelUuid,
                List.of(new Allocation(new ModelAsset("ISIN1", "XNAS", "USD"), 0.8)), 0.2);

            PaginatedOASPriceList priceList = mock(PaginatedOASPriceList.class);
            when(priceList.getResults()).thenReturn(List.of());
            when(assetUniverseApi.listAssetClosePrices(
                eq("USD"), any(), any(), isNull(), isNull(), isNull(), isNull(),
                eq("ISIN1"), isNull(), eq("XNAS"), isNull(), isNull()))
                .thenReturn(Mono.just(priceList));

            Asset asset = buildAsset("ISIN1", "XNAS", "USD");

            // Valuation date = today: nextValuationDate = tomorrow, workDays(tomorrow, today) = [] → List.of()
            OASAllocationPosition existingPosition = mock(OASAllocationPosition.class);
            when(existingPosition.getAsset()).thenReturn(asset.getUuid());
            when(existingPosition.getShares()).thenReturn(10.0);
            when(existingPosition.getPrice()).thenReturn(100.0);

            OASPortfolioAllocation existingAlloc = mock(OASPortfolioAllocation.class);
            when(existingAlloc.getValuationDate()).thenReturn(LocalDate.now());
            doReturn(List.of(existingPosition)).when(existingAlloc).getPositions();
            when(existingAlloc.getInvested()).thenReturn(10_000.0);
            when(existingAlloc.getCashActive()).thenReturn(0.0);
            when(existingAlloc.getTradeTotal()).thenReturn(1_000.0);

            PaginatedOASPortfolioAllocationList allocPage = mock(PaginatedOASPortfolioAllocationList.class);
            when(allocPage.getResults()).thenReturn(List.of(existingAlloc));
            when(allocationsApi.listPortfolioAllocations(
                eq(portfolioUuid.toString()), isNull(), isNull(), eq(1), isNull(), isNull(), any(), any()))
                .thenReturn(Mono.just(allocPage));

            InvestmentAssetData assetData = InvestmentAssetData.builder()
                .assets(List.of(asset))
                .build();

            // Act & Assert
            StepVerifier.create(service.generateAllocations(investmentPortfolio, List.of(portfolioProduct), assetData))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();

            verify(customIntegrationApiService, never())
                .createPortfolioAllocation(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("no matching portfolio product — falls back to default model from first two assets")
        void generateAllocations_noMatchingPortfolioProduct_fallsBackToDefaultModel() {
            // Arrange
            UUID portfolioUuid = UUID.randomUUID();

            PortfolioList portfolio = buildPortfolioList(portfolioUuid);
            InvestmentPortfolio investmentPortfolio = InvestmentPortfolio.builder().portfolio(portfolio).build();
            when(portfolio.getProduct()).thenReturn(UUID.randomUUID());
            when(portfolio.getActivated()).thenReturn(OffsetDateTime.now().minusMonths(2));

            PortfolioProduct nonMatchingProduct = mock(PortfolioProduct.class);
            when(nonMatchingProduct.getUuid()).thenReturn(UUID.randomUUID());

            Asset asset1 = buildAsset("ISIN1", "XNAS", "USD");
            Asset asset2 = buildAsset("ISIN2", "XNAS", "EUR");

            // Nearest weekday at or before now()-5, avoiding weekend flakiness.
            LocalDate priceDay = Stream.iterate(LocalDate.now().minusDays(5), d -> d.minusDays(1))
                .filter(d -> d.getDayOfWeek().getValue() <= 5)
                .findFirst().orElseThrow();

            OASPrice price1 = mock(OASPrice.class);
            when(price1.getAmount()).thenReturn(50.0);
            when(price1.getDatetime()).thenReturn(priceDay.atTime(0, 0).atOffset(ZoneOffset.UTC));
            PaginatedOASPriceList priceList1 = mock(PaginatedOASPriceList.class);
            when(priceList1.getResults()).thenReturn(List.of(price1));

            OASPrice price2 = mock(OASPrice.class);
            when(price2.getAmount()).thenReturn(75.0);
            when(price2.getDatetime()).thenReturn(priceDay.atTime(0, 0).atOffset(ZoneOffset.UTC));
            PaginatedOASPriceList priceList2 = mock(PaginatedOASPriceList.class);
            when(priceList2.getResults()).thenReturn(List.of(price2));

            when(assetUniverseApi.listAssetClosePrices(
                eq("USD"), any(), any(), isNull(), isNull(), isNull(), isNull(),
                eq("ISIN1"), isNull(), eq("XNAS"), isNull(), isNull()))
                .thenReturn(Mono.just(priceList1));
            when(assetUniverseApi.listAssetClosePrices(
                eq("EUR"), any(), any(), isNull(), isNull(), isNull(), isNull(),
                eq("ISIN2"), isNull(), eq("XNAS"), isNull(), isNull()))
                .thenReturn(Mono.just(priceList2));

            PaginatedOASPortfolioAllocationList emptyAllocPage = mock(PaginatedOASPortfolioAllocationList.class);
            when(emptyAllocPage.getResults()).thenReturn(List.of());
            when(allocationsApi.listPortfolioAllocations(
                eq(portfolioUuid.toString()), isNull(), isNull(), eq(1), isNull(), isNull(), any(), any()))
                .thenReturn(Mono.just(emptyAllocPage));

            PaginatedOASOrderList emptyOrderList = mock(PaginatedOASOrderList.class);
            when(emptyOrderList.getResults()).thenReturn(List.of());
            when(investmentApi.listOrders(isNull(), any(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), eq(portfolioUuid.toString()), isNull()))
                .thenReturn(Mono.just(emptyOrderList));
            when(investmentApi.createOrder(any(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(mock(OASOrder.class)));

            OASPortfolioAllocation createdAlloc = mock(OASPortfolioAllocation.class);
            when(customIntegrationApiService.createPortfolioAllocation(
                eq(portfolioUuid.toString()), any(OASAllocationCreateRequest.class), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(createdAlloc));

            InvestmentAssetData assetData = InvestmentAssetData.builder()
                .assets(List.of(asset1, asset2))
                .build();

            // Act & Assert
            StepVerifier.create(service.generateAllocations(investmentPortfolio, List.of(nonMatchingProduct), assetData))
                .expectNextMatches(result -> !result.isEmpty())
                .verifyComplete();

            verify(customIntegrationApiService, atLeastOnce())
                .createPortfolioAllocation(eq(portfolioUuid.toString()), any(), isNull(), isNull(), isNull());
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private PortfolioList buildPortfolioList(UUID portfolioUuid) {
        PortfolioList portfolio = mock(PortfolioList.class);
        when(portfolio.getUuid()).thenReturn(portfolioUuid);
        return portfolio;
    }

    private Deposit buildDeposit(UUID portfolioUuid, LocalDate completedAt, double amount) {
        Deposit deposit = mock(Deposit.class);
        when(deposit.getPortfolio()).thenReturn(portfolioUuid);
        when(deposit.getCompletedAt()).thenReturn(completedAt.atTime(0, 0).atOffset(ZoneOffset.UTC));
        when(deposit.getAmount()).thenReturn(amount);
        return deposit;
    }

    private Asset buildAsset(String isin, String market, String currency) {
        Asset asset = new Asset();
        asset.setUuid(UUID.randomUUID());
        asset.setIsin(isin);
        asset.setMarket(market);
        asset.setCurrency(currency);
        return asset;
    }

    private PortfolioProduct buildPortfolioProductWithModel(UUID productUuid, UUID modelUuid,
        List<Allocation> allocations, double cashWeight) {

        List<AssetModelPortfolio> apiAllocations = allocations.stream().map(a -> {
            RelatedAssetSerializerWithAssetCategories apiAsset = new RelatedAssetSerializerWithAssetCategories();
            apiAsset.setIsin(a.asset().getIsin());
            apiAsset.setMarket(a.asset().getMarket());
            apiAsset.setCurrency(a.asset().getCurrency());
            return new AssetModelPortfolio().asset(apiAsset).weight(a.weight());
        }).toList();

        InvestorModelPortfolio investorModel = new InvestorModelPortfolio(
            modelUuid, null, cashWeight, null, apiAllocations, null);

        PortfolioProduct product = mock(PortfolioProduct.class);
        when(product.getUuid()).thenReturn(productUuid);
        when(product.getModelPortfolio()).thenReturn(investorModel);
        return product;
    }
}

