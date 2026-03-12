package com.backbase.stream.investment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.investment.api.service.sync.v1.model.AssetCategory;
import com.backbase.investment.api.service.v1.AssetUniverseApi;
import com.backbase.investment.api.service.v1.model.AssetCategoryType;
import com.backbase.investment.api.service.v1.model.AssetCategoryTypeRequest;
import com.backbase.investment.api.service.v1.model.Market;
import com.backbase.investment.api.service.v1.model.MarketRequest;
import com.backbase.investment.api.service.v1.model.MarketSpecialDay;
import com.backbase.investment.api.service.v1.model.MarketSpecialDayRequest;
import com.backbase.investment.api.service.v1.model.PaginatedAssetCategoryList;
import com.backbase.investment.api.service.v1.model.PaginatedAssetCategoryTypeList;
import com.backbase.investment.api.service.v1.model.PaginatedMarketSpecialDayList;
import com.backbase.stream.investment.model.AssetCategoryEntry;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestAssetUniverseService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests for {@link InvestmentAssetUniverseService}.
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
@DisplayName("InvestmentAssetUniverseService")
class InvestmentAssetUniverseServiceTest {

    private AssetUniverseApi assetUniverseApi;
    private InvestmentRestAssetUniverseService investmentRestAssetUniverseService;
    private InvestmentAssetUniverseService service;

    @BeforeEach
    void setUp() {
        assetUniverseApi = mock(AssetUniverseApi.class);
        investmentRestAssetUniverseService = mock(InvestmentRestAssetUniverseService.class);
        service = new InvestmentAssetUniverseService(assetUniverseApi, investmentRestAssetUniverseService);
    }

    // =========================================================================
    // upsertMarket
    // =========================================================================

    /**
     * Tests for {@link InvestmentAssetUniverseService#upsertMarket(MarketRequest)}.
     *
     * <p>Covers:
     * <ul>
     *   <li>Market already exists → updateMarket is called and updated market returned</li>
     *   <li>Market not found (404) → createMarket is called and created market returned</li>
     *   <li>Non-404 error on getMarket → error propagated as-is</li>
     *   <li>Market exists but updateMarket fails → error propagated</li>
     *   <li>Market not found and createMarket fails → error propagated</li>
     * </ul>
     */
    @Nested
    @DisplayName("upsertMarket")
    class UpsertMarketTests {

        @Test
        @DisplayName("market already exists — updateMarket is called and updated market returned")
        void upsertMarket_marketExists_updateCalledAndReturned() {
            // Arrange
            MarketRequest request = new MarketRequest().code("US");
            Market existing = new Market().code("US").name("US Market");
            Market updated = new Market().code("US").name("US Market Updated");

            when(assetUniverseApi.getMarket("US")).thenReturn(Mono.just(existing));
            when(assetUniverseApi.updateMarket(eq("US"), eq(request))).thenReturn(Mono.just(updated));
            when(assetUniverseApi.createMarket(any())).thenReturn(Mono.empty()); // switchIfEmpty fallback

            // Act & Assert
            StepVerifier.create(service.upsertMarket(request))
                .expectNext(updated)
                .verifyComplete();

            verify(assetUniverseApi).updateMarket(eq("US"), eq(request));
            verify(assetUniverseApi, never()).createMarket(any());
        }

        @Test
        @DisplayName("market not found (404) — createMarket is called and created market returned")
        void upsertMarket_marketNotFound_createCalledAndReturned() {
            // Arrange
            MarketRequest request = new MarketRequest().code("US");
            Market created = new Market().code("US").name("US Market");

            when(assetUniverseApi.getMarket("US")).thenReturn(Mono.error(notFound()));
            when(assetUniverseApi.createMarket(request)).thenReturn(Mono.just(created));

            // Act & Assert
            StepVerifier.create(service.upsertMarket(request))
                .expectNext(created)
                .verifyComplete();

            verify(assetUniverseApi).createMarket(request);
            verify(assetUniverseApi, never()).updateMarket(any(), any());
        }

        @Test
        @DisplayName("non-404 error from getMarket — error propagated without calling create or update")
        void upsertMarket_nonNotFoundError_propagated() {
            // Arrange
            MarketRequest request = new MarketRequest().code("US");

            when(assetUniverseApi.getMarket("US"))
                .thenReturn(Mono.error(new RuntimeException("API error")));

            // Act & Assert
            StepVerifier.create(service.upsertMarket(request))
                .expectErrorMatches(e -> e instanceof RuntimeException && "API error".equals(e.getMessage()))
                .verify();

            verify(assetUniverseApi, never()).createMarket(any());
            verify(assetUniverseApi, never()).updateMarket(any(), any());
        }

        @Test
        @DisplayName("market exists but updateMarket fails — error propagated")
        void upsertMarket_updateFails_errorPropagated() {
            // Arrange
            MarketRequest request = new MarketRequest().code("US");
            Market existing = new Market().code("US").name("US Market");

            when(assetUniverseApi.getMarket("US")).thenReturn(Mono.just(existing));
            when(assetUniverseApi.updateMarket(eq("US"), eq(request)))
                .thenReturn(Mono.error(new RuntimeException("update failed")));
            when(assetUniverseApi.createMarket(any())).thenReturn(Mono.empty()); // switchIfEmpty fallback

            // Act & Assert
            StepVerifier.create(service.upsertMarket(request))
                .expectErrorMatches(e -> e instanceof RuntimeException && "update failed".equals(e.getMessage()))
                .verify();
        }

        @Test
        @DisplayName("market not found and createMarket fails — error propagated")
        void upsertMarket_notFoundAndCreateFails_errorPropagated() {
            // Arrange
            MarketRequest request = new MarketRequest().code("US");

            when(assetUniverseApi.getMarket("US")).thenReturn(Mono.error(notFound()));
            when(assetUniverseApi.createMarket(request))
                .thenReturn(Mono.error(new RuntimeException("create failed")));

            // Act & Assert
            StepVerifier.create(service.upsertMarket(request))
                .expectErrorMatches(e -> e instanceof RuntimeException && "create failed".equals(e.getMessage()))
                .verify();
        }
    }

    // =========================================================================
    // getOrCreateAsset
    // =========================================================================

    /**
     * Tests for {@link InvestmentAssetUniverseService#getOrCreateAsset}.
     *
     * <p>Covers:
     * <ul>
     *   <li>Asset already exists → patchAsset is called, asset mapped and returned</li>
     *   <li>Asset not found (404) → createAsset is called and created asset returned</li>
     *   <li>Non-404 error from getAsset → error propagated</li>
     *   <li>Asset not found and createAsset fails → error propagated</li>
     *   <li>Asset not found and createAsset returns empty → completes empty</li>
     *   <li>Null request → NullPointerException</li>
     * </ul>
     */
    @Nested
    @DisplayName("getOrCreateAsset")
    class GetOrCreateAssetTests {

        @Test
        @DisplayName("asset already exists — patchAsset is called and mapped asset returned")
        void getOrCreateAsset_assetExists_patchCalledAndMappedReturned() {
            // Arrange
            com.backbase.stream.investment.Asset req = buildAsset();
            com.backbase.investment.api.service.v1.model.Asset existingApiAsset =
                new com.backbase.investment.api.service.v1.model.Asset()
                    .isin("ABC123")
                    .market("market")
                    .currency("USD");
            com.backbase.stream.investment.Asset patchedAsset = buildAsset();

            when(assetUniverseApi.getAsset(eq("ABC123_market_USD"), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(existingApiAsset));
            when(investmentRestAssetUniverseService.patchAsset(eq(existingApiAsset), eq(req), any()))
                .thenReturn(Mono.just(patchedAsset));
            when(investmentRestAssetUniverseService.createAsset(any(), any())).thenReturn(Mono.empty()); // switchIfEmpty fallback

            // Act & Assert
            StepVerifier.create(service.getOrCreateAsset(req, null))
                .expectNextMatches(a -> "ABC123".equals(a.getIsin())
                    && "market".equals(a.getMarket())
                    && "USD".equals(a.getCurrency()))
                .verifyComplete();

            verify(investmentRestAssetUniverseService).patchAsset(eq(existingApiAsset), eq(req), any());
            verify(investmentRestAssetUniverseService, never()).createAsset(any(), any());
        }

        @Test
        @DisplayName("asset not found (404) — createAsset is called and created asset returned")
        void getOrCreateAsset_assetNotFound_createCalledAndReturned() {
            // Arrange
            com.backbase.stream.investment.Asset req = buildAsset();
            com.backbase.stream.investment.Asset created = buildAsset();

            when(assetUniverseApi.getAsset(eq("ABC123_market_USD"), isNull(), isNull(), isNull()))
                .thenReturn(Mono.error(notFound()));
            when(investmentRestAssetUniverseService.createAsset(eq(req), eq(Map.of())))
                .thenReturn(Mono.just(created));

            // Act & Assert
            StepVerifier.create(service.getOrCreateAsset(req, Map.of()))
                .expectNext(created)
                .verifyComplete();

            verify(investmentRestAssetUniverseService).createAsset(eq(req), eq(Map.of()));
            verify(investmentRestAssetUniverseService, never())
                .patchAsset(
                    any(com.backbase.investment.api.service.v1.model.Asset.class),
                    any(com.backbase.stream.investment.Asset.class),
                    any());
        }

        @Test
        @DisplayName("non-404 error from getAsset — error propagated")
        void getOrCreateAsset_nonNotFoundError_propagated() {
            // Arrange
            com.backbase.stream.investment.Asset req = buildAsset();

            when(assetUniverseApi.getAsset(anyString(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.error(new RuntimeException("API error")));
            when(investmentRestAssetUniverseService.createAsset(any(), any())).thenReturn(Mono.empty()); // switchIfEmpty fallback

            // Act & Assert
            StepVerifier.create(service.getOrCreateAsset(req, null))
                .expectErrorMatches(e -> e instanceof RuntimeException && "API error".equals(e.getMessage()))
                .verify();

            verify(investmentRestAssetUniverseService, never()).createAsset(any(), any());
            verify(investmentRestAssetUniverseService, never())
                .patchAsset(
                    any(com.backbase.investment.api.service.v1.model.Asset.class),
                    any(com.backbase.stream.investment.Asset.class),
                    any());
        }

        @Test
        @DisplayName("asset not found and createAsset fails — error propagated")
        void getOrCreateAsset_notFoundAndCreateFails_errorPropagated() {
            // Arrange
            com.backbase.stream.investment.Asset req = buildAsset();

            when(assetUniverseApi.getAsset(anyString(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.error(notFound()));
            when(investmentRestAssetUniverseService.createAsset(eq(req), isNull()))
                .thenReturn(Mono.error(new RuntimeException("create failed")));

            // Act & Assert
            StepVerifier.create(service.getOrCreateAsset(req, null))
                .expectErrorMatches(e -> e instanceof RuntimeException && "create failed".equals(e.getMessage()))
                .verify();
        }

        @Test
        @DisplayName("asset not found and createAsset returns empty — completes empty")
        void getOrCreateAsset_notFoundAndCreateReturnsEmpty_completesEmpty() {
            // Arrange
            com.backbase.stream.investment.Asset req = buildAsset();

            when(assetUniverseApi.getAsset(anyString(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.error(notFound()));
            when(investmentRestAssetUniverseService.createAsset(eq(req), isNull()))
                .thenReturn(Mono.empty());

            // Act & Assert
            StepVerifier.create(service.getOrCreateAsset(req, null))
                .verifyComplete();
        }

        @Test
        @DisplayName("null asset request — NullPointerException thrown")
        void getOrCreateAsset_nullRequest_throwsNullPointerException() {
            StepVerifier.create(Mono.defer(() -> service.getOrCreateAsset(null, null)))
                .expectError(NullPointerException.class)
                .verify();
        }
    }

    // =========================================================================
    // upsertMarketSpecialDay
    // =========================================================================

    /**
     * Tests for {@link InvestmentAssetUniverseService#upsertMarketSpecialDay(MarketSpecialDayRequest)}.
     *
     * <p>Covers:
     * <ul>
     *   <li>Matching special day exists → updateMarketSpecialDay is called and updated day returned</li>
     *   <li>Special day list is empty → createMarketSpecialDay is called and created day returned</li>
     *   <li>Special days exist but none match the requested market → createMarketSpecialDay is called</li>
     *   <li>Update of existing special day fails → error propagated</li>
     *   <li>createMarketSpecialDay fails → error propagated</li>
     * </ul>
     */
    @Nested
    @DisplayName("upsertMarketSpecialDay")
    class UpsertMarketSpecialDayTests {

        @Test
        @DisplayName("matching special day exists — updateMarketSpecialDay called and updated day returned")
        void upsertMarketSpecialDay_matchingExists_updateCalledAndReturned() {
            // Arrange
            LocalDate date = LocalDate.of(2025, 12, 25);
            UUID existingUuid = UUID.randomUUID();
            MarketSpecialDayRequest request = buildMarketSpecialDayRequest("NYSE", date);
            MarketSpecialDay existing = buildMarketSpecialDay(existingUuid, "NYSE", date);
            MarketSpecialDay updated = buildMarketSpecialDay(existingUuid, "NYSE", date);

            when(assetUniverseApi.listMarketSpecialDay(date, date, 100, 0))
                .thenReturn(Mono.just(buildMarketSpecialDayPage(List.of(existing))));
            when(assetUniverseApi.updateMarketSpecialDay(eq(existingUuid.toString()), eq(request)))
                .thenReturn(Mono.just(updated));
            when(assetUniverseApi.createMarketSpecialDay(any())).thenReturn(Mono.empty()); // switchIfEmpty fallback

            // Act & Assert
            StepVerifier.create(service.upsertMarketSpecialDay(request))
                .expectNext(updated)
                .verifyComplete();

            verify(assetUniverseApi).updateMarketSpecialDay(eq(existingUuid.toString()), eq(request));
            verify(assetUniverseApi, never()).createMarketSpecialDay(any());
        }

        @Test
        @DisplayName("special day list is empty — createMarketSpecialDay called and created day returned")
        void upsertMarketSpecialDay_emptyList_createCalledAndReturned() {
            // Arrange
            LocalDate date = LocalDate.of(2025, 12, 25);
            MarketSpecialDayRequest request = buildMarketSpecialDayRequest("NYSE", date);
            MarketSpecialDay created = buildMarketSpecialDay(UUID.randomUUID(), "NYSE", date);

            when(assetUniverseApi.listMarketSpecialDay(date, date, 100, 0))
                .thenReturn(Mono.just(buildMarketSpecialDayPage(List.of())));
            when(assetUniverseApi.createMarketSpecialDay(request)).thenReturn(Mono.just(created));

            // Act & Assert
            StepVerifier.create(service.upsertMarketSpecialDay(request))
                .expectNext(created)
                .verifyComplete();

            verify(assetUniverseApi).createMarketSpecialDay(request);
            verify(assetUniverseApi, never()).updateMarketSpecialDay(any(), any());
        }

        @Test
        @DisplayName("special days exist but none match requested market — createMarketSpecialDay called")
        void upsertMarketSpecialDay_noMatchingMarket_createCalledAndReturned() {
            // Arrange
            LocalDate date = LocalDate.of(2025, 12, 25);
            MarketSpecialDayRequest request = buildMarketSpecialDayRequest("NASDAQ", date);
            MarketSpecialDay created = buildMarketSpecialDay(UUID.randomUUID(), "NASDAQ", date);

            // Only NYSE entry exists; NASDAQ is the request
            MarketSpecialDay nyseEntry = buildMarketSpecialDay(UUID.randomUUID(), "NYSE", date);
            when(assetUniverseApi.listMarketSpecialDay(date, date, 100, 0))
                .thenReturn(Mono.just(buildMarketSpecialDayPage(List.of(nyseEntry))));
            when(assetUniverseApi.createMarketSpecialDay(request)).thenReturn(Mono.just(created));

            // Act & Assert
            StepVerifier.create(service.upsertMarketSpecialDay(request))
                .expectNext(created)
                .verifyComplete();

            verify(assetUniverseApi).createMarketSpecialDay(request);
            verify(assetUniverseApi, never()).updateMarketSpecialDay(any(), any());
        }

        @Test
        @DisplayName("matching special day exists but update fails — error propagated")
        void upsertMarketSpecialDay_updateFails_errorPropagated() {
            // Arrange
            LocalDate date = LocalDate.of(2025, 12, 25);
            UUID existingUuid = UUID.randomUUID();
            MarketSpecialDayRequest request = buildMarketSpecialDayRequest("NYSE", date);
            MarketSpecialDay existing = buildMarketSpecialDay(existingUuid, "NYSE", date);

            when(assetUniverseApi.listMarketSpecialDay(date, date, 100, 0))
                .thenReturn(Mono.just(buildMarketSpecialDayPage(List.of(existing))));
            when(assetUniverseApi.updateMarketSpecialDay(eq(existingUuid.toString()), eq(request)))
                .thenReturn(Mono.error(new RuntimeException("update failed")));
            when(assetUniverseApi.createMarketSpecialDay(any())).thenReturn(Mono.empty()); // switchIfEmpty fallback

            // Act & Assert
            StepVerifier.create(service.upsertMarketSpecialDay(request))
                .expectErrorMatches(e -> e instanceof RuntimeException && "update failed".equals(e.getMessage()))
                .verify();
        }

        @Test
        @DisplayName("createMarketSpecialDay fails — error propagated")
        void upsertMarketSpecialDay_createFails_errorPropagated() {
            // Arrange
            LocalDate date = LocalDate.of(2025, 12, 25);
            MarketSpecialDayRequest request = buildMarketSpecialDayRequest("NYSE", date);

            when(assetUniverseApi.listMarketSpecialDay(date, date, 100, 0))
                .thenReturn(Mono.just(buildMarketSpecialDayPage(List.of())));
            when(assetUniverseApi.createMarketSpecialDay(request))
                .thenReturn(Mono.error(new RuntimeException("create failed")));

            // Act & Assert
            StepVerifier.create(service.upsertMarketSpecialDay(request))
                .expectErrorMatches(e -> e instanceof RuntimeException && "create failed".equals(e.getMessage()))
                .verify();
        }
    }

    // =========================================================================
    // upsertAssetCategory
    // =========================================================================

    /**
     * Tests for {@link InvestmentAssetUniverseService#upsertAssetCategory(AssetCategoryEntry)}.
     *
     * <p>Covers:
     * <ul>
     *   <li>Null entry → returns Mono.empty()</li>
     *   <li>Matching category exists → patchAssetCategory is called and result returned</li>
     *   <li>No category in list matches code → createAssetCategory is called and result returned</li>
     *   <li>Results list is empty → createAssetCategory is called</li>
     *   <li>Patch fails → onErrorResume swallows error, Mono.empty() returned</li>
     *   <li>Create fails → onErrorResume swallows error, Mono.empty() returned</li>
     *   <li>Successful patch → entry uuid updated to the patched category uuid</li>
     * </ul>
     */
    @Nested
    @DisplayName("upsertAssetCategory")
    class UpsertAssetCategoryTests {

        @Test
        @DisplayName("null entry — returns empty without calling any API")
        void upsertAssetCategory_nullEntry_returnsEmpty() {
            // Act & Assert
            StepVerifier.create(service.upsertAssetCategory(null))
                .verifyComplete();

            verify(assetUniverseApi, never()).listAssetCategories(any(), any(), any(), any(), any(), any());
            verify(investmentRestAssetUniverseService, never()).patchAssetCategory(any(), any(), any());
            verify(investmentRestAssetUniverseService, never()).createAssetCategory(any(), any());
        }

        @Test
        @DisplayName("matching category exists — patchAssetCategory called and result returned")
        void upsertAssetCategory_matchingCategoryExists_patchCalledAndReturned() {
            // Arrange
            UUID existingUuid = UUID.randomUUID();
            AssetCategoryEntry entry = buildAssetCategoryEntry("EQUITY", "Equities", null);

            com.backbase.investment.api.service.v1.model.AssetCategory existingCategory =
                buildApiAssetCategory(existingUuid, "EQUITY");
            when(assetUniverseApi.listAssetCategories(eq("EQUITY"), eq(100), any(), eq(0), any(), any()))
                .thenReturn(Mono.just(buildAssetCategoryPage(List.of(existingCategory))));

            AssetCategory patchedCategory = buildSyncAssetCategory(existingUuid);
            when(investmentRestAssetUniverseService.patchAssetCategory(eq(existingUuid), eq(entry), isNull()))
                .thenReturn(Mono.just(patchedCategory));
            when(investmentRestAssetUniverseService.createAssetCategory(any(), any())).thenReturn(Mono.empty()); // switchIfEmpty fallback

            // Act & Assert
            StepVerifier.create(service.upsertAssetCategory(entry))
                .expectNextMatches(result -> existingUuid.equals(result.getUuid()))
                .verifyComplete();

            verify(investmentRestAssetUniverseService).patchAssetCategory(eq(existingUuid), eq(entry), isNull());
            verify(investmentRestAssetUniverseService, never()).createAssetCategory(any(), any());
        }

        @Test
        @DisplayName("no matching category in list — createAssetCategory called and result returned")
        void upsertAssetCategory_noMatchingCategory_createCalledAndReturned() {
            // Arrange
            AssetCategoryEntry entry = buildAssetCategoryEntry("EQUITY", "Equities", null);

            // Different code in the list — "BONDS" won't match "EQUITY"
            com.backbase.investment.api.service.v1.model.AssetCategory other =
                buildApiAssetCategory(UUID.randomUUID(), "BONDS");
            when(assetUniverseApi.listAssetCategories(eq("EQUITY"), eq(100), any(), eq(0), any(), any()))
                .thenReturn(Mono.just(buildAssetCategoryPage(List.of(other))));

            UUID newUuid = UUID.randomUUID();
            AssetCategory created = buildSyncAssetCategory(newUuid);
            when(investmentRestAssetUniverseService.createAssetCategory(eq(entry), isNull()))
                .thenReturn(Mono.just(created));

            // Act & Assert
            StepVerifier.create(service.upsertAssetCategory(entry))
                .expectNextMatches(result -> newUuid.equals(result.getUuid()))
                .verifyComplete();

            verify(investmentRestAssetUniverseService).createAssetCategory(eq(entry), isNull());
            verify(investmentRestAssetUniverseService, never()).patchAssetCategory(any(), any(), any());
        }

        @Test
        @DisplayName("results list is empty — createAssetCategory called")
        void upsertAssetCategory_emptyResultsList_createCalledAndReturned() {
            // Arrange
            AssetCategoryEntry entry = buildAssetCategoryEntry("EQUITY", "Equities", null);

            when(assetUniverseApi.listAssetCategories(eq("EQUITY"), eq(100), any(), eq(0), any(), any()))
                .thenReturn(Mono.just(buildAssetCategoryPage(List.of())));

            UUID newUuid = UUID.randomUUID();
            AssetCategory created = buildSyncAssetCategory(newUuid);
            when(investmentRestAssetUniverseService.createAssetCategory(eq(entry), isNull()))
                .thenReturn(Mono.just(created));

            // Act & Assert
            StepVerifier.create(service.upsertAssetCategory(entry))
                .expectNextMatches(result -> newUuid.equals(result.getUuid()))
                .verifyComplete();

            verify(investmentRestAssetUniverseService).createAssetCategory(eq(entry), isNull());
        }

        @Test
        @DisplayName("patch fails — onErrorResume swallows error, Mono.empty() returned")
        void upsertAssetCategory_patchFails_errorSwallowedReturnsEmpty() {
            // Arrange
            UUID existingUuid = UUID.randomUUID();
            AssetCategoryEntry entry = buildAssetCategoryEntry("EQUITY", "Equities", null);

            com.backbase.investment.api.service.v1.model.AssetCategory existingCategory =
                buildApiAssetCategory(existingUuid, "EQUITY");
            when(assetUniverseApi.listAssetCategories(eq("EQUITY"), eq(100), any(), eq(0), any(), any()))
                .thenReturn(Mono.just(buildAssetCategoryPage(List.of(existingCategory))));
            when(investmentRestAssetUniverseService.patchAssetCategory(eq(existingUuid), eq(entry), isNull()))
                .thenReturn(Mono.error(new RuntimeException("patch failed")));
            when(investmentRestAssetUniverseService.createAssetCategory(any(), any())).thenReturn(Mono.empty()); // switchIfEmpty fallback (not actually called on error)

            // Act & Assert — onErrorResume returns Mono.empty()
            StepVerifier.create(service.upsertAssetCategory(entry))
                .verifyComplete();
        }

        @Test
        @DisplayName("create fails — onErrorResume swallows error, Mono.empty() returned")
        void upsertAssetCategory_createFails_errorSwallowedReturnsEmpty() {
            // Arrange
            AssetCategoryEntry entry = buildAssetCategoryEntry("EQUITY", "Equities", null);

            when(assetUniverseApi.listAssetCategories(eq("EQUITY"), eq(100), any(), eq(0), any(), any()))
                .thenReturn(Mono.just(buildAssetCategoryPage(List.of())));
            when(investmentRestAssetUniverseService.createAssetCategory(eq(entry), isNull()))
                .thenReturn(Mono.error(new RuntimeException("create failed")));

            // Act & Assert — onErrorResume returns Mono.empty()
            StepVerifier.create(service.upsertAssetCategory(entry))
                .verifyComplete();
        }

        @Test
        @DisplayName("successful patch — entry uuid is updated to patched category uuid")
        void upsertAssetCategory_successfulPatch_entryUuidUpdated() {
            // Arrange
            UUID existingUuid = UUID.randomUUID();
            AssetCategoryEntry entry = buildAssetCategoryEntry("EQUITY", "Equities", null);

            com.backbase.investment.api.service.v1.model.AssetCategory existingCategory =
                buildApiAssetCategory(existingUuid, "EQUITY");
            when(assetUniverseApi.listAssetCategories(eq("EQUITY"), eq(100), any(), eq(0), any(), any()))
                .thenReturn(Mono.just(buildAssetCategoryPage(List.of(existingCategory))));

            AssetCategory patchedCategory = buildSyncAssetCategory(existingUuid);
            when(investmentRestAssetUniverseService.patchAssetCategory(eq(existingUuid), eq(entry), isNull()))
                .thenReturn(Mono.just(patchedCategory));

            // Act & Assert
            StepVerifier.create(service.upsertAssetCategory(entry))
                .expectNextMatches(result -> existingUuid.equals(result.getUuid()))
                .verifyComplete();

            // entry.uuid should be set to the patched category uuid via doOnSuccess
            assertThat(entry.getUuid()).isEqualTo(existingUuid);
        }
    }

    // =========================================================================
    // upsertAssetCategoryType
    // =========================================================================

    /**
     * Tests for {@link InvestmentAssetUniverseService#upsertAssetCategoryType(AssetCategoryTypeRequest)}.
     *
     * <p>Covers:
     * <ul>
     *   <li>Null request → returns Mono.empty()</li>
     *   <li>Matching type exists → updateAssetCategoryType is called and updated type returned</li>
     *   <li>Results list is null → createAssetCategoryType is called</li>
     *   <li>Results list is empty → createAssetCategoryType is called</li>
     *   <li>Results exist but none match code → createAssetCategoryType is called</li>
     *   <li>Update fails → onErrorResume swallows, Mono.empty() returned</li>
     *   <li>Create fails → error propagated</li>
     * </ul>
     */
    @Nested
    @DisplayName("upsertAssetCategoryType")
    class UpsertAssetCategoryTypeTests {

        @Test
        @DisplayName("null request — returns empty without calling any API")
        void upsertAssetCategoryType_nullRequest_returnsEmpty() {
            // Act & Assert
            StepVerifier.create(service.upsertAssetCategoryType(null))
                .verifyComplete();

            verify(assetUniverseApi, never()).listAssetCategoryTypes(any(), any(), any(), any());
            verify(assetUniverseApi, never()).updateAssetCategoryType(any(), any());
            verify(assetUniverseApi, never()).createAssetCategoryType(any());
        }

        @Test
        @DisplayName("matching type exists — updateAssetCategoryType called and updated type returned")
        void upsertAssetCategoryType_matchingExists_updateCalledAndReturned() {
            // Arrange
            UUID existingUuid = UUID.randomUUID();
            AssetCategoryTypeRequest request = buildAssetCategoryTypeRequest("SECTOR", "Sector");

            AssetCategoryType existingType = buildAssetCategoryType(existingUuid, "SECTOR", "Sector");
            when(assetUniverseApi.listAssetCategoryTypes(eq("SECTOR"), eq(100), eq("Sector"), eq(0)))
                .thenReturn(Mono.just(buildAssetCategoryTypePage(List.of(existingType))));

            AssetCategoryType updated = buildAssetCategoryType(existingUuid, "SECTOR", "Sector Updated");
            when(assetUniverseApi.updateAssetCategoryType(eq(existingUuid.toString()), eq(request)))
                .thenReturn(Mono.just(updated));

            // Act & Assert
            StepVerifier.create(service.upsertAssetCategoryType(request))
                .expectNextMatches(result -> "SECTOR".equals(result.getCode()))
                .verifyComplete();

            verify(assetUniverseApi).updateAssetCategoryType(eq(existingUuid.toString()), eq(request));
            verify(assetUniverseApi, never()).createAssetCategoryType(any());
        }

        @Test
        @DisplayName("results list is null — createAssetCategoryType called and created type returned")
        void upsertAssetCategoryType_nullResultsList_createCalledAndReturned() {
            // Arrange
            AssetCategoryTypeRequest request = buildAssetCategoryTypeRequest("SECTOR", "Sector");

            PaginatedAssetCategoryTypeList page = new PaginatedAssetCategoryTypeList();
            page.setResults(null);
            when(assetUniverseApi.listAssetCategoryTypes(eq("SECTOR"), eq(100), eq("Sector"), eq(0)))
                .thenReturn(Mono.just(page));

            AssetCategoryType created = buildAssetCategoryType(UUID.randomUUID(), "SECTOR", "Sector");
            when(assetUniverseApi.createAssetCategoryType(request)).thenReturn(Mono.just(created));

            // Act & Assert
            StepVerifier.create(service.upsertAssetCategoryType(request))
                .expectNextMatches(result -> "SECTOR".equals(result.getCode()))
                .verifyComplete();

            verify(assetUniverseApi).createAssetCategoryType(request);
            verify(assetUniverseApi, never()).updateAssetCategoryType(any(), any());
        }

        @Test
        @DisplayName("results list is empty — createAssetCategoryType called and created type returned")
        void upsertAssetCategoryType_emptyResultsList_createCalledAndReturned() {
            // Arrange
            AssetCategoryTypeRequest request = buildAssetCategoryTypeRequest("SECTOR", "Sector");

            when(assetUniverseApi.listAssetCategoryTypes(eq("SECTOR"), eq(100), eq("Sector"), eq(0)))
                .thenReturn(Mono.just(buildAssetCategoryTypePage(List.of())));

            AssetCategoryType created = buildAssetCategoryType(UUID.randomUUID(), "SECTOR", "Sector");
            when(assetUniverseApi.createAssetCategoryType(request)).thenReturn(Mono.just(created));

            // Act & Assert
            StepVerifier.create(service.upsertAssetCategoryType(request))
                .expectNextMatches(result -> "SECTOR".equals(result.getCode()))
                .verifyComplete();

            verify(assetUniverseApi).createAssetCategoryType(request);
            verify(assetUniverseApi, never()).updateAssetCategoryType(any(), any());
        }

        @Test
        @DisplayName("results exist but none match code — createAssetCategoryType called")
        void upsertAssetCategoryType_noMatchingCode_createCalledAndReturned() {
            // Arrange
            AssetCategoryTypeRequest request = buildAssetCategoryTypeRequest("SECTOR", "Sector");

            // Different code in the results
            AssetCategoryType other = buildAssetCategoryType(UUID.randomUUID(), "INDUSTRY", "Industry");
            when(assetUniverseApi.listAssetCategoryTypes(eq("SECTOR"), eq(100), eq("Sector"), eq(0)))
                .thenReturn(Mono.just(buildAssetCategoryTypePage(List.of(other))));

            AssetCategoryType created = buildAssetCategoryType(UUID.randomUUID(), "SECTOR", "Sector");
            when(assetUniverseApi.createAssetCategoryType(request)).thenReturn(Mono.just(created));

            // Act & Assert
            StepVerifier.create(service.upsertAssetCategoryType(request))
                .expectNextMatches(result -> "SECTOR".equals(result.getCode()))
                .verifyComplete();

            verify(assetUniverseApi).createAssetCategoryType(request);
            verify(assetUniverseApi, never()).updateAssetCategoryType(any(), any());
        }

        @Test
        @DisplayName("update fails — onErrorResume swallows error, Mono.empty() returned")
        void upsertAssetCategoryType_updateFails_errorSwallowedReturnsEmpty() {
            // Arrange
            UUID existingUuid = UUID.randomUUID();
            AssetCategoryTypeRequest request = buildAssetCategoryTypeRequest("SECTOR", "Sector");

            AssetCategoryType existingType = buildAssetCategoryType(existingUuid, "SECTOR", "Sector");
            when(assetUniverseApi.listAssetCategoryTypes(eq("SECTOR"), eq(100), eq("Sector"), eq(0)))
                .thenReturn(Mono.just(buildAssetCategoryTypePage(List.of(existingType))));
            when(assetUniverseApi.updateAssetCategoryType(eq(existingUuid.toString()), eq(request)))
                .thenReturn(Mono.error(new RuntimeException("update failed")));

            // switchIfEmpty path stubbed so the mono completes rather than hanging
            when(assetUniverseApi.createAssetCategoryType(request)).thenReturn(Mono.empty());

            // Act & Assert — onErrorResume returns Mono.empty()
            StepVerifier.create(service.upsertAssetCategoryType(request))
                .verifyComplete();
        }

        @Test
        @DisplayName("createAssetCategoryType fails — error propagated")
        void upsertAssetCategoryType_createFails_errorPropagated() {
            // Arrange
            AssetCategoryTypeRequest request = buildAssetCategoryTypeRequest("SECTOR", "Sector");

            when(assetUniverseApi.listAssetCategoryTypes(eq("SECTOR"), eq(100), eq("Sector"), eq(0)))
                .thenReturn(Mono.just(buildAssetCategoryTypePage(List.of())));
            when(assetUniverseApi.createAssetCategoryType(request))
                .thenReturn(Mono.error(new RuntimeException("create failed")));

            // Act & Assert
            StepVerifier.create(service.upsertAssetCategoryType(request))
                .expectErrorMatches(e -> e instanceof RuntimeException && "create failed".equals(e.getMessage()))
                .verify();
        }
    }

    // =========================================================================
    // createAssets
    // =========================================================================

    /**
     * Tests for {@link InvestmentAssetUniverseService#createAssets(List)}.
     *
     * <p>Covers:
     * <ul>
     *   <li>Null list → returns Flux.empty() without calling API</li>
     *   <li>Empty list → returns Flux.empty() without calling API</li>
     *   <li>Non-empty list → listAssetCategories called and each asset processed via getOrCreateAsset</li>
     * </ul>
     */
    @Nested
    @DisplayName("createAssets")
    class CreateAssetsTests {

        @Test
        @DisplayName("null asset list — returns empty Flux without calling any API")
        void createAssets_nullList_returnsEmptyFlux() {
            // Act & Assert
            StepVerifier.create(service.createAssets(null))
                .verifyComplete();

            verify(assetUniverseApi, never()).listAssetCategories(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("empty asset list — returns empty Flux without calling any API")
        void createAssets_emptyList_returnsEmptyFlux() {
            // Act & Assert
            StepVerifier.create(service.createAssets(List.of()))
                .verifyComplete();

            verify(assetUniverseApi, never()).listAssetCategories(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("non-empty list — listAssetCategories called and each asset processed")
        void createAssets_nonEmptyList_listCategoriesCalledAndAssetsProcessed() {
            // Arrange
            com.backbase.stream.investment.Asset assetReq = buildAsset();
            com.backbase.stream.investment.Asset created = buildAsset();

            when(assetUniverseApi.listAssetCategories(isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(buildAssetCategoryPage(List.of())));
            when(assetUniverseApi.getAsset(anyString(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.error(notFound()));
            when(investmentRestAssetUniverseService.createAsset(eq(assetReq), any()))
                .thenReturn(Mono.just(created));

            // Act & Assert
            StepVerifier.create(service.createAssets(List.of(assetReq)))
                .expectNextCount(1)
                .verifyComplete();

            verify(assetUniverseApi).listAssetCategories(isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
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
     * Builds a stream {@link com.backbase.stream.investment.Asset} with fixed ISIN, market and currency.
     */
    private com.backbase.stream.investment.Asset buildAsset() {
        com.backbase.stream.investment.Asset asset = new com.backbase.stream.investment.Asset();
        asset.setIsin("ABC123");
        asset.setMarket("market");
        asset.setCurrency("USD");
        return asset;
    }

    /**
     * Builds a {@link MarketSpecialDayRequest} for the given market and date.
     */
    private MarketSpecialDayRequest buildMarketSpecialDayRequest(String market, LocalDate date) {
        MarketSpecialDayRequest request = new MarketSpecialDayRequest();
        request.setMarket(market);
        request.setDate(date);
        return request;
    }

    /**
     * Builds a {@link MarketSpecialDay} with the given uuid, market and date.
     */
    private MarketSpecialDay buildMarketSpecialDay(UUID uuid, String market, LocalDate date) {
        MarketSpecialDay day = new MarketSpecialDay(uuid);
        day.setMarket(market);
        day.setDate(date);
        return day;
    }

    /**
     * Builds a {@link PaginatedMarketSpecialDayList} wrapping the given results.
     */
    private PaginatedMarketSpecialDayList buildMarketSpecialDayPage(List<MarketSpecialDay> results) {
        PaginatedMarketSpecialDayList page = new PaginatedMarketSpecialDayList();
        page.setResults(new ArrayList<>(results));
        page.setCount(results.size());
        return page;
    }

    /**
     * Builds an {@link AssetCategoryEntry} with the given code, name and optional uuid.
     */
    private AssetCategoryEntry buildAssetCategoryEntry(String code, String name, UUID uuid) {
        AssetCategoryEntry entry = new AssetCategoryEntry();
        entry.setCode(code);
        entry.setName(name);
        entry.setUuid(uuid);
        return entry;
    }

    /**
     * Builds a v1 API {@link com.backbase.investment.api.service.v1.model.AssetCategory}
     * with the given uuid and code.
     */
    private com.backbase.investment.api.service.v1.model.AssetCategory buildApiAssetCategory(UUID uuid, String code) {
        com.backbase.investment.api.service.v1.model.AssetCategory category =
            new com.backbase.investment.api.service.v1.model.AssetCategory(uuid);
        category.setCode(code);
        return category;
    }

    /**
     * Builds a sync API {@link AssetCategory} with the given uuid.
     */
    private AssetCategory buildSyncAssetCategory(UUID uuid) {
        return new AssetCategory(uuid);
    }

    /**
     * Builds a {@link PaginatedAssetCategoryList} wrapping the given results.
     */
    private PaginatedAssetCategoryList buildAssetCategoryPage(
        List<com.backbase.investment.api.service.v1.model.AssetCategory> results) {
        PaginatedAssetCategoryList page = new PaginatedAssetCategoryList();
        page.setResults(new ArrayList<>(results));
        page.setCount(results.size());
        return page;
    }

    /**
     * Builds an {@link AssetCategoryTypeRequest} with the given code and name.
     */
    private AssetCategoryTypeRequest buildAssetCategoryTypeRequest(String code, String name) {
        AssetCategoryTypeRequest request = new AssetCategoryTypeRequest();
        request.setCode(code);
        request.setName(name);
        return request;
    }

    /**
     * Builds an {@link AssetCategoryType} with the given uuid, code and name.
     */
    private AssetCategoryType buildAssetCategoryType(UUID uuid, String code, String name) {
        AssetCategoryType type = new AssetCategoryType(uuid);
        type.setCode(code);
        type.setName(name);
        return type;
    }

    /**
     * Builds a {@link PaginatedAssetCategoryTypeList} wrapping the given results.
     */
    private PaginatedAssetCategoryTypeList buildAssetCategoryTypePage(List<AssetCategoryType> results) {
        PaginatedAssetCategoryTypeList page = new PaginatedAssetCategoryTypeList();
        page.setResults(new ArrayList<>(results));
        page.setCount(results.size());
        return page;
    }
}