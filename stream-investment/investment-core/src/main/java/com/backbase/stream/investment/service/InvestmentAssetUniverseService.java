package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.ApiClient;
import com.backbase.investment.api.service.v1.AssetUniverseApi;
import com.backbase.investment.api.service.v1.model.Asset;
import com.backbase.investment.api.service.v1.model.Market;
import com.backbase.investment.api.service.v1.model.MarketRequest;
import com.backbase.investment.api.service.v1.model.OASAssetRequestDataRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvestmentAssetUniverseService {

    private final AssetUniverseApi assetUniverseApi;
    private final ApiClient apiClient;

    /**
     * Gets an existing market by code, or creates it if not found (404).
     * Handles 404 NOT_FOUND from getMarket by returning Mono.empty(),
     * which triggers market creation via switchIfEmpty.
     *
     * @param marketRequest the market request details
     * @return Mono<Market> representing the existing or newly created market
     */
    public Mono<Market> getOrCreateMarket(MarketRequest marketRequest) {
        log.debug("Creating market: {}", marketRequest);
        return assetUniverseApi.getMarket(marketRequest.getCode())
                // If getMarket returns 404 NOT_FOUND, treat as "not found" and return Mono.empty()
                .onErrorResume(error -> {
                    if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException.NotFound) {
                        log.info("Market not found for code: {}", marketRequest.getCode());
                        return Mono.empty();
                    }
                    // Propagate other errors
                    return Mono.error(error);
                })
                // If market exists, return it
                .flatMap(existingMarket -> {
                    log.info("Market already exists: {}", existingMarket);
                    return Mono.just(existingMarket);
                })
                // If Mono is empty (market not found), create the market
                .switchIfEmpty(
                        assetUniverseApi.createMarket(marketRequest)
                                .doOnSuccess(createdMarket -> log.info("Created market: {}", createdMarket))
                                .doOnError(error -> log.error("Error creating market: {}", error.getMessage(), error))
                );
    }

    /**
     * Gets an existing asset by its identifier, or creates it if not found (404).
     * Handles 404 NOT_FOUND from getAsset by returning Mono.empty(),
     * which triggers asset creation via switchIfEmpty.
     *
     * @param assetRequest the asset request details
     * @return Mono<Asset> representing the existing or newly created asset
     * @throws IOException if an I/O error occurs
     */
    public Mono<Asset> getOrCreateAsset(final OASAssetRequestDataRequest assetRequest) throws IOException {
        log.debug("Creating asset: {}", assetRequest);

        // Build a unique asset identifier using ISIN, market, and currency
        final String assetIdentifier = assetRequest.getIsin() + "_" + assetRequest.getMarket() + "_" + assetRequest.getCurrency();

        // Try to fetch the asset by its identifier
        return assetUniverseApi.getAsset(assetIdentifier, null, null, null)
                // Handle 404 NOT_FOUND by returning Mono.empty() to trigger asset creation
                .onErrorResume(error -> {
                    if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException.NotFound) {
                        log.info("Asset not found with Asset Identifier : {}", assetIdentifier);
                        return Mono.empty();
                    }
                    // Propagate other errors
                    return Mono.error(error);
                })
                // If asset exists, log and return it
                .flatMap(existingAsset -> {
                    log.info("Asset already exists with Asset Identifier : {}", assetIdentifier);
                    return Mono.just(existingAsset);
                })
                // If Mono is empty (asset not found), create the asset
                .switchIfEmpty(
                        createAsset(assetRequest)
                                .doOnSuccess(createdAsset -> log.info("Created asset with assetIdentifier: {}", assetIdentifier))
                                .doOnError(error -> {
                                    if (error instanceof WebClientResponseException) {
                                        WebClientResponseException w = (WebClientResponseException) error;
                                        log.error("Error creating asset with assetIdentifier: {} : HTTP {} -> {}", assetIdentifier, w.getStatusCode(), w.getResponseBodyAsString());
                                    } else {
                                        log.error("Error creating asset with assetIdentifier: {} : {}", assetIdentifier, error.getMessage(), error);
                                    }
                                })
                );
    }

    /**
     * Creates a new asset by sending a POST request to the asset API.
     *
     * @param data the asset request payload
     * @return Mono<Asset> representing the created asset
     * @throws WebClientResponseException if the API call fails
     */
    public Mono<Asset> createAsset(OASAssetRequestDataRequest data) throws WebClientResponseException {
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = {
                "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {
                "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{};

        ParameterizedTypeReference<Asset> localVarReturnType = new ParameterizedTypeReference<Asset>() {
        };
        return apiClient.invokeAPI("/service-api/v2/asset/assets/", HttpMethod.POST, pathParams, queryParams, data,
                        headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames,
                        localVarReturnType)
                .bodyToMono(localVarReturnType);
    }

}
