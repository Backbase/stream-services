package com.backbase.stream.investment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import java.net.URI;
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
import reactor.core.Exceptions;
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
        @DisplayName("market already exists with identical data — updateMarket is skipped and existing market returned")
        void upsertMarket_marketExistsUnchanged_updateSkipped() {
            // Arrange — request and existing market carry exactly the same fields
            MarketRequest request = new MarketRequest()
                .code("US")
                .name("US Market")
                .sessionStart("09:30")
                .sessionEnd("16:00");
            Market existing = new Market()
                .code("US")
                .name("US Market")
                .sessionStart("09:30")
                .sessionEnd("16:00");

            when(assetUniverseApi.getMarket("US")).thenReturn(Mono.just(existing));
            // createMarket is always evaluated eagerly as the switchIfEmpty argument; stub to avoid NPE
            when(assetUniverseApi.createMarket(any())).thenReturn(Mono.just(new Market()));

            // Act & Assert — existing market returned, no update call made
            StepVerifier.create(service.upsertMarket(request))
                .expectNext(existing)
                .verifyComplete();

            verify(assetUniverseApi, never()).updateMarket(any(), any());
        }

        @Test
        @DisplayName("market already exists — updateMarket is called and updated market returned")
        void upsertMarket_marketExists_updateCalledAndReturned() {
            // Arrange
            MarketRequest request = new MarketRequest().code("US");
            Market existing = new Market().code("US").name("US Market");
            Market updated = new Market().code("US").name("US Market Updated");

            when(assetUniverseApi.getMarket("US")).thenReturn(Mono.just(existing));
            when(assetUniverseApi.updateMarket("US", request)).thenReturn(Mono.just(updated));
            when(assetUniverseApi.createMarket(any())).thenReturn(Mono.empty()); // switchIfEmpty fallback

            // Act & Assert
            StepVerifier.create(service.upsertMarket(request))
                .expectNext(updated)
                .verifyComplete();

            verify(assetUniverseApi).updateMarket("US", request);
            // Note: createMarket is always invoked eagerly as the switchIfEmpty argument during Mono assembly,
            // even when the update path is taken. Verify it was not *subscribed to* indirectly via updateMarket.
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
            // createMarket is always evaluated eagerly as the switchIfEmpty argument; stub to avoid NPE
            when(assetUniverseApi.createMarket(any())).thenReturn(Mono.just(new Market()));

            // Act & Assert
            StepVerifier.create(service.upsertMarket(request))
                .expectErrorMatches(e -> e instanceof RuntimeException && "API error".equals(e.getMessage()))
                .verify();

            verify(assetUniverseApi, never()).updateMarket(any(), any());
        }

        @Test
        @DisplayName("market exists but updateMarket fails — error propagated")
        void upsertMarket_updateFails_errorPropagated() {
            // Arrange
            MarketRequest request = new MarketRequest().code("US");
            Market existing = new Market().code("US").name("US Market");

            when(assetUniverseApi.getMarket("US")).thenReturn(Mono.just(existing));
            when(assetUniverseApi.updateMarket("US", request))
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

        @Test
        @DisplayName("503 on updateMarket — retries exhausted, RetryExhaustedException propagated")
        void upsertMarket_503OnUpdate_retriesExhaustedErrorPropagated() {
            // Arrange — existing has a name; request does not, so the data differs and update is triggered
            MarketRequest request = new MarketRequest().code("US");
            Market existing = new Market().code("US").name("US Market");

            when(assetUniverseApi.getMarket("US")).thenReturn(Mono.just(existing));
            when(assetUniverseApi.updateMarket("US", request))
                .thenReturn(Mono.error(serverError(503)));
            // createMarket is always evaluated eagerly as the switchIfEmpty argument; stub to avoid NPE
            when(assetUniverseApi.createMarket(any())).thenReturn(Mono.just(new Market()));

            // Act & Assert — after 3 retries the error is wrapped in RetryExhaustedException
            StepVerifier.create(service.upsertMarket(request))
                .expectErrorSatisfies(e -> assertThat(Exceptions.isRetryExhausted(e)).isTrue())
                .verify();
        }

        @Test
        @DisplayName("non-retryable HTTP 400 error on createMarket — propagated immediately without retry")
        void upsertMarket_nonRetryableHttpErrorOnCreate_propagatedImmediately() {
            // Arrange — 400 Bad Request is not in the retryable set (only 409 and 503 are)
            MarketRequest request = new MarketRequest().code("US");

            when(assetUniverseApi.getMarket("US")).thenReturn(Mono.error(notFound()));
            when(assetUniverseApi.createMarket(request)).thenReturn(Mono.error(serverError(400)));

            // Act & Assert
            StepVerifier.create(service.upsertMarket(request))
                .expectErrorMatches(e -> e instanceof WebClientResponseException
                    && ((WebClientResponseException) e).getStatusCode().value() == 400)
                .verify();

            verify(assetUniverseApi, times(1)).createMarket(request);
        }
    }

    // =========================================================================
    // upsertAsset
    // =========================================================================

    /**
     * Tests for {@link InvestmentAssetUniverseService#upsertAsset}.
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
    @DisplayName("upsertAsset")
    class UpsertAssetTests {

        @Test
        @DisplayName("asset already exists with identical data and no logo configured — patchAsset is skipped")
        void upsertAsset_assetExistsUnchangedNoLogo_patchSkipped() {
            // Arrange — desired asset has no logo, data fields identical → skip
            com.backbase.stream.investment.Asset req = buildAsset(); // logo=null
            com.backbase.investment.api.service.v1.model.Asset existingApiAsset =
                new com.backbase.investment.api.service.v1.model.Asset()
                    .isin("ABC123").market("market").currency("USD");

            when(assetUniverseApi.getAsset("ABC123_market_USD", null, null, null))
                .thenReturn(Mono.just(existingApiAsset));
            when(investmentRestAssetUniverseService.createAsset(any(), any())).thenReturn(Mono.empty());

            StepVerifier.create(service.upsertAsset(req, Map.of()))
                .expectNextMatches(a -> "ABC123".equals(a.getIsin()))
                .verifyComplete();

            verify(investmentRestAssetUniverseService, never())
                .patchAsset(any(com.backbase.investment.api.service.v1.model.Asset.class),
                    any(com.backbase.stream.investment.Asset.class), any());
        }

        @Test
        @DisplayName("asset exists, logo filename found in server URI — patchAsset is skipped")
        void upsertAsset_assetExistsLogoFilenameInServerUri_patchSkipped() {
            // Arrange — server returns a signed URI that contains the desired logo filename
            com.backbase.stream.investment.Asset req = buildAsset();
            req.setLogo("apple.png"); // desired filename

            com.backbase.investment.api.service.v1.model.Asset existingApiAsset =
                new com.backbase.investment.api.service.v1.model.Asset()
                    .isin("ABC123").market("market").currency("USD")
                    .logo(URI.create("http://azurite:10000/account1/assets/logos/apple.png?se=2029-05-25&sp=r"));

            when(assetUniverseApi.getAsset("ABC123_market_USD", null, null, null))
                .thenReturn(Mono.just(existingApiAsset));
            when(investmentRestAssetUniverseService.createAsset(any(), any())).thenReturn(Mono.empty());

            // Act & Assert — URI contains "apple.png" → logo unchanged → skip
            StepVerifier.create(service.upsertAsset(req, Map.of()))
                .expectNextMatches(a -> "ABC123".equals(a.getIsin()))
                .verifyComplete();

            verify(investmentRestAssetUniverseService, never())
                .patchAsset(any(com.backbase.investment.api.service.v1.model.Asset.class),
                    any(com.backbase.stream.investment.Asset.class), any());
        }

        @Test
        @DisplayName("asset exists, logo filename NOT in server URI — patchAsset IS called")
        void upsertAsset_assetExistsLogoFilenameNotInServerUri_patchCalled() {
            // Arrange — server URI contains "old-logo.png", desired is "new-logo.png"
            com.backbase.stream.investment.Asset req = buildAsset();
            req.setLogo("new-logo.png");

            com.backbase.investment.api.service.v1.model.Asset existingApiAsset =
                new com.backbase.investment.api.service.v1.model.Asset()
                    .isin("ABC123").market("market").currency("USD")
                    .logo(URI.create("http://azurite:10000/account1/assets/logos/old-logo.png?se=2029-05-25"));

            when(assetUniverseApi.getAsset("ABC123_market_USD", null, null, null))
                .thenReturn(Mono.just(existingApiAsset));
            com.backbase.stream.investment.Asset patchedAsset = buildAsset();
            when(investmentRestAssetUniverseService.patchAsset(eq(existingApiAsset), eq(req), any()))
                .thenReturn(Mono.just(patchedAsset));
            when(investmentRestAssetUniverseService.createAsset(any(), any())).thenReturn(Mono.empty());

            // Act & Assert — URI does not contain "new-logo.png" → patch IS called
            StepVerifier.create(service.upsertAsset(req, Map.of()))
                .expectNextMatches(a -> "ABC123".equals(a.getIsin()))
                .verifyComplete();

            verify(investmentRestAssetUniverseService)
                .patchAsset(eq(existingApiAsset), eq(req), any());
        }

        @Test
        @DisplayName("asset exists, logo desired but server has no logo URI yet — patchAsset IS called")
        void upsertAsset_assetExistsLogoDesiredButNoServerUri_patchCalled() {
            // Arrange — logo configured but server has no URI yet (logo never uploaded)
            com.backbase.stream.investment.Asset req = buildAsset();
            req.setLogo("apple.png");

            com.backbase.investment.api.service.v1.model.Asset existingApiAsset =
                new com.backbase.investment.api.service.v1.model.Asset()
                    .isin("ABC123").market("market").currency("USD")
                    .logo(null); // no logo on server yet

            when(assetUniverseApi.getAsset("ABC123_market_USD", null, null, null))
                .thenReturn(Mono.just(existingApiAsset));
            com.backbase.stream.investment.Asset patchedAsset = buildAsset();
            when(investmentRestAssetUniverseService.patchAsset(eq(existingApiAsset), eq(req), any()))
                .thenReturn(Mono.just(patchedAsset));
            when(investmentRestAssetUniverseService.createAsset(any(), any())).thenReturn(Mono.empty());

            // Act & Assert — logo desired but none stored → patch IS called
            StepVerifier.create(service.upsertAsset(req, Map.of()))
                .expectNextMatches(a -> "ABC123".equals(a.getIsin()))
                .verifyComplete();

            verify(investmentRestAssetUniverseService)
                .patchAsset(eq(existingApiAsset), eq(req), any());
        }

        @Test
        @DisplayName("asset already exists — patchAsset is called and mapped asset returned")
        void upsertAsset_assetExists_patchCalledAndMappedReturned() {
            // Arrange — req carries a name that existingApiAsset does not, so data differs and patch is triggered
            com.backbase.stream.investment.Asset req = buildAsset();
            req.setName("Updated Name"); // differs from existingApiAsset (null name) → triggers patch
            com.backbase.investment.api.service.v1.model.Asset existingApiAsset =
                new com.backbase.investment.api.service.v1.model.Asset()
                    .isin("ABC123")
                    .market("market")
                    .currency("USD");
            com.backbase.stream.investment.Asset patchedAsset = buildAsset();

            when(assetUniverseApi.getAsset("ABC123_market_USD", null, null, null))
                .thenReturn(Mono.just(existingApiAsset));
            when(investmentRestAssetUniverseService.patchAsset(eq(existingApiAsset), eq(req), any()))
                .thenReturn(Mono.just(patchedAsset));
            when(investmentRestAssetUniverseService.createAsset(any(), any())).thenReturn(Mono.empty());

            // Act & Assert
            StepVerifier.create(service.upsertAsset(req, null))
                .expectNextMatches(a -> "ABC123".equals(a.getIsin())
                    && "market".equals(a.getMarket())
                    && "USD".equals(a.getCurrency()))
                .verifyComplete();

            verify(investmentRestAssetUniverseService).patchAsset(eq(existingApiAsset), eq(req), any());
            verify(investmentRestAssetUniverseService, never()).createAsset(any(), any());
        }

        @Test
        @DisplayName("asset not found (404) — createAsset is called and created asset returned")
        void upsertAsset_assetNotFound_createCalledAndReturned() {
            // Arrange
            com.backbase.stream.investment.Asset req = buildAsset();
            com.backbase.stream.investment.Asset created = buildAsset();

            when(assetUniverseApi.getAsset("ABC123_market_USD", null, null, null))
                .thenReturn(Mono.error(notFound()));
            when(investmentRestAssetUniverseService.createAsset(req, Map.of()))
                .thenReturn(Mono.just(created));

            // Act & Assert
            StepVerifier.create(service.upsertAsset(req, Map.of()))
                .expectNext(created)
                .verifyComplete();

            verify(investmentRestAssetUniverseService).createAsset(req, Map.of());
            verify(investmentRestAssetUniverseService, never())
                .patchAsset(
                    any(com.backbase.investment.api.service.v1.model.Asset.class),
                    any(com.backbase.stream.investment.Asset.class),
                    any());
        }

        @Test
        @DisplayName("non-404 error from getAsset — error propagated")
        void upsertAsset_nonNotFoundError_propagated() {
            // Arrange
            com.backbase.stream.investment.Asset req = buildAsset();

            when(assetUniverseApi.getAsset(anyString(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.error(new RuntimeException("API error")));
            when(investmentRestAssetUniverseService.createAsset(any(), any())).thenReturn(Mono.empty());

            // Act & Assert
            StepVerifier.create(service.upsertAsset(req, null))
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
        void upsertAsset_notFoundAndCreateFails_errorPropagated() {
            // Arrange
            com.backbase.stream.investment.Asset req = buildAsset();

            when(assetUniverseApi.getAsset(anyString(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.error(notFound()));
            when(investmentRestAssetUniverseService.createAsset(req, null))
                .thenReturn(Mono.error(new RuntimeException("create failed")));

            // Act & Assert
            StepVerifier.create(service.upsertAsset(req, null))
                .expectErrorMatches(e -> e instanceof RuntimeException && "create failed".equals(e.getMessage()))
                .verify();
        }

        @Test
        @DisplayName("asset not found and createAsset returns empty — completes empty")
        void upsertAsset_notFoundAndCreateReturnsEmpty_completesEmpty() {
            // Arrange
            com.backbase.stream.investment.Asset req = buildAsset();

            when(assetUniverseApi.getAsset(anyString(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.error(notFound()));
            when(investmentRestAssetUniverseService.createAsset(req, null))
                .thenReturn(Mono.empty());

            // Act & Assert
            StepVerifier.create(service.upsertAsset(req, null))
                .verifyComplete();
        }

        @Test
        @DisplayName("null asset request — NullPointerException thrown")
        void upsertAsset_nullRequest_throwsNullPointerException() {
            StepVerifier.create(Mono.defer(() -> service.upsertAsset(null, null)))
                .expectError(NullPointerException.class)
                .verify();
        }

        @Test
        @DisplayName("createAsset fails with WebClientResponseException — error propagated")
        void upsertAsset_createFailsWithWebClientException_errorPropagated() {
            // Arrange
            com.backbase.stream.investment.Asset req = buildAsset();

            when(assetUniverseApi.getAsset(anyString(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.error(notFound()));
            when(investmentRestAssetUniverseService.createAsset(req, null))
                .thenReturn(Mono.error(serverError(500)));

            // Act & Assert
            StepVerifier.create(service.upsertAsset(req, null))
                .expectErrorMatches(e -> e instanceof WebClientResponseException
                    && ((WebClientResponseException) e).getStatusCode().value() == 500)
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
        @DisplayName("matching special day exists with identical data — updateMarketSpecialDay is skipped and existing day returned")
        void upsertMarketSpecialDay_matchingExistsUnchanged_updateSkipped() {
            // Arrange — request and existing record carry identical fields
            LocalDate date = LocalDate.of(2025, 12, 25);
            UUID existingUuid = UUID.randomUUID();
            MarketSpecialDayRequest request = new MarketSpecialDayRequest()
                .date(date)
                .market("NYSE")
                .description("Christmas")
                .sessionStart("09:30")
                .sessionEnd("13:00");
            MarketSpecialDay existing = new MarketSpecialDay(existingUuid);
            existing.setDate(date);
            existing.setMarket("NYSE");
            existing.setDescription("Christmas");
            existing.setSessionStart("09:30");
            existing.setSessionEnd("13:00");

            when(assetUniverseApi.listMarketSpecialDay(date, date, 100, 0))
                .thenReturn(Mono.just(buildMarketSpecialDayPage(List.of(existing))));
            // createMarketSpecialDay stubbed as the switchIfEmpty argument; should not be subscribed
            when(assetUniverseApi.createMarketSpecialDay(any())).thenReturn(Mono.just(new MarketSpecialDay(UUID.randomUUID())));

            // Act & Assert — existing record returned, no update call made
            StepVerifier.create(service.upsertMarketSpecialDay(request))
                .expectNext(existing)
                .verifyComplete();

            verify(assetUniverseApi, never()).updateMarketSpecialDay(any(), any());
        }

        @Test
        @DisplayName("matching special day exists — updateMarketSpecialDay called and updated day returned")
        void upsertMarketSpecialDay_matchingExists_updateCalledAndReturned() {
            // Arrange — existing has a description the request does not; data differs so update is triggered
            LocalDate date = LocalDate.of(2025, 12, 25);
            UUID existingUuid = UUID.randomUUID();
            MarketSpecialDayRequest request = buildMarketSpecialDayRequest("NYSE", date);
            MarketSpecialDay existing = buildMarketSpecialDay(existingUuid, "NYSE", date);
            existing.setDescription("Old description"); // differs from request (null) → triggers update
            MarketSpecialDay updated = buildMarketSpecialDay(existingUuid, "NYSE", date);

            when(assetUniverseApi.listMarketSpecialDay(date, date, 100, 0))
                .thenReturn(Mono.just(buildMarketSpecialDayPage(List.of(existing))));
            when(assetUniverseApi.updateMarketSpecialDay(existingUuid.toString(), request))
                .thenReturn(Mono.just(updated));
            when(assetUniverseApi.createMarketSpecialDay(any())).thenReturn(Mono.empty()); // switchIfEmpty fallback

            // Act & Assert
            StepVerifier.create(service.upsertMarketSpecialDay(request))
                .expectNext(updated)
                .verifyComplete();

            verify(assetUniverseApi).updateMarketSpecialDay(existingUuid.toString(), request);
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
            // Arrange — existing description differs from request so update is triggered
            LocalDate date = LocalDate.of(2025, 12, 25);
            UUID existingUuid = UUID.randomUUID();
            MarketSpecialDayRequest request = buildMarketSpecialDayRequest("NYSE", date);
            MarketSpecialDay existing = buildMarketSpecialDay(existingUuid, "NYSE", date);
            existing.setDescription("Old description"); // differs from request (null) → triggers update

            when(assetUniverseApi.listMarketSpecialDay(date, date, 100, 0))
                .thenReturn(Mono.just(buildMarketSpecialDayPage(List.of(existing))));
            when(assetUniverseApi.updateMarketSpecialDay(existingUuid.toString(), request))
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

        @Test
        @DisplayName("listMarketSpecialDay API itself fails — error propagated without calling create or update")
        void upsertMarketSpecialDay_listApiError_propagated() {
            // Arrange
            LocalDate date = LocalDate.of(2025, 12, 25);
            MarketSpecialDayRequest request = buildMarketSpecialDayRequest("NYSE", date);

            when(assetUniverseApi.listMarketSpecialDay(date, date, 100, 0))
                .thenReturn(Mono.error(new RuntimeException("list API unavailable")));

            // Act & Assert
            StepVerifier.create(service.upsertMarketSpecialDay(request))
                .expectErrorMatches(e -> e instanceof RuntimeException
                    && "list API unavailable".equals(e.getMessage()))
                .verify();

            verify(assetUniverseApi, never()).createMarketSpecialDay(any());
            verify(assetUniverseApi, never()).updateMarketSpecialDay(any(), any());
        }

        @Test
        @DisplayName("matching special day exists but update fails with WebClientResponseException — error propagated")
        void upsertMarketSpecialDay_webClientExceptionOnUpdate_propagated() {
            // Arrange — existing description differs from request so update is triggered
            LocalDate date = LocalDate.of(2025, 12, 25);
            UUID existingUuid = UUID.randomUUID();
            MarketSpecialDayRequest request = buildMarketSpecialDayRequest("NYSE", date);
            MarketSpecialDay existing = buildMarketSpecialDay(existingUuid, "NYSE", date);
            existing.setDescription("Old description"); // differs from request (null) → triggers update

            when(assetUniverseApi.listMarketSpecialDay(date, date, 100, 0))
                .thenReturn(Mono.just(buildMarketSpecialDayPage(List.of(existing))));
            when(assetUniverseApi.updateMarketSpecialDay(existingUuid.toString(), request))
                .thenReturn(Mono.error(serverError(503)));
            when(assetUniverseApi.createMarketSpecialDay(any())).thenReturn(Mono.empty());

            // Act & Assert — 503 triggers retries; after exhaustion RetryExhaustedException is propagated
            StepVerifier.create(service.upsertMarketSpecialDay(request))
                .expectErrorSatisfies(e -> assertThat(Exceptions.isRetryExhausted(e)).isTrue())
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
            when(investmentRestAssetUniverseService.patchAssetCategory(existingUuid, entry, null))
                .thenReturn(Mono.just(patchedCategory));
            when(investmentRestAssetUniverseService.createAssetCategory(any(), any())).thenReturn(Mono.empty());

            // Act & Assert
            StepVerifier.create(service.upsertAssetCategory(entry))
                .expectNextMatches(result -> existingUuid.equals(result.getUuid()))
                .verifyComplete();

            verify(investmentRestAssetUniverseService).patchAssetCategory(existingUuid, entry, null);
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
            when(investmentRestAssetUniverseService.createAssetCategory(entry, null))
                .thenReturn(Mono.just(created));

            // Act & Assert
            StepVerifier.create(service.upsertAssetCategory(entry))
                .expectNextMatches(result -> newUuid.equals(result.getUuid()))
                .verifyComplete();

            verify(investmentRestAssetUniverseService).createAssetCategory(entry, null);
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
            when(investmentRestAssetUniverseService.createAssetCategory(entry, null))
                .thenReturn(Mono.just(created));

            // Act & Assert
            StepVerifier.create(service.upsertAssetCategory(entry))
                .expectNextMatches(result -> newUuid.equals(result.getUuid()))
                .verifyComplete();

            verify(investmentRestAssetUniverseService).createAssetCategory(entry, null);
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
            when(investmentRestAssetUniverseService.patchAssetCategory(existingUuid, entry, null))
                .thenReturn(Mono.error(new RuntimeException("patch failed")));
            when(investmentRestAssetUniverseService.createAssetCategory(any(), any())).thenReturn(Mono.empty());

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
            when(investmentRestAssetUniverseService.createAssetCategory(entry, null))
                .thenReturn(Mono.error(new RuntimeException("create failed")));

            // Act & Assert — onErrorResume returns Mono.empty()
            StepVerifier.create(service.upsertAssetCategory(entry))
                .verifyComplete();
        }

        @Test
        @DisplayName("existing category unchanged, no imageResource supplied — patchAssetCategory is skipped")
        void upsertAssetCategory_existingUnchangedNoImage_patchSkipped() {
            // Arrange — mirrors real-world: existing has server uuid, desired has uuid=null.
            // No imageResource on desired entry → image check returns unchanged → skip entirely.
            UUID existingUuid = UUID.randomUUID();
            AssetCategoryEntry entry = new AssetCategoryEntry();
            entry.setCode("CERTIFICATE");
            entry.setName("Certificate");
            entry.setOrder(1);
            entry.setType("ASSET_TYPE");
            entry.setDescription("Certificates are structured financial instruments.");
            // uuid and imageResource intentionally null

            com.backbase.investment.api.service.v1.model.AssetCategory existingCategory =
                new com.backbase.investment.api.service.v1.model.AssetCategory(existingUuid);
            existingCategory.setCode("CERTIFICATE");
            existingCategory.setName("Certificate");
            existingCategory.setOrder(1);
            existingCategory.setType("ASSET_TYPE");
            existingCategory.setDescription("Certificates are structured financial instruments.");

            when(assetUniverseApi.listAssetCategories(eq("CERTIFICATE"), eq(100), any(), eq(0), any(), any()))
                .thenReturn(Mono.just(buildAssetCategoryPage(List.of(existingCategory))));

            // Act & Assert — skip path: no patch, uuid set on entry via doOnSuccess
            StepVerifier.create(service.upsertAssetCategory(entry))
                .expectNextMatches(result -> existingUuid.equals(result.getUuid()))
                .verifyComplete();

            assertThat(entry.getUuid()).isEqualTo(existingUuid);
            verify(investmentRestAssetUniverseService, never()).patchAssetCategory(any(), any(), any());
        }

        @Test
        @DisplayName("existing category unchanged, imageResource filename found in server URI — patchAssetCategory is skipped")
        void upsertAssetCategory_existingUnchangedImageFilenameMatches_patchSkipped() {
            // Arrange — server returns a signed URI that contains the desired filename.
            // isImageUnchanged checks existingUri.toString().contains(desiredFilename).
            UUID existingUuid = UUID.randomUUID();
            AssetCategoryEntry entry = new AssetCategoryEntry();
            entry.setCode("TECH_TITANS");
            entry.setName("Tech titans");
            entry.setOrder(3);
            entry.setType("COLLECTION");
            entry.setDescription("Dominant innovators shaping tomorrow's digital economy.");
            entry.setImageResource(new org.springframework.core.io.ByteArrayResource("img".getBytes()) {
                @Override public String getFilename() { return "tech-titans.png"; }
            });

            com.backbase.investment.api.service.v1.model.AssetCategory existingCategory =
                new com.backbase.investment.api.service.v1.model.AssetCategory(existingUuid);
            existingCategory.setCode("TECH_TITANS");
            existingCategory.setName("Tech titans");
            existingCategory.setOrder(3);
            existingCategory.setType("COLLECTION");
            existingCategory.setDescription("Dominant innovators shaping tomorrow's digital economy.");
            // URI string contains "tech-titans.png" → filename found → image unchanged
            existingCategory.setImage(URI.create(
                "http://azurite:10000/account1/asset_categories/images/tech-titans.png"
                    + "?se=2029-05-25T18%3A10%3A11Z&sp=r&sv=2026-02-06&sr=b&sig=abc123"));

            when(assetUniverseApi.listAssetCategories(eq("TECH_TITANS"), eq(100), any(), eq(0), any(), any()))
                .thenReturn(Mono.just(buildAssetCategoryPage(List.of(existingCategory))));

            // Act & Assert — skip: data unchanged AND URI contains desired filename
            StepVerifier.create(service.upsertAssetCategory(entry))
                .expectNextMatches(result -> existingUuid.equals(result.getUuid()))
                .verifyComplete();

            assertThat(entry.getUuid()).isEqualTo(existingUuid);
            verify(investmentRestAssetUniverseService, never()).patchAssetCategory(any(), any(), any());
        }

        @Test
        @DisplayName("existing category unchanged, imageResource filename NOT in server URI — patchAssetCategory IS called")
        void upsertAssetCategory_existingUnchangedButDifferentImageFilename_patchCalled() {
            // Arrange — server URI does not contain the desired filename → image changed → patch
            UUID existingUuid = UUID.randomUUID();
            AssetCategoryEntry entry = new AssetCategoryEntry();
            entry.setCode("EQUITY");
            entry.setName("Equities");
            entry.setOrder(1);
            entry.setType("ASSET_TYPE");
            entry.setImageResource(new org.springframework.core.io.ByteArrayResource("img".getBytes()) {
                @Override public String getFilename() { return "new-logo.png"; }
            });

            com.backbase.investment.api.service.v1.model.AssetCategory existingCategory =
                new com.backbase.investment.api.service.v1.model.AssetCategory(existingUuid);
            existingCategory.setCode("EQUITY");
            existingCategory.setName("Equities");
            existingCategory.setOrder(1);
            existingCategory.setType("ASSET_TYPE");
            // URI contains "old-logo.png", NOT "new-logo.png" → patch required
            existingCategory.setImage(URI.create(
                "http://azurite:10000/account1/asset_categories/images/old-logo.png?se=2029-05-25"));

            when(assetUniverseApi.listAssetCategories(eq("EQUITY"), eq(100), any(), eq(0), any(), any()))
                .thenReturn(Mono.just(buildAssetCategoryPage(List.of(existingCategory))));

            AssetCategory patchedCategory = buildSyncAssetCategory(existingUuid);
            when(investmentRestAssetUniverseService.patchAssetCategory(eq(existingUuid), eq(entry), any()))
                .thenReturn(Mono.just(patchedCategory));

            // Act & Assert — patch IS called because filename not found in server URI
            StepVerifier.create(service.upsertAssetCategory(entry))
                .expectNextMatches(result -> existingUuid.equals(result.getUuid()))
                .verifyComplete();

            verify(investmentRestAssetUniverseService).patchAssetCategory(eq(existingUuid), eq(entry), any());
        }

        @Test
        @DisplayName("imageResource supplied but server has no image URI yet — patchAssetCategory IS called")
        void upsertAssetCategory_imageResourceSuppliedButNoServerImage_patchCalled() {
            // Arrange — first run after data fields already exist but image upload was missed
            UUID existingUuid = UUID.randomUUID();
            AssetCategoryEntry entry = new AssetCategoryEntry();
            entry.setCode("EQUITY");
            entry.setName("Equities");
            entry.setOrder(1);
            entry.setType("ASSET_TYPE");
            entry.setImageResource(new org.springframework.core.io.ByteArrayResource("img".getBytes()) {
                @Override public String getFilename() { return "equity.png"; }
            });

            com.backbase.investment.api.service.v1.model.AssetCategory existingCategory =
                new com.backbase.investment.api.service.v1.model.AssetCategory(existingUuid);
            existingCategory.setCode("EQUITY");
            existingCategory.setName("Equities");
            existingCategory.setOrder(1);
            existingCategory.setType("ASSET_TYPE");
            existingCategory.setImage(null); // no image stored on server yet

            when(assetUniverseApi.listAssetCategories(eq("EQUITY"), eq(100), any(), eq(0), any(), any()))
                .thenReturn(Mono.just(buildAssetCategoryPage(List.of(existingCategory))));

            AssetCategory patchedCategory = buildSyncAssetCategory(existingUuid);
            when(investmentRestAssetUniverseService.patchAssetCategory(eq(existingUuid), eq(entry), any()))
                .thenReturn(Mono.just(patchedCategory));

            // Act & Assert — patch IS called: image desired but none stored yet
            StepVerifier.create(service.upsertAssetCategory(entry))
                .expectNextMatches(result -> existingUuid.equals(result.getUuid()))
                .verifyComplete();

            verify(investmentRestAssetUniverseService).patchAssetCategory(eq(existingUuid), eq(entry), any());
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
            when(investmentRestAssetUniverseService.patchAssetCategory(existingUuid, entry, null))
                .thenReturn(Mono.just(patchedCategory));

            // Act & Assert
            StepVerifier.create(service.upsertAssetCategory(entry))
                .expectNextMatches(result -> existingUuid.equals(result.getUuid()))
                .verifyComplete();

            // entry.uuid should be set to the patched category uuid via doOnSuccess
            assertThat(entry.getUuid()).isEqualTo(existingUuid);
        }

        @Test
        @DisplayName("WebClientResponseException on patchAssetCategory — error swallowed by onErrorResume")
        void upsertAssetCategory_webClientExceptionOnPatch_swallowedReturnsEmpty() {
            // Arrange
            UUID existingUuid = UUID.randomUUID();
            AssetCategoryEntry entry = buildAssetCategoryEntry("EQUITY", "Equities", null);

            com.backbase.investment.api.service.v1.model.AssetCategory existingCategory =
                buildApiAssetCategory(existingUuid, "EQUITY");
            when(assetUniverseApi.listAssetCategories(eq("EQUITY"), eq(100), any(), eq(0), any(), any()))
                .thenReturn(Mono.just(buildAssetCategoryPage(List.of(existingCategory))));
            when(investmentRestAssetUniverseService.patchAssetCategory(existingUuid, entry, null))
                .thenReturn(Mono.error(serverError(500)));
            when(investmentRestAssetUniverseService.createAssetCategory(any(), any()))
                .thenReturn(Mono.empty());

            // Act & Assert — onErrorResume swallows WebClientResponseException too
            StepVerifier.create(service.upsertAssetCategory(entry))
                .verifyComplete();
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
        @DisplayName("matching type exists with identical data — updateAssetCategoryType is skipped and existing type returned")
        void upsertAssetCategoryType_matchingExistsUnchanged_updateSkipped() {
            // Arrange — request and existing type carry identical code and name
            UUID existingUuid = UUID.randomUUID();
            AssetCategoryTypeRequest request = buildAssetCategoryTypeRequest("SECTOR", "Sector");
            AssetCategoryType existingType = buildAssetCategoryType(existingUuid, "SECTOR", "Sector");

            when(assetUniverseApi.listAssetCategoryTypes("SECTOR", 100, "Sector", 0))
                .thenReturn(Mono.just(buildAssetCategoryTypePage(List.of(existingType))));

            // Act & Assert — existing type returned, no update call made
            StepVerifier.create(service.upsertAssetCategoryType(request))
                .expectNext(existingType)
                .verifyComplete();

            verify(assetUniverseApi, never()).updateAssetCategoryType(any(), any());
            verify(assetUniverseApi, never()).createAssetCategoryType(any());
        }

        @Test
        @DisplayName("matching type exists — updateAssetCategoryType called and updated type returned")
        void upsertAssetCategoryType_matchingExists_updateCalledAndReturned() {
            // Arrange — request name differs from existingType name so update IS triggered
            UUID existingUuid = UUID.randomUUID();
            AssetCategoryTypeRequest request = buildAssetCategoryTypeRequest("SECTOR", "Sector Updated");

            AssetCategoryType existingType = buildAssetCategoryType(existingUuid, "SECTOR", "Sector");
            when(assetUniverseApi.listAssetCategoryTypes("SECTOR", 100, "Sector Updated", 0))
                .thenReturn(Mono.just(buildAssetCategoryTypePage(List.of(existingType))));

            AssetCategoryType updated = buildAssetCategoryType(existingUuid, "SECTOR", "Sector Updated");
            when(assetUniverseApi.updateAssetCategoryType(existingUuid.toString(), request))
                .thenReturn(Mono.just(updated));

            // Act & Assert
            StepVerifier.create(service.upsertAssetCategoryType(request))
                .expectNextMatches(result -> "SECTOR".equals(result.getCode()))
                .verifyComplete();

            verify(assetUniverseApi).updateAssetCategoryType(existingUuid.toString(), request);
            verify(assetUniverseApi, never()).createAssetCategoryType(any());
        }

        @Test
        @DisplayName("results list is null — createAssetCategoryType called and created type returned")
        void upsertAssetCategoryType_nullResultsList_createCalledAndReturned() {
            // Arrange
            AssetCategoryTypeRequest request = buildAssetCategoryTypeRequest("SECTOR", "Sector");

            PaginatedAssetCategoryTypeList page = new PaginatedAssetCategoryTypeList();
            page.setResults(null);
            when(assetUniverseApi.listAssetCategoryTypes("SECTOR", 100, "Sector", 0))
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

            when(assetUniverseApi.listAssetCategoryTypes("SECTOR", 100, "Sector", 0))
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
            when(assetUniverseApi.listAssetCategoryTypes("SECTOR", 100, "Sector", 0))
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
            // Arrange — request name differs from existingType so update IS triggered (then fails)
            UUID existingUuid = UUID.randomUUID();
            AssetCategoryTypeRequest request = buildAssetCategoryTypeRequest("SECTOR", "Sector Updated");

            AssetCategoryType existingType = buildAssetCategoryType(existingUuid, "SECTOR", "Sector");
            when(assetUniverseApi.listAssetCategoryTypes("SECTOR", 100, "Sector Updated", 0))
                .thenReturn(Mono.just(buildAssetCategoryTypePage(List.of(existingType))));
            when(assetUniverseApi.updateAssetCategoryType(existingUuid.toString(), request))
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

            when(assetUniverseApi.listAssetCategoryTypes("SECTOR", 100, "Sector", 0))
                .thenReturn(Mono.just(buildAssetCategoryTypePage(List.of())));
            when(assetUniverseApi.createAssetCategoryType(request))
                .thenReturn(Mono.error(new RuntimeException("create failed")));

            // Act & Assert
            StepVerifier.create(service.upsertAssetCategoryType(request))
                .expectErrorMatches(e -> e instanceof RuntimeException && "create failed".equals(e.getMessage()))
                .verify();
        }

        @Test
        @DisplayName("WebClientResponseException on updateAssetCategoryType — swallowed by onErrorResume")
        void upsertAssetCategoryType_webClientExceptionOnUpdate_swallowedReturnsEmpty() {
            // Arrange — request name differs from existingType so update IS triggered (then fails)
            UUID existingUuid = UUID.randomUUID();
            AssetCategoryTypeRequest request = buildAssetCategoryTypeRequest("SECTOR", "Sector Updated");

            AssetCategoryType existingType = buildAssetCategoryType(existingUuid, "SECTOR", "Sector");
            when(assetUniverseApi.listAssetCategoryTypes("SECTOR", 100, "Sector Updated", 0))
                .thenReturn(Mono.just(buildAssetCategoryTypePage(List.of(existingType))));
            when(assetUniverseApi.updateAssetCategoryType(existingUuid.toString(), request))
                .thenReturn(Mono.error(serverError(500)));
            when(assetUniverseApi.createAssetCategoryType(request)).thenReturn(Mono.empty());

            // Act & Assert — onErrorResume wraps both RuntimeException and WebClientResponseException
            StepVerifier.create(service.upsertAssetCategoryType(request))
                .verifyComplete();
        }
    }

    // =========================================================================
    // upsertAssets
    // =========================================================================

    /**
     * Tests for {@link InvestmentAssetUniverseService#upsertAssets(List)}.
     *
     * <p>Covers:
     * <ul>
     *   <li>Null list → returns Flux.empty() without calling API</li>
     *   <li>Empty list → returns Flux.empty() without calling API</li>
     *   <li>Non-empty list → listAssetCategories called and each asset processed via upsertAsset</li>
     * </ul>
     */
    @Nested
    @DisplayName("upsertAssets")
    class UpsertAssetsTests {

        @Test
        @DisplayName("null asset list — returns empty Flux without calling any API")
        void upsertAssets_nullList_returnsEmptyFlux() {
            // Act & Assert
            StepVerifier.create(service.upsertAssets(null))
                .verifyComplete();

            verify(assetUniverseApi, never()).listAssetCategories(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("empty asset list — returns empty Flux without calling any API")
        void upsertAssets_emptyList_returnsEmptyFlux() {
            // Act & Assert
            StepVerifier.create(service.upsertAssets(List.of()))
                .verifyComplete();

            verify(assetUniverseApi, never()).listAssetCategories(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("non-empty list — listAssetCategories called and each asset processed")
        void upsertAssets_nonEmptyList_listCategoriesCalledAndAssetsProcessed() {
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
            StepVerifier.create(service.upsertAssets(List.of(assetReq)))
                .expectNextCount(1)
                .verifyComplete();

            verify(assetUniverseApi).listAssetCategories(isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
        }

        @Test
        @DisplayName("duplicate asset keys in input — deduplicated, only one asset processed")
        void upsertAssets_duplicateAssetKeys_deduplicatedAndProcessedOnce() {
            // Arrange — two distinct instances with the same isin+market+currency key
            com.backbase.stream.investment.Asset asset1 = buildAsset(); // key: ABC123_market_USD
            com.backbase.stream.investment.Asset asset2 = buildAsset(); // same key

            when(assetUniverseApi.listAssetCategories(isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(buildAssetCategoryPage(List.of())));
            when(assetUniverseApi.getAsset("ABC123_market_USD", null, null, null))
                .thenReturn(Mono.error(notFound()));
            when(investmentRestAssetUniverseService.createAsset(any(), any()))
                .thenReturn(Mono.just(asset1));

            // Act & Assert — only one element emitted despite two inputs
            StepVerifier.create(service.upsertAssets(List.of(asset1, asset2)))
                .expectNextCount(1)
                .verifyComplete();

            // createAsset invoked exactly once — duplicate was removed before processing
            verify(investmentRestAssetUniverseService, times(1)).createAsset(any(), any());
        }

        @Test
        @DisplayName("multiple distinct assets — all assets processed and emitted")
        void upsertAssets_multipleDistinctAssets_allAssetsProcessed() {
            // Arrange
            com.backbase.stream.investment.Asset assetA = buildAsset("ISINA", "XNAS", "USD");
            com.backbase.stream.investment.Asset assetB = buildAsset("ISINB", "XAMS", "EUR");

            when(assetUniverseApi.listAssetCategories(isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(buildAssetCategoryPage(List.of())));
            when(assetUniverseApi.getAsset("ISINA_XNAS_USD", null, null, null))
                .thenReturn(Mono.error(notFound()));
            when(assetUniverseApi.getAsset("ISINB_XAMS_EUR", null, null, null))
                .thenReturn(Mono.error(notFound()));
            when(investmentRestAssetUniverseService.createAsset(eq(assetA), any()))
                .thenReturn(Mono.just(assetA));
            when(investmentRestAssetUniverseService.createAsset(eq(assetB), any()))
                .thenReturn(Mono.just(assetB));

            // Act & Assert
            StepVerifier.create(service.upsertAssets(List.of(assetA, assetB)))
                .expectNextCount(2)
                .verifyComplete();

            verify(investmentRestAssetUniverseService, times(2)).createAsset(any(), any());
        }

        @Test
        @DisplayName("listAssetCategories returns page with null results — empty Flux returned without processing assets")
        void upsertAssets_listCategoriesReturnsNullResults_returnsEmptyFlux() {
            // Arrange — the second filter(Objects::nonNull) in the chain stops execution when results is null
            com.backbase.stream.investment.Asset assetReq = buildAsset();
            PaginatedAssetCategoryList nullResultPage = new PaginatedAssetCategoryList();
            nullResultPage.setResults(null);

            when(assetUniverseApi.listAssetCategories(isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(nullResultPage));

            // Act & Assert
            StepVerifier.create(service.upsertAssets(List.of(assetReq)))
                .verifyComplete();

            verify(investmentRestAssetUniverseService, never()).createAsset(any(), any());
        }

        @Test
        @DisplayName("asset already exists — patchAsset called within upsertAssets, existing asset returned")
        void upsertAssets_assetAlreadyExists_patchCalledAndReturned() {
            // Arrange — req carries a name that existingApiAsset does not, so data differs and patch is triggered
            com.backbase.stream.investment.Asset req = buildAsset();
            req.setName("Updated Name"); // differs from existingApiAsset (null name) → triggers patch
            com.backbase.investment.api.service.v1.model.Asset existingApiAsset =
                new com.backbase.investment.api.service.v1.model.Asset()
                    .isin("ABC123")
                    .market("market")
                    .currency("USD");

            when(assetUniverseApi.listAssetCategories(isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(buildAssetCategoryPage(List.of())));
            when(assetUniverseApi.getAsset("ABC123_market_USD", null, null, null))
                .thenReturn(Mono.just(existingApiAsset));
            // patchAsset returns the desired stream asset; verify it is what gets emitted
            when(investmentRestAssetUniverseService.patchAsset(eq(existingApiAsset), eq(req), any()))
                .thenReturn(Mono.just(req));

            // Act & Assert — patched asset (req) is emitted
            StepVerifier.create(service.upsertAssets(List.of(req)))
                .expectNextMatches(a -> "ABC123".equals(a.getIsin())
                    && "market".equals(a.getMarket())
                    && "USD".equals(a.getCurrency()))
                .verifyComplete();

            verify(investmentRestAssetUniverseService).patchAsset(eq(existingApiAsset), eq(req), any());
            verify(investmentRestAssetUniverseService, never()).createAsset(any(), any());
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
     * Builds a {@link WebClientResponseException} for the given HTTP status code.
     */
    private WebClientResponseException serverError(int statusCode) {
        return WebClientResponseException.create(
            statusCode,
            HttpStatus.valueOf(statusCode).getReasonPhrase(),
            HttpHeaders.EMPTY,
            null,
            StandardCharsets.UTF_8
        );
    }

    /**
     * Builds a stream {@link com.backbase.stream.investment.Asset} with fixed ISIN, market and currency.
     */
    private com.backbase.stream.investment.Asset buildAsset() {
        return buildAsset("ABC123", "market", "USD");
    }

    /**
     * Builds a stream {@link com.backbase.stream.investment.Asset} with the given ISIN, market and currency.
     */
    private com.backbase.stream.investment.Asset buildAsset(String isin, String market, String currency) {
        com.backbase.stream.investment.Asset asset = new com.backbase.stream.investment.Asset();
        asset.setIsin(isin);
        asset.setMarket(market);
        asset.setCurrency(currency);
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

