package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.ApiClient;
import com.backbase.investment.api.service.v1.AssetUniverseApi;
import com.backbase.investment.api.service.v1.model.Asset;
import com.backbase.investment.api.service.v1.model.Market;
import com.backbase.investment.api.service.v1.model.MarketRequest;
import com.backbase.investment.api.service.v1.model.OASAssetRequestDataRequest;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestAssetUniverseService;
import java.io.File;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


/**
 * This is a custom implementation to avoid issues with Reactive and multipart requests.
 */
class InvestmentAssetUniverseServiceTest {

    InvestmentAssetUniverseService service;
    AssetUniverseApi assetUniverseApi;
    ApiClient apiClient;
    InvestmentRestAssetUniverseService investmentRestAssetUniverseService;
    CustomIntegrationApiService customIntegrationApiService;

    @BeforeEach
    void setUp() {
        assetUniverseApi = Mockito.mock(AssetUniverseApi.class);
        apiClient = Mockito.mock(ApiClient.class);
        investmentRestAssetUniverseService = Mockito.mock(InvestmentRestAssetUniverseService.class);
        customIntegrationApiService = Mockito.mock(CustomIntegrationApiService.class);
        service = new InvestmentAssetUniverseService(assetUniverseApi,
            investmentRestAssetUniverseService, customIntegrationApiService);
    }

    @Test
    void upsertMarket_marketExists() {
        MarketRequest request = new MarketRequest().code("US");
        Market market = new Market().code("US").name("Usa Market");
        Market marketUpdated = new Market().code("US").name("Usa Market Updated");
        Mockito.when(assetUniverseApi.getMarket("US")).thenReturn(Mono.just(market));
        Mockito.when(assetUniverseApi.createMarket(request)).thenReturn(Mono.just(market));
        Mockito.when(assetUniverseApi.updateMarket("US",request)).thenReturn(Mono.just(marketUpdated));

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
        OASAssetRequestDataRequest req = new OASAssetRequestDataRequest()
            .isin("ABC123").market("US").currency("USD");
        Asset asset = new Asset().isin("ABC123");
        String assetId = "ABC123_US_USD";
        Mockito.when(assetUniverseApi.getAsset(assetId, null, null, null)).thenReturn(Mono.just(asset));

        WebClient.ResponseSpec responseSpec = Mockito.mock(WebClient.ResponseSpec.class);
        Mockito.when(apiClient.invokeAPI(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.eq(HttpMethod.POST),
            ArgumentMatchers.anyMap(),
            ArgumentMatchers.any(),
            ArgumentMatchers.eq(req),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(ParameterizedTypeReference.class)
        )).thenReturn(responseSpec);
        Mockito.when(responseSpec.bodyToMono(ArgumentMatchers.any(ParameterizedTypeReference.class)))
            .thenReturn(Mono.just(asset));

        StepVerifier.create(service.getOrCreateAsset(req, null))
            .expectNext(asset)
            .verifyComplete();
    }

    @Test
    void getOrCreateAsset_assetNotFound_createsAsset() {
        OASAssetRequestDataRequest req = new OASAssetRequestDataRequest()
            .isin("ABC123").market("US").currency("USD");
        Asset createdAsset = new Asset().isin("ABC123");
        String assetId = "ABC123_US_USD";
        File logo = null;

        Mockito.when(assetUniverseApi.getAsset(assetId, null, null, null))
            .thenReturn(Mono.error(WebClientResponseException.create(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                HttpHeaders.EMPTY,
                null,
                StandardCharsets.UTF_8
            )));
        WebClient.ResponseSpec responseSpec = Mockito.mock(WebClient.ResponseSpec.class);
        Mockito.when(apiClient.invokeAPI(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.eq(HttpMethod.POST),
            ArgumentMatchers.anyMap(),
            ArgumentMatchers.any(),
            ArgumentMatchers.eq(req),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(ParameterizedTypeReference.class)
        )).thenReturn(responseSpec);
        Mockito.when(responseSpec.bodyToMono(ArgumentMatchers.any(ParameterizedTypeReference.class)))
            .thenReturn(Mono.just(createdAsset));

        StepVerifier.create(service.getOrCreateAsset(req, logo))
            .expectNext(createdAsset)
            .verifyComplete();
    }

    @Test
    void getOrCreateAsset_otherError_propagates() {
        OASAssetRequestDataRequest req = new OASAssetRequestDataRequest()
            .isin("ABC123").market("US").currency("USD");
        String assetId = "ABC123_US_USD";
        Mockito.when(assetUniverseApi.getAsset(assetId, null, null, null))
            .thenReturn(Mono.error(new RuntimeException("API error")));

        WebClient.ResponseSpec responseSpec = Mockito.mock(WebClient.ResponseSpec.class);
        Mockito.when(apiClient.invokeAPI(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.eq(HttpMethod.POST),
            ArgumentMatchers.anyMap(),
            ArgumentMatchers.any(),
            ArgumentMatchers.eq(req),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(ParameterizedTypeReference.class)
        )).thenReturn(responseSpec);
        Mockito.when(responseSpec.bodyToMono(ArgumentMatchers.any(ParameterizedTypeReference.class)))
            .thenReturn(Mono.error(new RuntimeException("API error")));

        StepVerifier.create(service.getOrCreateAsset(req, null))
            .expectErrorMatches(e -> e instanceof RuntimeException && e.getMessage().equals("API error"))
            .verify();
    }

    @Test
    void getOrCreateAsset_createAssetFails_propagates() {
        OASAssetRequestDataRequest req = new OASAssetRequestDataRequest()
            .isin("ABC123").market("US").currency("USD");
        String assetId = "ABC123_US_USD";
        Mockito.when(assetUniverseApi.getAsset(assetId, null, null, null))
            .thenReturn(Mono.error(WebClientResponseException.create(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                HttpHeaders.EMPTY,
                null,
                StandardCharsets.UTF_8
            )));
        WebClient.ResponseSpec responseSpec = Mockito.mock(WebClient.ResponseSpec.class);
        Mockito.when(apiClient.invokeAPI(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.eq(HttpMethod.POST),
            ArgumentMatchers.anyMap(),
            ArgumentMatchers.any(),
            ArgumentMatchers.eq(req),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(ParameterizedTypeReference.class)
        )).thenReturn(responseSpec);
        Mockito.when(responseSpec.bodyToMono(ArgumentMatchers.any(ParameterizedTypeReference.class)))
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
        OASAssetRequestDataRequest req = new OASAssetRequestDataRequest()
            .isin("ABC123").market("US").currency("USD");
        String assetId = "ABC123_US_USD";
        Mockito.when(assetUniverseApi.getAsset(assetId, null, null, null))
            .thenReturn(Mono.error(WebClientResponseException.create(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                HttpHeaders.EMPTY,
                null,
                StandardCharsets.UTF_8
            )));
        WebClient.ResponseSpec responseSpec = Mockito.mock(WebClient.ResponseSpec.class);
        Mockito.when(apiClient.invokeAPI(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.eq(HttpMethod.POST),
            ArgumentMatchers.anyMap(),
            ArgumentMatchers.any(),
            ArgumentMatchers.eq(req),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(ParameterizedTypeReference.class)
        )).thenReturn(responseSpec);
        Mockito.when(responseSpec.bodyToMono(ArgumentMatchers.any(ParameterizedTypeReference.class)))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.getOrCreateAsset(req, null))
            .expectComplete()
            .verify();
    }
}