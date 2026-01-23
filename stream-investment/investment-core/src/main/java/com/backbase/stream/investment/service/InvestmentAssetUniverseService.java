package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.v1.AssetUniverseApi;
import com.backbase.investment.api.service.v1.model.Asset;
import com.backbase.investment.api.service.v1.model.AssetCategory;
import com.backbase.investment.api.service.v1.model.AssetCategoryRequest;
import com.backbase.investment.api.service.v1.model.AssetCategoryType;
import com.backbase.investment.api.service.v1.model.AssetCategoryTypeRequest;
import com.backbase.investment.api.service.v1.model.Market;
import com.backbase.investment.api.service.v1.model.MarketRequest;
import com.backbase.investment.api.service.v1.model.MarketSpecialDay;
import com.backbase.investment.api.service.v1.model.MarketSpecialDayRequest;
import com.backbase.investment.api.service.v1.model.OASAssetRequestDataRequest;
import com.backbase.investment.api.service.v1.model.PaginatedAssetCategoryList;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class InvestmentAssetUniverseService {

    private final AssetUniverseApi assetUniverseApi;
    private final CustomIntegrationApiService customIntegrationApiService;
    private final AssetMapper assetMapper = Mappers.getMapper(AssetMapper.class);

    /**
     * Gets an existing market by code, or creates it if not found (404). Handles 404 NOT_FOUND from getMarket by
     * returning Mono.empty(), which triggers market creation via switchIfEmpty.
     *
     * @param marketRequest the market request details
     * @return Mono<Market> representing the existing or newly created market
     */
    public Mono<Market> upsertMarket(MarketRequest marketRequest) {
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
                return assetUniverseApi.updateMarket(existingMarket.getCode(), marketRequest)
                    .doOnSuccess(updatedMarket -> log.info("Updated market: {}", updatedMarket))
                    .doOnError(error -> {
                        if (error instanceof WebClientResponseException w) {
                            log.error("Error updating market: {} : HTTP {} -> {}", marketRequest.getCode(),
                                w.getStatusCode(), w.getResponseBodyAsString());
                        } else {
                            log.error("Error updating market: {} : {}", marketRequest.getCode(),
                                error.getMessage(), error);
                        }
                    });
            })
            // If Mono is empty (market not found), create the market
            .switchIfEmpty(assetUniverseApi.createMarket(marketRequest)
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
    public Mono<Asset> getOrCreateAsset(OASAssetRequestDataRequest assetRequest) {
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
            .switchIfEmpty(customIntegrationApiService.createAsset(assetRequest)
                .doOnSuccess(createdAsset -> log.info("Created asset with assetIdentifier: {}", assetIdentifier))
                .doOnError(error -> {
                    if (error instanceof WebClientResponseException w) {
                        log.error("Error creating asset with assetIdentifier: {} : HTTP {} -> {}", assetIdentifier,
                            w.getStatusCode(), w.getResponseBodyAsString());
                    } else {
                        log.error("Error creating asset with assetIdentifier: {} : {}", assetIdentifier,
                            error.getMessage(), error);
                    }
                })
            );
    }

    /**
     * Gets an existing market special day by date and market, or creates it if not found. Handles 404 or empty results
     * by creating the market special day.
     *
     * @param marketSpecialDayRequest the request containing market and date details
     * @return Mono\<MarketSpecialDay\> representing the existing or newly created market special day
     */
    public Mono<MarketSpecialDay> upsertMarketSpecialDay(MarketSpecialDayRequest marketSpecialDayRequest) {
        log.debug("Creating market special day: {}", marketSpecialDayRequest);
        LocalDate date = marketSpecialDayRequest.getDate();

        // Fetch market special days for the given date
        return assetUniverseApi.listMarketSpecialDay(date, date, 100, 0)
            .flatMap(paginatedMarketSpecialDayList -> {
                List<MarketSpecialDay> marketSpecialDayList = paginatedMarketSpecialDayList.getResults();

                // If no special days exist, return empty to trigger creation
                if (marketSpecialDayList.isEmpty()) {
                    log.debug("No market special day exists for day: {}", marketSpecialDayRequest);
                    return Mono.empty();
                } else {
                    // Find a matching special day for the requested market
                    Optional<MarketSpecialDay> matchingSpecialDay = marketSpecialDayList.stream()
                        .filter(msd -> marketSpecialDayRequest.getMarket().equals(msd.getMarket()))
                        .findFirst();
                    if (matchingSpecialDay.isPresent()) {
                        log.info("Market special day already exists for day: {}", marketSpecialDayRequest);
                        return assetUniverseApi.updateMarketSpecialDay(matchingSpecialDay.get().getUuid().toString(),
                                marketSpecialDayRequest)
                            .doOnSuccess(updatedMarketSpecialDay ->
                                log.info("Updated market special day: {}", updatedMarketSpecialDay))
                            .doOnError(error -> {
                                if (error instanceof WebClientResponseException w) {
                                    log.error("Error updating market special day : {} : HTTP {} -> {}",
                                        marketSpecialDayRequest,
                                        w.getStatusCode(), w.getResponseBodyAsString());
                                } else {
                                    log.error("Error updating market special day {} : {}", marketSpecialDayRequest,
                                        error.getMessage(), error);
                                }
                            });
                    } else {
                        log.debug("No market special day exists for day: {}", marketSpecialDayRequest);
                        return Mono.empty();
                    }

                }
            })
            // If Mono is empty (market special day not found), create the market special day
            .switchIfEmpty(assetUniverseApi.createMarketSpecialDay(marketSpecialDayRequest)
                .doOnSuccess(
                    createdMarketSpecialDay -> log.info("Created market special day: {}", createdMarketSpecialDay))
                .doOnError(error -> {
                    if (error instanceof WebClientResponseException) {
                        WebClientResponseException w = (WebClientResponseException) error;
                        log.error("Error creating market special day : {} : HTTP {} -> {}", marketSpecialDayRequest,
                            w.getStatusCode(), w.getResponseBodyAsString());
                    } else {
                        log.error("Error creating market special day {} : {}", marketSpecialDayRequest,
                            error.getMessage(), error);
                    }

                })
            );
    }

    public Flux<com.backbase.stream.investment.Asset> createAssets(List<com.backbase.stream.investment.Asset> assets) {
        if (CollectionUtils.isEmpty(assets)) {
            return Flux.empty();
        }
        return assetUniverseApi.listAssetCategories(null,
                null, null, null, null, null)
            .filter(Objects::nonNull)
            .map(PaginatedAssetCategoryList::getResults)
            .filter(Objects::nonNull)
            .flatMapMany(categories -> {
                Map<String, UUID> categoryIdByCode = categories.stream()
                    .collect(Collectors.toMap(AssetCategory::getCode, AssetCategory::getUuid));

                return Flux.fromIterable(assets)
                    .flatMap(asset -> {
                        OASAssetRequestDataRequest assetRequest = assetMapper.map(asset, categoryIdByCode);
                        return this.getOrCreateAsset(assetRequest).map(assetMapper::map);
                    });
            });
    }

    /**
     * Gets an existing asset category by its code, or creates it if not found. Handles empty results by creating the
     * asset category.
     *
     * @param assetCategoryRequest the request containing asset category details
     * @return Mono<AssetCategory> representing the existing or newly created asset category
     */
    public Mono<AssetCategory> upsertAssetCategory(AssetCategoryRequest assetCategoryRequest) {
        if (assetCategoryRequest == null) {
            return Mono.empty();
        }
        return assetUniverseApi.listAssetCategories(assetCategoryRequest.getCode(), 100,
                assetCategoryRequest.getName(), 0, assetCategoryRequest.getOrder(), assetCategoryRequest.getType())
            .flatMap(paginatedAssetCategoryList -> {
                List<AssetCategory> assetCategoryList = paginatedAssetCategoryList.getResults();
                if (assetCategoryList == null || assetCategoryList.isEmpty()) {
                    log.debug("No asset category exists for code: {}", assetCategoryRequest.getCode());
                    return Mono.empty();
                } else {
                    Optional<AssetCategory> matchingCategory = assetCategoryList.stream()
                        .filter(ac -> assetCategoryRequest.getCode().equals(ac.getCode()))
                        .findFirst();
                    if (matchingCategory.isPresent()) {
                        log.info("Asset category already exists for code: {}", assetCategoryRequest.getCode());
                        return assetUniverseApi.updateAssetCategory(matchingCategory.get().getUuid().toString(), assetCategoryRequest)
                            .doOnSuccess(updatedCategory -> log.info("Updated asset category: {}", updatedCategory))
                            .doOnError(error -> {
                                if (error instanceof WebClientResponseException w) {
                                    log.error("Error updating asset category: {} : HTTP {} -> {}", assetCategoryRequest.getCode(),
                                        w.getStatusCode(), w.getResponseBodyAsString());
                                } else {
                                    log.error("Error updating asset category: {} : {}", assetCategoryRequest.getCode(),
                                        error.getMessage(), error);
                                }
                            })
                            .onErrorResume(e -> Mono.empty());
                    } else {
                        log.debug("No asset category exists for code: {}", assetCategoryRequest.getCode());
                        return Mono.empty();
                    }
                }
            })
            .switchIfEmpty(
                assetUniverseApi.createAssetCategory(assetCategoryRequest)
                    .doOnSuccess(createdCategory -> log.info("Created asset category : {}", createdCategory))
                    .doOnError(error -> {
                        if (error instanceof WebClientResponseException w) {
                            log.error("Error creating asset category: {} : HTTP {} -> {}",
                                assetCategoryRequest.getCode(),
                                w.getStatusCode(), w.getResponseBodyAsString());
                        } else {
                            log.error("Error creating asset category: {} : {}", assetCategoryRequest.getCode(),
                                error.getMessage(), error);
                        }
                    })
                    .onErrorResume(e -> Mono.empty())
            );
    }

    /**
     * Gets an existing asset category type by its code, or creates it if not found. Handles 404 or empty results by
     * creating the asset category type.
     *
     * @param assetCategoryTypeRequest the request containing asset category type details
     * @return Mono<AssetCategoryType> representing the existing or newly created asset category type
     */
    public Mono<AssetCategoryType> upsertAssetCategoryType(AssetCategoryTypeRequest assetCategoryTypeRequest) {
        if (assetCategoryTypeRequest == null) {
            return Mono.empty();
        }
        return assetUniverseApi.listAssetCategoryTypes(assetCategoryTypeRequest.getCode(), 100,
                assetCategoryTypeRequest.getName(), 0)
            .flatMap(paginatedAssetCategoryTypeList -> {
                List<AssetCategoryType> assetCategoryTypeList = paginatedAssetCategoryTypeList.getResults();
                if (assetCategoryTypeList == null || assetCategoryTypeList.isEmpty()) {
                    log.debug("No asset category type exists for code: {}", assetCategoryTypeRequest.getCode());
                    return Mono.empty();
                } else {
                    Optional<AssetCategoryType> matchingType = assetCategoryTypeList.stream()
                        .filter(act -> assetCategoryTypeRequest.getCode().equals(act.getCode()))
                        .findFirst();
                    if (matchingType.isPresent()) {
                        log.info("Asset category type already exists for code: {}", assetCategoryTypeRequest.getCode());
                        return assetUniverseApi.updateAssetCategoryType(matchingType.get().getUuid().toString(), assetCategoryTypeRequest)
                            .doOnSuccess(updatedType -> log.info("Updated asset category type: {}", updatedType))
                            .doOnError(error -> {
                                if (error instanceof WebClientResponseException w) {
                                    log.error("Error updating asset category type: {} : HTTP {} -> {}",
                                        assetCategoryTypeRequest.getCode(), w.getStatusCode(), w.getResponseBodyAsString());
                                } else {
                                    log.error("Error updating asset category type: {} : {}",
                                        assetCategoryTypeRequest.getCode(), error.getMessage(), error);
                                }
                            })
                            .onErrorResume(e -> Mono.empty());
                    } else {
                        log.debug("No asset category type exists for code: {}", assetCategoryTypeRequest.getCode());
                        return Mono.empty();
                    }
                }
            })
            .switchIfEmpty(
                assetUniverseApi.createAssetCategoryType(assetCategoryTypeRequest)
                    .doOnSuccess(createdType -> log.info("Created asset category type: {}", createdType))
                    .doOnError(error -> {
                        if (error instanceof WebClientResponseException w) {
                            log.error("Error creating asset category type: {} : HTTP {} -> {}",
                                assetCategoryTypeRequest.getCode(),
                                w.getStatusCode(), w.getResponseBodyAsString());
                        } else {
                            log.error("Error creating asset category type: {} : {}", assetCategoryTypeRequest.getCode(),
                                error.getMessage(), error);
                        }
                    })
            );
    }
}