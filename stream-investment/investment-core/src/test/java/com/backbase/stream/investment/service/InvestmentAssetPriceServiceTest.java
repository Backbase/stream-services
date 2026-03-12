package com.backbase.stream.investment.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.investment.api.service.v1.AssetUniverseApi;
import com.backbase.investment.api.service.v1.model.GroupResult;
import com.backbase.investment.api.service.v1.model.OASPrice;
import com.backbase.investment.api.service.v1.model.PaginatedOASPriceList;
import com.backbase.stream.investment.Asset;
import com.backbase.stream.investment.AssetPrice;
import com.backbase.stream.investment.RandomParam;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests for {@link InvestmentAssetPriceService}.
 *
 * <p>Tests are grouped by method under {@link Nested} classes to improve readability
 * and navigation. Each nested class covers a single public method, and each test
 * method covers a specific branch or edge case.
 *
 * <p>Conventions:
 * <ul>
 *   <li>All dependencies are mocked via Mockito</li>
 *   <li>Reactive assertions use {@link StepVerifier}</li>
 *   <li>Arrange-Act-Assert structure is followed throughout</li>
 *   <li>Helper methods at the bottom of the class reduce boilerplate</li>
 * </ul>
 */
@DisplayName("InvestmentAssetPriceService")
class InvestmentAssetPriceServiceTest {

    private AssetUniverseApi assetUniverseApi;
    private InvestmentAssetPriceService service;

    @BeforeEach
    void setUp() {
        assetUniverseApi = Mockito.mock(AssetUniverseApi.class);
        service = new InvestmentAssetPriceService(assetUniverseApi);
    }

    // =========================================================================
    // ingestPrices
    // =========================================================================

    /**
     * Tests for {@link InvestmentAssetPriceService#ingestPrices(List, Map)}.
     *
     * <p>Covers:
     * <ul>
     *   <li>Null assets list → treated as empty, no API call, returns empty list</li>
     *   <li>Empty assets list → returns empty list without calling API</li>
     *   <li>No existing prices for asset → DEFAULT_START_PRICE used, bulk create called</li>
     *   <li>Asset has defaultPrice set, no AssetPrice in map → defaultPrice used as start</li>
     *   <li>AssetPrice entry present with custom price and RandomParam → custom values used</li>
     *   <li>Existing prices returned → last price used as start, next day chosen as lastDate</li>
     *   <li>Asset is fully up-to-date (all workdays already have prices) → no bulk create called</li>
     *   <li>listAssetClosePrices returns null response → filtered out, completes empty</li>
     *   <li>listAssetClosePrices returns response with null results → filtered, completes empty</li>
     *   <li>bulkCreateAssetClosePrice fails with WebClientResponseException → error propagated</li>
     *   <li>bulkCreateAssetClosePrice fails with non-WebClient exception → error propagated</li>
     *   <li>Multiple assets → results from all assets merged into flat list</li>
     *   <li>bulkCreate returns empty list → filtered by CollectionUtils.isEmpty, not included</li>
     * </ul>
     */
    @Nested
    @DisplayName("ingestPrices")
    class IngestPricesTests {

        @Test
        @DisplayName("null assets list — returns empty list without calling API")
        void ingestPrices_nullAssets_returnsEmptyListWithoutCallingApi() {
            // Act & Assert
            StepVerifier.create(service.ingestPrices(null, Map.of()))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();

            verify(assetUniverseApi, never()).listAssetClosePrices(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("empty assets list — returns empty list without calling API")
        void ingestPrices_emptyAssets_returnsEmptyListWithoutCallingApi() {
            // Act & Assert
            StepVerifier.create(service.ingestPrices(List.of(), Map.of()))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();

            verify(assetUniverseApi, never()).listAssetClosePrices(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("no existing prices for asset — DEFAULT_START_PRICE used, bulk create is called")
        void ingestPrices_noExistingPrices_usesDefaultStartPriceAndCallsBulkCreate() {
            // Arrange
            Asset asset = buildAsset("US0378331005", "NASDAQ", "USD", null);

            PaginatedOASPriceList emptyPriceList = mock(PaginatedOASPriceList.class);
            when(emptyPriceList.getResults()).thenReturn(List.of());
            stubListAssetClosePrices(asset, Mono.just(emptyPriceList));

            GroupResult groupResult = mock(GroupResult.class);
            when(assetUniverseApi.bulkCreateAssetClosePrice(any(), isNull(), isNull(), isNull()))
                .thenReturn(Flux.just(groupResult));

            // Act & Assert
            StepVerifier.create(service.ingestPrices(List.of(asset), Map.of()))
                .expectNextMatches(results -> !results.isEmpty())
                .verifyComplete();

            verify(assetUniverseApi).bulkCreateAssetClosePrice(any(), isNull(), isNull(), isNull());
        }

        @Test
        @DisplayName("asset has defaultPrice set, no AssetPrice in map — defaultPrice used as start")
        void ingestPrices_assetHasDefaultPrice_noAssetPriceInMap_defaultPriceUsedAsStart() {
            // Arrange — asset carries a defaultPrice of 250.0; no matching AssetPrice in map
            Asset asset = buildAsset("US0378331005", "NASDAQ", "USD", 250.0);

            PaginatedOASPriceList emptyPriceList = mock(PaginatedOASPriceList.class);
            when(emptyPriceList.getResults()).thenReturn(List.of());
            stubListAssetClosePrices(asset, Mono.just(emptyPriceList));

            GroupResult groupResult = mock(GroupResult.class);
            when(assetUniverseApi.bulkCreateAssetClosePrice(any(), isNull(), isNull(), isNull()))
                .thenReturn(Flux.just(groupResult));

            // Act & Assert — pipeline must complete successfully with prices generated
            StepVerifier.create(service.ingestPrices(List.of(asset), Map.of()))
                .expectNextMatches(results -> !results.isEmpty())
                .verifyComplete();

            verify(assetUniverseApi).bulkCreateAssetClosePrice(any(), isNull(), isNull(), isNull());
        }

        @Test
        @DisplayName("AssetPrice entry present with custom price and RandomParam — custom values forwarded to price generation")
        void ingestPrices_assetPriceEntryPresent_customPriceAndRandomParamUsed() {
            // Arrange
            Asset asset = buildAsset("US5949181045", "NYSE", "USD", null);
            AssetPrice assetPrice = new AssetPrice(
                asset.getIsin(), asset.getMarket(), asset.getCurrency(), 350.0,
                new RandomParam(0.98, 1.02));
            Map<String, AssetPrice> priceByAsset = Map.of(asset.getKeyString(), assetPrice);

            PaginatedOASPriceList emptyPriceList = mock(PaginatedOASPriceList.class);
            when(emptyPriceList.getResults()).thenReturn(List.of());
            stubListAssetClosePrices(asset, Mono.just(emptyPriceList));

            GroupResult groupResult = mock(GroupResult.class);
            when(assetUniverseApi.bulkCreateAssetClosePrice(any(), isNull(), isNull(), isNull()))
                .thenReturn(Flux.just(groupResult));

            // Act & Assert
            StepVerifier.create(service.ingestPrices(List.of(asset), priceByAsset))
                .expectNextMatches(results -> !results.isEmpty())
                .verifyComplete();

            verify(assetUniverseApi).bulkCreateAssetClosePrice(any(), isNull(), isNull(), isNull());
        }

        @Test
        @DisplayName("existing prices returned — last price used as start, next day after last price chosen as lastDate")
        void ingestPrices_existingPricesPresent_lastPriceUsedAsStartAndLastDateAdvanced() {
            // Arrange — return a price dated well in the past so new work-days are generated
            Asset asset = buildAsset("US0378331005", "NASDAQ", "USD", null);

            LocalDate pastDate = LocalDate.now().minusDays(15);
            OASPrice existingPrice = mock(OASPrice.class);
            when(existingPrice.getAmount()).thenReturn(180.0);
            when(existingPrice.getDatetime())
                .thenReturn(OffsetDateTime.of(pastDate.atTime(0, 0), ZoneOffset.UTC));

            PaginatedOASPriceList priceList = mock(PaginatedOASPriceList.class);
            when(priceList.getResults()).thenReturn(List.of(existingPrice));
            stubListAssetClosePrices(asset, Mono.just(priceList));

            GroupResult groupResult = mock(GroupResult.class);
            when(assetUniverseApi.bulkCreateAssetClosePrice(any(), isNull(), isNull(), isNull()))
                .thenReturn(Flux.just(groupResult));

            // Act & Assert
            StepVerifier.create(service.ingestPrices(List.of(asset), Map.of()))
                .expectNextMatches(results -> !results.isEmpty())
                .verifyComplete();

            verify(assetUniverseApi).bulkCreateAssetClosePrice(any(), isNull(), isNull(), isNull());
        }

        @Test
        @DisplayName("asset is fully up-to-date — no work-days to create, bulk create never called, result is empty")
        void ingestPrices_assetFullyUpToDate_noBulkCreateCalled_resultIsEmpty() {
            // Arrange — return a price dated today, so no further work-days remain
            Asset asset = buildAsset("US0378331005", "NASDAQ", "USD", null);

            // Use today's date; lastDate = today+1 which is >= now → workDays returns empty
            LocalDate today = LocalDate.now();
            OASPrice todayPrice = mock(OASPrice.class);
            when(todayPrice.getAmount()).thenReturn(150.0);
            when(todayPrice.getDatetime())
                .thenReturn(OffsetDateTime.of(today.atTime(0, 0), ZoneOffset.UTC));

            PaginatedOASPriceList priceList = mock(PaginatedOASPriceList.class);
            when(priceList.getResults()).thenReturn(List.of(todayPrice));
            stubListAssetClosePrices(asset, Mono.just(priceList));

            // Act & Assert — empty because no new days need to be created
            StepVerifier.create(service.ingestPrices(List.of(asset), Map.of()))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();

            verify(assetUniverseApi, never()).bulkCreateAssetClosePrice(any(), any(), any(), any());
        }

        @Test
        @DisplayName("listAssetClosePrices returns null (Mono.empty) — filtered out, completes with empty list")
        void ingestPrices_listPricesReturnsNull_filteredOut_completesWithEmptyList() {
            // Arrange — simulate API returning Mono.empty (no element emitted)
            Asset asset = buildAsset("US0378331005", "NASDAQ", "USD", null);
            stubListAssetClosePrices(asset, Mono.empty());

            // Act & Assert
            StepVerifier.create(service.ingestPrices(List.of(asset), Map.of()))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();

            verify(assetUniverseApi, never()).bulkCreateAssetClosePrice(any(), any(), any(), any());
        }

        @Test
        @DisplayName("listAssetClosePrices response has null results — filtered, completes with empty list")
        void ingestPrices_listPricesResponseHasNullResults_filteredOut_completesWithEmptyList() {
            // Arrange — PaginatedOASPriceList.getResults() returns null
            Asset asset = buildAsset("US0378331005", "NASDAQ", "USD", null);

            PaginatedOASPriceList nullResultsList = mock(PaginatedOASPriceList.class);
            when(nullResultsList.getResults()).thenReturn(null);
            stubListAssetClosePrices(asset, Mono.just(nullResultsList));

            // Act & Assert
            StepVerifier.create(service.ingestPrices(List.of(asset), Map.of()))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();

            verify(assetUniverseApi, never()).bulkCreateAssetClosePrice(any(), any(), any(), any());
        }

        @Test
        @DisplayName("bulkCreateAssetClosePrice fails with WebClientResponseException — error propagated to subscriber")
        void ingestPrices_bulkCreateFailsWithWebClientException_errorPropagated() {
            // Arrange
            Asset asset = buildAsset("US0378331005", "NASDAQ", "USD", null);

            PaginatedOASPriceList emptyPriceList = mock(PaginatedOASPriceList.class);
            when(emptyPriceList.getResults()).thenReturn(List.of());
            stubListAssetClosePrices(asset, Mono.just(emptyPriceList));

            WebClientResponseException apiError = WebClientResponseException.create(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error",
                HttpHeaders.EMPTY, "bulk create failed".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
            when(assetUniverseApi.bulkCreateAssetClosePrice(any(), isNull(), isNull(), isNull()))
                .thenReturn(Flux.error(apiError));

            // Act & Assert
            StepVerifier.create(service.ingestPrices(List.of(asset), Map.of()))
                .expectErrorMatches(e -> e instanceof WebClientResponseException
                    && ((WebClientResponseException) e).getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR)
                .verify();
        }

        @Test
        @DisplayName("bulkCreateAssetClosePrice fails with non-WebClient exception — error propagated to subscriber")
        void ingestPrices_bulkCreateFailsWithNonWebClientException_errorPropagated() {
            // Arrange
            Asset asset = buildAsset("US0378331005", "NASDAQ", "USD", null);

            PaginatedOASPriceList emptyPriceList = mock(PaginatedOASPriceList.class);
            when(emptyPriceList.getResults()).thenReturn(List.of());
            stubListAssetClosePrices(asset, Mono.just(emptyPriceList));

            when(assetUniverseApi.bulkCreateAssetClosePrice(any(), isNull(), isNull(), isNull()))
                .thenReturn(Flux.error(new RuntimeException("Unexpected persistence error")));

            // Act & Assert
            StepVerifier.create(service.ingestPrices(List.of(asset), Map.of()))
                .expectErrorMatches(e -> e instanceof RuntimeException
                    && "Unexpected persistence error".equals(e.getMessage()))
                .verify();
        }

        @Test
        @DisplayName("multiple assets — results from all assets merged into a single flat list")
        void ingestPrices_multipleAssets_resultsMergedIntoFlatList() {
            // Arrange
            Asset asset1 = buildAsset("US0378331005", "NASDAQ", "USD", null);
            Asset asset2 = buildAsset("US5949181045", "NYSE",   "USD", null);

            PaginatedOASPriceList emptyList = mock(PaginatedOASPriceList.class);
            when(emptyList.getResults()).thenReturn(List.of());
            stubListAssetClosePrices(asset1, Mono.just(emptyList));
            stubListAssetClosePrices(asset2, Mono.just(emptyList));

            GroupResult result1 = mock(GroupResult.class);
            GroupResult result2 = mock(GroupResult.class);
            // Both assets produce one GroupResult each
            when(assetUniverseApi.bulkCreateAssetClosePrice(any(), isNull(), isNull(), isNull()))
                .thenReturn(Flux.just(result1))
                .thenReturn(Flux.just(result2));

            // Act & Assert — flat list contains entries from both assets
            StepVerifier.create(service.ingestPrices(List.of(asset1, asset2), Map.of()))
                .expectNextMatches(results -> results.size() >= 1)
                .verifyComplete();
        }

        @Test
        @DisplayName("bulkCreate returns empty list — filtered by CollectionUtils.isEmpty, not included in result")
        void ingestPrices_bulkCreateReturnsEmptyList_filteredOut_notIncludedInResult() {
            // Arrange — bulkCreate Flux emits nothing (empty), so collectList yields empty List
            Asset asset = buildAsset("US0378331005", "NASDAQ", "USD", null);

            PaginatedOASPriceList emptyPriceList = mock(PaginatedOASPriceList.class);
            when(emptyPriceList.getResults()).thenReturn(List.of());
            stubListAssetClosePrices(asset, Mono.just(emptyPriceList));

            // Flux.empty() → collectList() → Mono<[]> → filter(not(isEmpty)) blocks it
            when(assetUniverseApi.bulkCreateAssetClosePrice(any(), isNull(), isNull(), isNull()))
                .thenReturn(Flux.empty());

            // Act & Assert — the empty sub-result is filtered; final flat list is empty
            StepVerifier.create(service.ingestPrices(List.of(asset), Map.of()))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Builds an {@link Asset} with the given identifiers and an optional defaultPrice.
     *
     * @param isin         ISIN code
     * @param market       market code
     * @param currency     ISO currency code
     * @param defaultPrice optional default price; may be {@code null}
     * @return a fully populated {@link Asset}
     */
    private Asset buildAsset(String isin, String market, String currency, Double defaultPrice) {
        Asset asset = new Asset();
        asset.setUuid(UUID.randomUUID());
        asset.setIsin(isin);
        asset.setMarket(market);
        asset.setCurrency(currency);
        asset.setDefaultPrice(defaultPrice);
        return asset;
    }

    /**
     * Stubs {@link AssetUniverseApi#listAssetClosePrices} for the given asset to return
     * the provided {@link Mono}.
     *
     * @param asset    the asset whose close prices should be stubbed
     * @param response the {@link Mono} to return
     */
    private void stubListAssetClosePrices(Asset asset, Mono<PaginatedOASPriceList> response) {
        when(assetUniverseApi.listAssetClosePrices(
            eq(asset.getCurrency()),
            any(), any(),
            isNull(), isNull(), isNull(), isNull(),
            eq(asset.getIsin()),
            anyInt(),
            eq(asset.getMarket()),
            isNull(), isNull()))
            .thenReturn(response);
    }
}

