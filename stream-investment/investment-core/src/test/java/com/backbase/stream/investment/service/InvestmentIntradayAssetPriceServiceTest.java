package com.backbase.stream.investment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.investment.api.service.v1.AssetUniverseApi;
import com.backbase.investment.api.service.v1.model.GroupResult;
import com.backbase.investment.api.service.v1.model.OASCreatePriceRequest;
import com.backbase.investment.api.service.v1.model.TypeEnum;
import com.backbase.stream.investment.model.AssetWithMarketAndLatestPrice;
import com.backbase.stream.investment.model.ExpandedLatestPrice;
import com.backbase.stream.investment.model.ExpandedMarket;
import com.backbase.stream.investment.model.PaginatedExpandedAssetList;
import com.backbase.stream.investment.service.InvestmentIntradayAssetPriceService.Ohlc;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests for {@link InvestmentIntradayAssetPriceService}.
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
@DisplayName("InvestmentIntradayAssetPriceService")
class InvestmentIntradayAssetPriceServiceTest {

    private AssetUniverseApi assetUniverseApi;
    private InvestmentIntradayAssetPriceService service;

    @BeforeEach
    void setUp() {
        assetUniverseApi = mock(AssetUniverseApi.class);
        service = new InvestmentIntradayAssetPriceService(assetUniverseApi);
    }

    // =========================================================================
    // generateIntradayOhlc
    // =========================================================================

    /**
     * Tests for {@link InvestmentIntradayAssetPriceService#generateIntradayOhlc(Double)}.
     *
     * <p>Covers:
     * <ul>
     *   <li>null previousClose → IllegalArgumentException thrown</li>
     *   <li>zero previousClose → IllegalArgumentException thrown</li>
     *   <li>negative previousClose → IllegalArgumentException thrown</li>
     *   <li>valid previousClose → all OHLC values are positive and correctly rounded to 6 decimal places</li>
     *   <li>high >= max(open, close) and low <= min(open, close) invariant always holds</li>
     *   <li>open and close are both within [low, high]</li>
     *   <li>very small previousClose (0.000001) → all values remain positive</li>
     *   <li>very large previousClose (1_000_000.0) → values scale proportionally</li>
     *   <li>Ohlc record accessors return the correct component values</li>
     * </ul>
     */
    @Nested
    @DisplayName("generateIntradayOhlc")
    class GenerateIntradayOhlcTests {

        @Test
        @DisplayName("null previousClose — IllegalArgumentException thrown")
        void generateIntradayOhlc_nullPreviousClose_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> InvestmentIntradayAssetPriceService.generateIntradayOhlc(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Previous close must be positive");
        }

        @Test
        @DisplayName("zero previousClose — IllegalArgumentException thrown")
        void generateIntradayOhlc_zeroPreviousClose_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> InvestmentIntradayAssetPriceService.generateIntradayOhlc(0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Previous close must be positive");
        }

        @Test
        @DisplayName("negative previousClose — IllegalArgumentException thrown")
        void generateIntradayOhlc_negativePreviousClose_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> InvestmentIntradayAssetPriceService.generateIntradayOhlc(-1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Previous close must be positive");
        }

        @RepeatedTest(10)
        @DisplayName("valid previousClose — all OHLC values positive and rounded to 6 decimal places")
        void generateIntradayOhlc_validPreviousClose_producesValidOhlcStructure() {
            // Arrange
            double previous = 100.0;

            // Act
            Ohlc ohlc = InvestmentIntradayAssetPriceService.generateIntradayOhlc(previous);

            // Assert — all values positive
            assertThat(ohlc.open()).isGreaterThan(0.0);
            assertThat(ohlc.high()).isGreaterThan(0.0);
            assertThat(ohlc.low()).isGreaterThan(0.0);
            assertThat(ohlc.close()).isGreaterThan(0.0);

            // Assert — high >= max(open,close), low <= min(open,close)
            assertThat(ohlc.high()).isGreaterThanOrEqualTo(Math.max(ohlc.open(), ohlc.close()));
            assertThat(ohlc.low()).isLessThanOrEqualTo(Math.min(ohlc.open(), ohlc.close()));

            // Assert — rounded to 6 decimal places
            assertThat(Math.abs(Math.round(ohlc.open() * 1_000_000.0) - ohlc.open() * 1_000_000.0))
                .isLessThan(1e-6);
            assertThat(Math.abs(Math.round(ohlc.high() * 1_000_000.0) - ohlc.high() * 1_000_000.0))
                .isLessThan(1e-6);
            assertThat(Math.abs(Math.round(ohlc.low() * 1_000_000.0) - ohlc.low() * 1_000_000.0))
                .isLessThan(1e-6);
            assertThat(Math.abs(Math.round(ohlc.close() * 1_000_000.0) - ohlc.close() * 1_000_000.0))
                .isLessThan(1e-6);
        }

        @RepeatedTest(20)
        @DisplayName("high >= low invariant always holds regardless of bullish/bearish direction")
        void generateIntradayOhlc_highAlwaysGreaterThanOrEqualToLow() {
            // Act
            Ohlc ohlc = InvestmentIntradayAssetPriceService.generateIntradayOhlc(50.0);

            // Assert
            assertThat(ohlc.high()).isGreaterThanOrEqualTo(ohlc.low());
        }

        @RepeatedTest(20)
        @DisplayName("open and close are both within [low, high]")
        void generateIntradayOhlc_openAndCloseWithinLowHighRange() {
            // Act
            Ohlc ohlc = InvestmentIntradayAssetPriceService.generateIntradayOhlc(200.0);

            // Assert
            assertThat(ohlc.open()).isBetween(ohlc.low(), ohlc.high());
            assertThat(ohlc.close()).isBetween(ohlc.low(), ohlc.high());
        }

        @RepeatedTest(10)
        @DisplayName("very small previousClose (0.000001) — all OHLC values are positive")
        void generateIntradayOhlc_verySmallPreviousClose_allValuesPositive() {
            // Arrange
            double previous = 0.000001;

            // Act
            Ohlc ohlc = InvestmentIntradayAssetPriceService.generateIntradayOhlc(previous);

            // Assert
            assertThat(ohlc.open()).isGreaterThan(0.0);
            assertThat(ohlc.high()).isGreaterThan(0.0);
            assertThat(ohlc.low()).isGreaterThan(0.0);
            assertThat(ohlc.close()).isGreaterThan(0.0);
        }

        @RepeatedTest(10)
        @DisplayName("very large previousClose (1_000_000.0) — values scale proportionally and remain positive")
        void generateIntradayOhlc_veryLargePreviousClose_valuesScaleAndRemainPositive() {
            // Arrange
            double previous = 1_000_000.0;

            // Act
            Ohlc ohlc = InvestmentIntradayAssetPriceService.generateIntradayOhlc(previous);

            // Assert
            assertThat(ohlc.open()).isGreaterThan(900_000.0);
            assertThat(ohlc.high()).isGreaterThan(0.0);
            assertThat(ohlc.low()).isGreaterThan(0.0);
            assertThat(ohlc.close()).isGreaterThan(900_000.0);
        }

        @Test
        @DisplayName("Ohlc record — accessors return the correct component values")
        void ohlcRecord_accessorsReturnCorrectValues() {
            // Arrange
            Ohlc ohlc = new Ohlc(1.0, 2.0, 0.5, 1.5);

            // Assert
            assertThat(ohlc.open()).isEqualTo(1.0);
            assertThat(ohlc.high()).isEqualTo(2.0);
            assertThat(ohlc.low()).isEqualTo(0.5);
            assertThat(ohlc.close()).isEqualTo(1.5);
        }
    }

    // =========================================================================
    // ingestIntradayPrices
    // =========================================================================

    /**
     * Tests for {@link InvestmentIntradayAssetPriceService#ingestIntradayPrices()}.
     *
     * <p>Covers:
     * <ul>
     *   <li>Zero assets (count == 0) → returns empty list, bulkCreate never called</li>
     *   <li>Asset with null expandedLatestPrice → skipped, bulkCreate never called</li>
     *   <li>Asset with null previousClosePrice inside expandedLatestPrice → skipped, bulkCreate never called</li>
     *   <li>Single asset with valid latest price → bulkCreate called, GroupResult returned and flattened</li>
     *   <li>Single valid asset → each request has type INTRADAY, correct asset uuid, and exactly 15 candles</li>
     *   <li>Multiple assets with valid latest prices → results from all assets are flattened into one list</li>
     *   <li>Mixed assets (one valid, one with null price) → only valid asset processed</li>
     *   <li>bulkCreate fails with WebClientResponseException → error swallowed, pipeline completes</li>
     *   <li>bulkCreate fails with generic RuntimeException → error swallowed, pipeline completes</li>
     *   <li>listAssetsWithResponseSpec fails with WebClientResponseException → error propagated</li>
     *   <li>listAssetsWithResponseSpec fails with generic RuntimeException → error propagated</li>
     * </ul>
     */
    @Nested
    @DisplayName("ingestIntradayPrices")
    class IngestIntradayPricesTests {

        @Test
        @DisplayName("zero assets (count == 0) — returns empty list, bulkCreate never called")
        void ingestIntradayPrices_zeroAssets_returnsEmptyList() {
            // Arrange
            WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
            PaginatedExpandedAssetList emptyPage = PaginatedExpandedAssetList.builder()
                .count(0)
                .results(List.of())
                .build();

            when(assetUniverseApi.listAssetsWithResponseSpec(
                isNull(), isNull(), isNull(), isNull(),
                any(), isNull(), isNull(), any(),
                isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull()))
                .thenReturn(responseSpec);
            when(responseSpec.bodyToMono(PaginatedExpandedAssetList.class))
                .thenReturn(Mono.just(emptyPage));

            // Act & Assert
            StepVerifier.create(service.ingestIntradayPrices())
                .assertNext(result -> assertThat(result).isEmpty())
                .verifyComplete();

            verify(assetUniverseApi, never()).bulkCreateIntradayAssetPrice(any(), any(), any(), any());
        }

        @Test
        @DisplayName("asset with null expandedLatestPrice — skipped, bulkCreate never called")
        void ingestIntradayPrices_assetWithNullLatestPrice_skipped() {
            // Arrange
            AssetWithMarketAndLatestPrice assetWithNullLatestPrice =
                new AssetWithMarketAndLatestPrice(UUID.randomUUID(), buildExpandedMarket(), null);

            WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
            PaginatedExpandedAssetList page = PaginatedExpandedAssetList.builder()
                .count(1)
                .results(List.of(assetWithNullLatestPrice))
                .build();

            when(assetUniverseApi.listAssetsWithResponseSpec(
                isNull(), isNull(), isNull(), isNull(),
                any(), isNull(), isNull(), any(),
                isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull()))
                .thenReturn(responseSpec);
            when(responseSpec.bodyToMono(PaginatedExpandedAssetList.class))
                .thenReturn(Mono.just(page));

            // Act & Assert
            StepVerifier.create(service.ingestIntradayPrices())
                .assertNext(result -> assertThat(result).isEmpty())
                .verifyComplete();

            verify(assetUniverseApi, never()).bulkCreateIntradayAssetPrice(any(), any(), any(), any());
        }

        @Test
        @DisplayName("asset with null previousClosePrice — skipped, bulkCreate never called")
        void ingestIntradayPrices_assetWithNullPreviousClosePrice_skipped() {
            // Arrange
            ExpandedLatestPrice latestPriceWithNullClose =
                new ExpandedLatestPrice(100.0, OffsetDateTime.now(), 99.0, 101.0, 98.0, null);
            AssetWithMarketAndLatestPrice asset =
                new AssetWithMarketAndLatestPrice(UUID.randomUUID(), buildExpandedMarket(), latestPriceWithNullClose);

            WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
            PaginatedExpandedAssetList page = PaginatedExpandedAssetList.builder()
                .count(1)
                .results(List.of(asset))
                .build();

            when(assetUniverseApi.listAssetsWithResponseSpec(
                isNull(), isNull(), isNull(), isNull(),
                any(), isNull(), isNull(), any(),
                isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull()))
                .thenReturn(responseSpec);
            when(responseSpec.bodyToMono(PaginatedExpandedAssetList.class))
                .thenReturn(Mono.just(page));

            // Act & Assert
            StepVerifier.create(service.ingestIntradayPrices())
                .assertNext(result -> assertThat(result).isEmpty())
                .verifyComplete();

            verify(assetUniverseApi, never()).bulkCreateIntradayAssetPrice(any(), any(), any(), any());
        }

        @Test
        @DisplayName("single asset with valid latest price — bulkCreate called and GroupResult returned")
        void ingestIntradayPrices_singleValidAsset_bulkCreateCalledAndResultReturned() {
            // Arrange
            UUID assetUuid = UUID.randomUUID();
            AssetWithMarketAndLatestPrice asset = buildValidAsset(assetUuid, 150.0);
            GroupResult groupResult = buildGroupResult();

            WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
            PaginatedExpandedAssetList page = PaginatedExpandedAssetList.builder()
                .count(1)
                .results(List.of(asset))
                .build();

            when(assetUniverseApi.listAssetsWithResponseSpec(
                isNull(), isNull(), isNull(), isNull(),
                any(), isNull(), isNull(), any(),
                isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull()))
                .thenReturn(responseSpec);
            when(responseSpec.bodyToMono(PaginatedExpandedAssetList.class))
                .thenReturn(Mono.just(page));
            when(assetUniverseApi.bulkCreateIntradayAssetPrice(any(), isNull(), isNull(), isNull()))
                .thenReturn(Flux.just(groupResult));

            // Act & Assert
            StepVerifier.create(service.ingestIntradayPrices())
                .assertNext(result -> {
                    assertThat(result).hasSize(1);
                    assertThat(result.getFirst().getUuid()).isEqualTo(groupResult.getUuid());
                })
                .verifyComplete();

            verify(assetUniverseApi).bulkCreateIntradayAssetPrice(any(), isNull(), isNull(), isNull());
        }

        @Test
        @DisplayName("single valid asset — 15 requests submitted, each with type INTRADAY and correct asset uuid")
        void ingestIntradayPrices_singleValidAsset_requestsHaveCorrectTypeUuidAnd15Candles() {
            // Arrange
            UUID assetUuid = UUID.randomUUID();
            AssetWithMarketAndLatestPrice asset = buildValidAsset(assetUuid, 50.0);
            GroupResult groupResult = buildGroupResult();

            var responseSpec = mock(WebClient.ResponseSpec.class);
            PaginatedExpandedAssetList page = PaginatedExpandedAssetList.builder()
                .count(1)
                .results(List.of(asset))
                .build();

            when(assetUniverseApi.listAssetsWithResponseSpec(
                isNull(), isNull(), isNull(), isNull(),
                any(), isNull(), isNull(), any(),
                isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull()))
                .thenReturn(responseSpec);
            when(responseSpec.bodyToMono(PaginatedExpandedAssetList.class))
                .thenReturn(Mono.just(page));
            when(assetUniverseApi.bulkCreateIntradayAssetPrice(any(), isNull(), isNull(), isNull()))
                .thenAnswer(invocation -> {
                    List<OASCreatePriceRequest> requests = invocation.getArgument(0);
                    assertThat(requests).hasSize(15);
                    requests.forEach(req -> {
                        assertThat(req.getType()).isEqualTo(TypeEnum.INTRADAY);
                        assertThat(req.getAsset())
                            .containsEntry("uuid", assetUuid.toString());
                    });
                    return Flux.just(groupResult);
                });

            // Act & Assert
            StepVerifier.create(service.ingestIntradayPrices())
                .assertNext(result -> assertThat(result).hasSize(1))
                .verifyComplete();
        }

        @Test
        @DisplayName("multiple assets with valid latest prices — results from all assets are flattened")
        void ingestIntradayPrices_multipleValidAssets_resultsFlattened() {
            // Arrange
            AssetWithMarketAndLatestPrice asset1 = buildValidAsset(UUID.randomUUID(), 100.0);
            AssetWithMarketAndLatestPrice asset2 = buildValidAsset(UUID.randomUUID(), 200.0);
            GroupResult groupResult1 = buildGroupResult();
            GroupResult groupResult2 = buildGroupResult();

            var responseSpec = mock(WebClient.ResponseSpec.class);
            PaginatedExpandedAssetList page = PaginatedExpandedAssetList.builder()
                .count(2)
                .results(List.of(asset1, asset2))
                .build();

            when(assetUniverseApi.listAssetsWithResponseSpec(
                isNull(), isNull(), isNull(), isNull(),
                any(), isNull(), isNull(), any(),
                isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull()))
                .thenReturn(responseSpec);
            when(responseSpec.bodyToMono(PaginatedExpandedAssetList.class)) // bodyToMono with custom type
                .thenReturn(Mono.just(page));
            when(assetUniverseApi.bulkCreateIntradayAssetPrice(any(), isNull(), isNull(), isNull()))
                .thenReturn(Flux.just(groupResult1))
                .thenReturn(Flux.just(groupResult2));

            // Act & Assert
            StepVerifier.create(service.ingestIntradayPrices())
                .assertNext(result -> assertThat(result).hasSize(2))
                .verifyComplete();
        }

        @Test
        @DisplayName("mixed assets — one valid, one with null latest price — only valid asset processed")
        void ingestIntradayPrices_mixedAssets_onlyValidAssetProcessed() {
            // Arrange
            AssetWithMarketAndLatestPrice validAsset = buildValidAsset(UUID.randomUUID(), 300.0);
            AssetWithMarketAndLatestPrice invalidAsset =
                new AssetWithMarketAndLatestPrice(UUID.randomUUID(), buildExpandedMarket(), null);
            GroupResult groupResult = buildGroupResult();

            WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
            PaginatedExpandedAssetList page = PaginatedExpandedAssetList.builder()
                .count(2)
                .results(List.of(validAsset, invalidAsset))
                .build();

            when(assetUniverseApi.listAssetsWithResponseSpec(
                isNull(), isNull(), isNull(), isNull(),
                any(), isNull(), isNull(), any(),
                isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull()))
                .thenReturn(responseSpec);
            when(responseSpec.bodyToMono(PaginatedExpandedAssetList.class))
                .thenReturn(Mono.just(page));
            when(assetUniverseApi.bulkCreateIntradayAssetPrice(any(), isNull(), isNull(), isNull()))
                .thenReturn(Flux.just(groupResult));

            // Act & Assert
            StepVerifier.create(service.ingestIntradayPrices())
                .assertNext(result -> assertThat(result).hasSize(1))
                .verifyComplete();

            verify(assetUniverseApi).bulkCreateIntradayAssetPrice(any(), isNull(), isNull(), isNull());
        }

        @Test
        @DisplayName("bulkCreate fails with WebClientResponseException — error swallowed, pipeline completes")
        void ingestIntradayPrices_bulkCreateFailsWithWebClientException_errorSwallowedPipelineCompletes() {
            // Arrange
            AssetWithMarketAndLatestPrice asset = buildValidAsset(UUID.randomUUID(), 120.0);

            WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
            PaginatedExpandedAssetList page = PaginatedExpandedAssetList.builder()
                .count(1)
                .results(List.of(asset))
                .build();

            when(assetUniverseApi.listAssetsWithResponseSpec(
                isNull(), isNull(), isNull(), isNull(),
                any(), isNull(), isNull(), any(),
                isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull()))
                .thenReturn(responseSpec);
            when(responseSpec.bodyToMono(PaginatedExpandedAssetList.class))
                .thenReturn(Mono.just(page));
            when(assetUniverseApi.bulkCreateIntradayAssetPrice(any(), isNull(), isNull(), isNull()))
                .thenReturn(Flux.error(notFound()));

            // Act & Assert
            StepVerifier.create(service.ingestIntradayPrices())
                .assertNext(result -> assertThat(result).isEmpty())
                .verifyComplete();
        }

        @Test
        @DisplayName("bulkCreate fails with generic RuntimeException — error swallowed, pipeline completes")
        void ingestIntradayPrices_bulkCreateFailsWithRuntimeException_errorSwallowedPipelineCompletes() {
            // Arrange
            AssetWithMarketAndLatestPrice asset = buildValidAsset(UUID.randomUUID(), 75.0);

            WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
            PaginatedExpandedAssetList page = PaginatedExpandedAssetList.builder()
                .count(1)
                .results(List.of(asset))
                .build();

            when(assetUniverseApi.listAssetsWithResponseSpec(
                isNull(), isNull(), isNull(), isNull(),
                any(), isNull(), isNull(), any(),
                isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull()))
                .thenReturn(responseSpec);
            when(responseSpec.bodyToMono(PaginatedExpandedAssetList.class))
                .thenReturn(Mono.just(page));
            when(assetUniverseApi.bulkCreateIntradayAssetPrice(any(), isNull(), isNull(), isNull()))
                .thenReturn(Flux.error(new RuntimeException("unexpected failure")));

            // Act & Assert
            StepVerifier.create(service.ingestIntradayPrices())
                .assertNext(result -> assertThat(result).isEmpty())
                .verifyComplete();
        }

        @Test
        @DisplayName("listAssetsWithResponseSpec fails with WebClientResponseException — error propagated")
        void ingestIntradayPrices_listAssetsFails_webClientError_errorPropagated() {
            // Arrange
            WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

            when(assetUniverseApi.listAssetsWithResponseSpec(
                isNull(), isNull(), isNull(), isNull(),
                any(), isNull(), isNull(), any(),
                isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull()))
                .thenReturn(responseSpec);
            when(responseSpec.bodyToMono(PaginatedExpandedAssetList.class))
                .thenReturn(Mono.error(notFound()));

            // Act & Assert
            StepVerifier.create(service.ingestIntradayPrices())
                .expectErrorMatches(e -> e instanceof WebClientResponseException
                    && ((WebClientResponseException) e).getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();
        }

        @Test
        @DisplayName("listAssetsWithResponseSpec fails with generic RuntimeException — error propagated")
        void ingestIntradayPrices_listAssetsFails_runtimeError_errorPropagated() {
            // Arrange
            WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

            when(assetUniverseApi.listAssetsWithResponseSpec(
                isNull(), isNull(), isNull(), isNull(),
                any(), isNull(), isNull(), any(),
                isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull()))
                .thenReturn(responseSpec);
            when(responseSpec.bodyToMono(PaginatedExpandedAssetList.class))
                .thenReturn(Mono.error(new RuntimeException("connection refused")));

            // Act & Assert
            StepVerifier.create(service.ingestIntradayPrices())
                .expectErrorMatches(e -> e instanceof RuntimeException
                    && "connection refused".equals(e.getMessage()))
                .verify();
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Builds a 404 NOT_FOUND {@link WebClientResponseException}.
     */
    private WebClientResponseException notFound() {
        return WebClientResponseException.create(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            HttpHeaders.EMPTY,
            null,
            StandardCharsets.UTF_8
        );
    }

    /**
     * Builds an {@link ExpandedMarket} with a fixed NYSE session start time.
     */
    private ExpandedMarket buildExpandedMarket() {
        return new ExpandedMarket(
            "NYSE",
            "New York Stock Exchange",
            true,
            OffsetDateTime.of(2026, 3, 12, 9, 30, 0, 0, ZoneOffset.UTC),
            OffsetDateTime.of(2026, 3, 12, 16, 0, 0, 0, ZoneOffset.UTC),
            null
        );
    }

    /**
     * Builds a valid {@link AssetWithMarketAndLatestPrice} with the given uuid and previousClosePrice.
     */
    private AssetWithMarketAndLatestPrice buildValidAsset(UUID uuid, double previousClosePrice) {
        ExpandedLatestPrice latestPrice = new ExpandedLatestPrice(
            previousClosePrice,
            OffsetDateTime.of(2026, 3, 11, 16, 0, 0, 0, ZoneOffset.UTC),
            previousClosePrice * 0.99,
            previousClosePrice * 1.02,
            previousClosePrice * 0.98,
            previousClosePrice
        );
        return new AssetWithMarketAndLatestPrice(uuid, buildExpandedMarket(), latestPrice);
    }

    /**
     * Builds a {@link GroupResult} with a random UUID and status "SUCCESS".
     */
    private GroupResult buildGroupResult() {
        return new GroupResult(UUID.randomUUID(), "SUCCESS", List.of());
    }
}









