package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.ApiClient;
import com.backbase.investment.api.service.v1.AssetUniverseApi;
import com.backbase.investment.api.service.v1.model.Asset;
import com.backbase.investment.api.service.v1.model.Market;
import com.backbase.investment.api.service.v1.model.MarketRequest;
import com.backbase.investment.api.service.v1.model.OASAssetRequestDataRequest;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * This is a custom implementations to avoid issues with Reactive and multipart requests
 */
class InvestmentAssetUniverseServiceTest {

    InvestmentAssetUniverseService service;
    AssetUniverseApi assetUniverseApi;
    ApiClient apiClient;
    CustomIntegrationApiService customIntegrationApiService;

    @BeforeEach
    void setUp() {
        assetUniverseApi = Mockito.mock(AssetUniverseApi.class);
        apiClient = Mockito.mock(ApiClient.class);
        customIntegrationApiService = Mockito.spy(new CustomIntegrationApiService(apiClient));
        service = new InvestmentAssetUniverseService(assetUniverseApi, customIntegrationApiService);
    }

    @Test
    void getOrCreateMarket_marketExists() {
        MarketRequest request = new MarketRequest().code("US");
        Market market = new Market().code("US");
        Mockito.when(assetUniverseApi.getMarket("US")).thenReturn(Mono.just(market));
        Mockito.when(assetUniverseApi.createMarket(request)).thenReturn(Mono.empty());

        StepVerifier.create(service.getOrCreateMarket(request))
            .expectNext(market)
            .verifyComplete();
    }

    @Test
    void getOrCreateMarket_marketNotFound_createsMarket() {
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

        StepVerifier.create(service.getOrCreateMarket(request))
            .expectNext(createdMarket)
            .verifyComplete();
    }

    @Test
    void getOrCreateMarket_otherError_propagates() {
        MarketRequest request = new MarketRequest().code("US");
        Mockito.when(assetUniverseApi.getMarket("US"))
            .thenReturn(Mono.error(new RuntimeException("API error")));
        Mockito.when(assetUniverseApi.createMarket(request)).thenReturn(Mono.empty());

        StepVerifier.create(service.getOrCreateMarket(request))
            .expectErrorMatches(e -> e instanceof RuntimeException && e.getMessage().equals("API error"))
            .verify();
    }

    @Test
    void getOrCreateAsset_assetExists() throws IOException {
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

        StepVerifier.create(service.getOrCreateAsset(req))
            .expectNext(asset)
            .verifyComplete();
    }

    @Test
    void getOrCreateAsset_assetNotFound_createsAsset() throws IOException {
        OASAssetRequestDataRequest req = new OASAssetRequestDataRequest()
            .isin("ABC123").market("US").currency("USD");
        Asset createdAsset = new Asset().isin("ABC123");
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
            .thenReturn(Mono.just(createdAsset));

        StepVerifier.create(service.getOrCreateAsset(req))
            .expectNext(createdAsset)
            .verifyComplete();
    }

    @Test
    void getOrCreateAsset_otherError_propagates() throws IOException {
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

        StepVerifier.create(service.getOrCreateAsset(req))
            .expectErrorMatches(e -> e instanceof RuntimeException && e.getMessage().equals("API error"))
            .verify();
    }

    @Test
    void getOrCreateAsset_createAssetFails_propagates() throws IOException {
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

        StepVerifier.create(service.getOrCreateAsset(req))
            .expectErrorMatches(e -> e instanceof RuntimeException && e.getMessage().equals("Create asset failed"))
            .verify();
    }

    @Test
    void getOrCreateAsset_nullRequest_returnsError() {
        StepVerifier.create(Mono.defer(() -> {
                return service.getOrCreateAsset(null);
            }))
            .expectError(NullPointerException.class)
            .verify();
    }

    @Test
    void getOrCreateAsset_emptyMonoFromCreateAsset() throws IOException {
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

        StepVerifier.create(service.getOrCreateAsset(req))
            .expectComplete()
            .verify();
    }
}