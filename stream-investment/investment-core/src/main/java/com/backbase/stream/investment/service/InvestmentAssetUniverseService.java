package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.v1.AssetUniverseApi;
import com.backbase.investment.api.service.v1.model.Asset;
import com.backbase.investment.api.service.v1.model.Market;
import com.backbase.investment.api.service.v1.model.MarketRequest;
import com.backbase.investment.api.service.v1.model.OASAssetRequestDataRequest;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class InvestmentAssetUniverseService {

    private final AssetUniverseApi assetUniverseApi;
    private final CustomIntegrationApiService customIntegrationApiService;

    /**
     * Gets an existing market by code, or creates it if not found (404). Handles 404 NOT_FOUND from getMarket by
     * returning Mono.empty(), which triggers market creation via switchIfEmpty.
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
                log.info("Market already exists: {}", existingMarket.getCode());
                log.debug("Market already exists: {}", existingMarket);
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
     * Gets an existing asset by its identifier, or creates it if not found (404). Handles 404 NOT_FOUND from getAsset
     * by returning Mono.empty(), which triggers asset creation via switchIfEmpty.
     *
     * @param assetRequest the asset request details
     * @return Mono<Asset> representing the existing or newly created asset
     * @throws IOException if an I/O error occurs
     */
    public Mono<Asset> getOrCreateAsset(final OASAssetRequestDataRequest assetRequest) throws IOException {
        log.debug("Creating asset: {}", assetRequest);

        // Build a unique asset identifier using ISIN, market, and currency
        final String assetIdentifier =
            assetRequest.getIsin() + "_" + assetRequest.getMarket() + "_" + assetRequest.getCurrency();

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
                customIntegrationApiService.createAsset(assetRequest)
                    .doOnSuccess(createdAsset -> log.info("Created asset with assetIdentifier: {}", assetIdentifier))
                    .doOnError(error -> {
                        if (error instanceof WebClientResponseException) {
                            WebClientResponseException w = (WebClientResponseException) error;
                            log.error("Error creating asset with assetIdentifier: {} : HTTP {} -> {}", assetIdentifier,
                                w.getStatusCode(), w.getResponseBodyAsString());
                        } else {
                            log.error("Error creating asset with assetIdentifier: {} : {}", assetIdentifier,
                                error.getMessage(), error);
                        }
                    })
            );
    }

}