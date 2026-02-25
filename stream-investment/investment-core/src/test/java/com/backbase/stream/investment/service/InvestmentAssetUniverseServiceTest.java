package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.v1.AssetUniverseApi;
import com.backbase.investment.api.service.v1.model.Asset;
import com.backbase.investment.api.service.v1.model.Market;
import com.backbase.investment.api.service.v1.model.MarketRequest;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestAssetUniverseService;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


/**
 * This is a custom implementation to avoid issues with Reactive and multipart requests.
 */
class InvestmentAssetUniverseServiceTest {

    InvestmentAssetUniverseService service;
    AssetUniverseApi assetUniverseApi;
    InvestmentRestAssetUniverseService investmentRestAssetUniverseService;

    @BeforeEach
    void setUp() {
        assetUniverseApi = Mockito.mock(AssetUniverseApi.class);
        investmentRestAssetUniverseService = Mockito.mock(InvestmentRestAssetUniverseService.class);
        service = new InvestmentAssetUniverseService(assetUniverseApi,
            investmentRestAssetUniverseService);
    }

    @Test
    void upsertMarket_marketExists() {
        MarketRequest request = new MarketRequest().code("US");
        Market market = new Market().code("US").name("Usa Market");
        Market marketUpdated = new Market().code("US").name("Usa Market Updated");
        Mockito.when(assetUniverseApi.getMarket("US")).thenReturn(Mono.just(market));
        Mockito.when(assetUniverseApi.createMarket(request)).thenReturn(Mono.just(market));
        Mockito.when(assetUniverseApi.updateMarket("US", request)).thenReturn(Mono.just(marketUpdated));

        StepVerifier.create(service.upsertMarket(request))
            .expectNext(marketUpdated)
            .verifyComplete();
    }

    @Test
    void upsertMarket_marketNotFound_createsMarket() {
        MarketRequest request = new MarketRequest().code("US");
        Market createdMarket = new Market().code("US");
        Mockito.when(assetUniverseApi.getMarket("US"))
            .thenReturn(Mono.error(WebClientResponseException.create(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                HttpHeaders.EMPTY,
                null,
                StandardCharsets.UTF_8
            )));
        Mockito.when(assetUniverseApi.createMarket(request)).thenReturn(Mono.just(createdMarket));

        StepVerifier.create(service.upsertMarket(request))
            .expectNext(createdMarket)
            .verifyComplete();
    }

    @Test
    void upsertMarket_otherError_propagates() {
        MarketRequest request = new MarketRequest().code("US");
        Mockito.when(assetUniverseApi.getMarket("US"))
            .thenReturn(Mono.error(new RuntimeException("API error")));
        Mockito.when(assetUniverseApi.createMarket(request)).thenReturn(Mono.empty());

        StepVerifier.create(service.upsertMarket(request))
            .expectErrorMatches(e -> e instanceof RuntimeException && e.getMessage().equals("API error"))
            .verify();
    }

    @Test
    void getOrCreateAsset_assetExists() {
        com.backbase.stream.investment.Asset req = createAsset();
        com.backbase.stream.investment.Asset asset = createAsset();
        Asset existingAsset = new Asset()
            .isin("ABC123")
            .market("market")
            .currency("USD");

//        req.setIsin("ABC123");
//        req.setMarket("market");
//        req.setCurrency("USD");

        Mockito.when(assetUniverseApi.getAsset(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any()))
            .thenReturn(Mono.just(existingAsset));
        Mockito.when(investmentRestAssetUniverseService.patchAsset(ArgumentMatchers.any(), ArgumentMatchers.any(),
                HashMap.newHashMap(1)))
            .thenReturn(Mono.just(asset));
        Mockito.when(investmentRestAssetUniverseService.createAsset(
                ArgumentMatchers.any(),
                ArgumentMatchers.any()))
            .thenReturn(Mono.just(asset)); // This won't be called, but needed for switchIfEmpty evaluation

        StepVerifier.create(service.getOrCreateAsset(req, null))
            .expectNextMatches(
                asset1 -> asset1.getIsin().equals(req.getIsin()) && asset1.getMarket().equals(req.getMarket())
                    && asset1.getCurrency()
                    .equals(req.getCurrency()))
            .verifyComplete();
    }

    private static com.backbase.stream.investment.Asset createAsset() {
        com.backbase.stream.investment.Asset req = new com.backbase.stream.investment.Asset();
        req.setIsin("ABC123");
        req.setMarket("market");
        req.setCurrency("USD");
        return req;
    }

    @Test
    void getOrCreateAsset_assetNotFound_createsAsset() {
        com.backbase.stream.investment.Asset req = createAsset();
        com.backbase.stream.investment.Asset createdAsset = createAsset();
        String assetId = "ABC123_market_USD";

        Mockito.when(assetUniverseApi.getAsset(
                ArgumentMatchers.eq(assetId),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.isNull()))
            .thenReturn(Mono.error(WebClientResponseException.create(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                HttpHeaders.EMPTY,
                null,
                StandardCharsets.UTF_8
            )));
        Mockito.when(investmentRestAssetUniverseService.createAsset(
                ArgumentMatchers.eq(req),
                ArgumentMatchers.eq(Map.of())))
            .thenReturn(Mono.just(createdAsset));

        StepVerifier.create(service.getOrCreateAsset(req, Map.of()))
            .expectNext(createdAsset)
            .verifyComplete();
    }

    @Test
    void getOrCreateAsset_otherError_propagates() {
        com.backbase.stream.investment.Asset req = createAsset();
        String assetId = "ABC123_market_USD";
        Mockito.when(assetUniverseApi.getAsset(
                ArgumentMatchers.eq(assetId),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.isNull()))
            .thenReturn(Mono.error(new RuntimeException("API error")));
        Mockito.when(investmentRestAssetUniverseService.createAsset(
                ArgumentMatchers.eq(req),
                ArgumentMatchers.isNull()))
            .thenReturn(Mono.empty()); // This won't be called, but needed for switchIfEmpty evaluation

        StepVerifier.create(service.getOrCreateAsset(req, null))
            .expectErrorMatches(e -> e instanceof RuntimeException && e.getMessage().equals("API error"))
            .verify();
    }

    @Test
    void getOrCreateAsset_createAssetFails_propagates() {
        com.backbase.stream.investment.Asset req = createAsset();
        String assetId = "ABC123_market_USD";
        Mockito.when(assetUniverseApi.getAsset(
                ArgumentMatchers.eq(assetId),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.isNull()))
            .thenReturn(Mono.error(WebClientResponseException.create(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                HttpHeaders.EMPTY,
                null,
                StandardCharsets.UTF_8
            )));
        Mockito.when(investmentRestAssetUniverseService.createAsset(
                ArgumentMatchers.eq(req),
                ArgumentMatchers.isNull()))
            .thenReturn(Mono.error(new RuntimeException("Create asset failed")));

        StepVerifier.create(service.getOrCreateAsset(req, null))
            .expectErrorMatches(e -> e instanceof RuntimeException && e.getMessage().equals("Create asset failed"))
            .verify();
    }

    @Test
    void getOrCreateAsset_nullRequest_returnsError() {
        StepVerifier.create(Mono.defer(() -> service.getOrCreateAsset(null, null)))
            .expectError(NullPointerException.class)
            .verify();
    }

    @Test
    void getOrCreateAsset_emptyMonoFromCreateAsset() {
        com.backbase.stream.investment.Asset req = createAsset();
        String assetId = "ABC123_market_USD";
        Mockito.when(assetUniverseApi.getAsset(
                ArgumentMatchers.eq(assetId),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.isNull()))
            .thenReturn(Mono.error(WebClientResponseException.create(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                HttpHeaders.EMPTY,
                null,
                StandardCharsets.UTF_8
            )));
        Mockito.when(investmentRestAssetUniverseService.createAsset(
                ArgumentMatchers.eq(req),
                ArgumentMatchers.isNull()))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.getOrCreateAsset(req, null))
            .expectComplete()
            .verify();
    }
}