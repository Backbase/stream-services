package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.sync.v1.model.AssetCategory;
import com.backbase.investment.api.service.v1.AssetUniverseApi;
import com.backbase.investment.api.service.v1.model.AssetCategoryType;
import com.backbase.investment.api.service.v1.model.AssetCategoryTypeRequest;
import com.backbase.investment.api.service.v1.model.Market;
import com.backbase.investment.api.service.v1.model.MarketRequest;
import com.backbase.investment.api.service.v1.model.MarketSpecialDay;
import com.backbase.investment.api.service.v1.model.MarketSpecialDayRequest;
import com.backbase.investment.api.service.v1.model.PaginatedAssetCategoryList;
import com.backbase.stream.investment.model.AssetCategoryEntry;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestAssetUniverseService;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Slf4j
@RequiredArgsConstructor
public class InvestmentAssetUniverseService {

    private final AssetUniverseApi assetUniverseApi;
    private final InvestmentRestAssetUniverseService investmentRestAssetUniverseService;
    private final AssetMapper assetMapper = Mappers.getMapper(AssetMapper.class);

    /**
     * Upserts a market by code: updates if changed, creates if not found.
     *
     * @param marketRequest the market details
     * @return the existing, updated, or newly created {@link Market}
     */
    public Mono<Market> upsertMarket(MarketRequest marketRequest) {
        log.debug("Creating market: {}", marketRequest);
        return assetUniverseApi.getMarket(marketRequest.getCode())
            .onErrorResume(error -> {
                if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException.NotFound) {
                    log.info("Market not found for code: {}", marketRequest.getCode());
                    return Mono.empty();
                }
                return Mono.error(error);
            })
            .flatMap(existingMarket -> {
                log.info("Market already exists: code={}", existingMarket.getCode());
                // Skip the update if the incoming request carries the same data as what is already stored.
                if (isMarketSame(marketRequest, existingMarket)) {
                    log.info("Skipping market update - no changes detected for code: {}", existingMarket.getCode());
                    return Mono.just(existingMarket);
                }
                return assetUniverseApi.updateMarket(existingMarket.getCode(), marketRequest)
                    .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                        .filter(this::isRetryableError)
                        .doBeforeRetry(signal -> log.warn("Retrying market update: code={}, attempt={}",
                            marketRequest.getCode(), signal.totalRetries() + 1)))
                    .doOnSuccess(updatedMarket -> log.info("Updated market: code={}", updatedMarket.getCode()))
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
            .switchIfEmpty(Mono.defer(() -> assetUniverseApi.createMarket(marketRequest)
                .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                    .filter(this::isRetryableError)
                    .doBeforeRetry(signal -> log.warn("Retrying market create: code={}, attempt={}",
                        marketRequest.getCode(), signal.totalRetries() + 1)))
                .doOnSuccess(createdMarket -> log.info("Created market: code={}", createdMarket.getCode()))
                .doOnError(error -> log.error("Error creating market: {}", error.getMessage(), error))
            ));
    }

    /**
     * Upserts an asset by identifier: patches if changed, creates if not found.
     *
     * @param asset            the desired asset state
     * @param categoryIdByCode category UUID lookup map keyed by code
     * @return the existing, patched, or newly created asset
     */
    public Mono<com.backbase.stream.investment.Asset> upsertAsset(com.backbase.stream.investment.Asset asset,
        Map<String, UUID> categoryIdByCode) {
        log.debug("Creating asset: {}", asset);

        // Build a unique asset identifier using ISIN, market, and currency
        String assetIdentifier = asset.getKeyString();

        // Try to fetch the asset by its identifier
        return assetUniverseApi.getAsset(assetIdentifier, null, null, null)
            // Handle 404 NOT_FOUND by returning Mono.empty() to trigger asset creation
            .onErrorResume(error -> {
                if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException.NotFound) {
                    log.info("Asset not found: assetIdentifier={}", assetIdentifier);
                    return Mono.empty();
                }
                // Propagate other errors
                return Mono.error(error);
            })
            // If asset exists, compare mapped fields and only patch when something changed
            .flatMap(a -> {
                log.info("Asset already exists: assetIdentifier={}", assetIdentifier);
                // Map existing API asset; logo (URI vs String) and categories (order-insensitive)
                // are checked separately inside isAssetSame().
                com.backbase.stream.investment.Asset existingMapped = assetMapper.map(a);
                if (isAssetSame(asset, existingMapped, a.getLogo())) {
                    log.info("Skipping asset patch - no changes detected for assetIdentifier: {}", assetIdentifier);
                    return Mono.just(existingMapped);
                }
                log.info("Asset changed for assetIdentifier: {}", assetIdentifier);
                // Stamp the server UUID back onto the patched asset so callers always get a non-null key.
                return investmentRestAssetUniverseService.patchAsset(a, asset, categoryIdByCode)
                    .map(patchedAsset -> patchedAsset.toBuilder().uuid(a.getUuid()).build());
            })
            // If Mono is empty (asset not found), create the asset
            .switchIfEmpty(Mono.defer(() -> investmentRestAssetUniverseService.createAsset(asset, categoryIdByCode)
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
            ));
    }

    /**
     * Upserts a market special day: updates if changed, creates if not found.
     *
     * @param marketSpecialDayRequest the market and date details
     * @return the existing, updated, or newly created {@link MarketSpecialDay}
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
                        log.info("Market special day already exists: date={}, market={}",
                            date, marketSpecialDayRequest.getMarket());
                        MarketSpecialDay existing = matchingSpecialDay.get();
                        if (isMarketSpecialDaySame(marketSpecialDayRequest, existing)) {
                            log.info("Skipping market special day update - no changes detected for date: {}, market: {}",
                                date, marketSpecialDayRequest.getMarket());
                            return Mono.just(existing);
                        }
                        return assetUniverseApi.updateMarketSpecialDay(existing.getUuid().toString(),
                                marketSpecialDayRequest)
                            .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                                .filter(this::isRetryableError)
                                .doBeforeRetry(signal -> log.warn(
                                    "Retrying market special day update: date={}, attempt={}",
                                    date, signal.totalRetries() + 1)))
                            .doOnSuccess(updatedMarketSpecialDay ->
                                log.info("Updated market special day: date={}, market={}",
                                    date, marketSpecialDayRequest.getMarket()))
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
            .switchIfEmpty(Mono.defer(() -> assetUniverseApi.createMarketSpecialDay(marketSpecialDayRequest)
                .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                    .filter(this::isRetryableError)
                    .doBeforeRetry(signal -> log.warn(
                        "Retrying market special day create: date={}, attempt={}",
                        marketSpecialDayRequest.getDate(), signal.totalRetries() + 1)))
                .doOnSuccess(createdMarketSpecialDay -> log.info("Created market special day: date={}, market={}",
                    marketSpecialDayRequest.getDate(), marketSpecialDayRequest.getMarket()))
                .doOnError(error -> {
                    if (error instanceof WebClientResponseException w) {
                        log.error("Error creating market special day : {} : HTTP {} -> {}", marketSpecialDayRequest,
                            w.getStatusCode(), w.getResponseBodyAsString());
                    } else {
                        log.error("Error creating market special day {} : {}", marketSpecialDayRequest,
                            error.getMessage(), error);
                    }

                })
            ));
    }

    /**
     * Upserts a list of assets: patches each if changed, creates if not found.
     * Deduplicates by key before processing and limits concurrency to 5.
     *
     * @param assets the desired asset states
     * @return the existing, patched, or newly created assets
     */
    public Flux<com.backbase.stream.investment.Asset> upsertAssets(List<com.backbase.stream.investment.Asset> assets) {
        if (CollectionUtils.isEmpty(assets)) {
            return Flux.empty();
        }
        return assetUniverseApi.listAssetCategories(null,
                null, null, null, null, null)
            .filter(Objects::nonNull)
            .flatMap(page -> Mono.justOrEmpty(page.getResults()))
            .flatMapMany(categories -> {
                Map<String, UUID> categoryIdByCode = categories.stream()
                    .collect(Collectors.toMap(com.backbase.investment.api.service.v1.model.AssetCategory::getCode,
                        com.backbase.investment.api.service.v1.model.AssetCategory::getUuid));
                // Deduplicate assets by key to prevent concurrent creation of the same asset
                Map<String, com.backbase.stream.investment.Asset> uniqueAssets = assets.stream()
                    .collect(Collectors.toMap(
                        com.backbase.stream.investment.Asset::getKeyString,
                        a -> a,
                        (existing, replacement) -> {
                            log.warn("Duplicate asset key found: {}. Using first occurrence.",
                                existing.getKeyString());
                            return existing;
                        }
                    ));
                // Limit concurrency to 5 to prevent overwhelming the service and triggering 503 errors
                return Flux.fromIterable(uniqueAssets.values())
                    .flatMap(asset -> this.upsertAsset(asset, categoryIdByCode)
                        .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                            .filter(this::isRetryableError)
                            .doBeforeRetry(signal -> log.warn(
                                "Retrying asset upsert: key={}, attempt={}",
                                asset.getKeyString(), signal.totalRetries() + 1))),
                        5);
            });
    }

    /**
     * Returns {@code true} when the given {@link MarketRequest} carries the same data as the
     * already-stored {@link Market}.
     */
    private boolean isMarketSame(MarketRequest request, Market existing) {
        log.debug("Mapped existing marketRequest: {}", assetMapper.toMarketRequest(existing));
        log.debug("Incoming marketRequest: {}", request);
        boolean same = request.equals(assetMapper.toMarketRequest(existing));
        log.debug("Market same check: code={}, same={}", existing.getCode(), same);
        return same;
    }

    /**
     * Returns {@code true} when the desired asset state is the same as what is already stored.
     * Checks normalized data fields, logo file, and categories (order-insensitive).
     */
    private boolean isAssetSame(com.backbase.stream.investment.Asset desired,
        com.backbase.stream.investment.Asset existingMapped, URI existingLogo) {
        log.debug("Existing mapped asset: {}", existingMapped);
        log.debug("Desired asset: {}", desired);
        boolean dataSame = normalizeAsset(existingMapped).equals(normalizeAsset(desired));
        boolean logoSame = isFileSame(existingLogo, desired.getLogo());
        boolean categoriesSame = areCategoriesSame(existingMapped.getCategories(), desired.getCategories());
        log.debug("Asset same check: dataSame={}, logoSame={}, categoriesSame={}",
            dataSame, logoSame, categoriesSame);
        return dataSame && logoSame && categoriesSame;
    }

    /**
     * Returns {@code true} when the given {@link MarketSpecialDayRequest} carries the same data as
     * the already-stored {@link MarketSpecialDay}.
     */
    private boolean isMarketSpecialDaySame(MarketSpecialDayRequest request, MarketSpecialDay existing) {
        log.debug("Mapped existing MarketSpecialDayRequest: {}", assetMapper.toMarketSpecialDayRequest(existing));
        log.debug("Incoming MarketSpecialDayRequest: {}", request);
        boolean same = request.equals(assetMapper.toMarketSpecialDayRequest(existing));
        log.debug("Market special day same check: date={}, market={}, same={}",
            existing.getDate(), existing.getMarket(), same);
        return same;
    }

    /**
     * Returns {@code true} when the desired asset category state is the same as what is already
     * stored. Checks data fields and image file separately.
     */
    private boolean isAssetCategorySame(AssetCategoryEntry desired, AssetCategoryEntry existingEntry,
        URI existingImage) {
        log.debug("Existing asset category: {}", existingEntry);
        log.debug("Desired asset category:  {}", desired);
        boolean dataSame  = existingEntry.equals(desired);
        String desiredImageFilename = desired.getImageResource() != null
            ? desired.getImageResource().getFilename() : null;
        boolean imageSame = isFileSame(existingImage, desiredImageFilename);
        log.debug("Asset category same check: code={}, dataSame={}, imageSame={}",
            desired.getCode(), dataSame, imageSame);
        return dataSame && imageSame;
    }

    /**
     * Returns {@code true} when the given {@link AssetCategoryTypeRequest} carries the same data as
     * the already-stored {@link AssetCategoryType}.
     */
    private boolean isAssetCategoryTypeSame(AssetCategoryTypeRequest request, AssetCategoryType existing) {
        boolean same = request.equals(assetMapper.toAssetCategoryTypeRequest(existing));
        log.debug("Asset category type same check: code={}, same={}", existing.getCode(), same);
        return same;
    }

    /**
     * Determines whether an error is retryable based on HTTP status code.
     *
     * <p>Retryable errors:
     * <ul>
     *   <li>409 Conflict – race condition during concurrent creation</li>
     *   <li>503 Service Unavailable – temporary service overload or maintenance</li>
     * </ul>
     *
     * @param throwable the error to evaluate
     * @return {@code true} if the error should trigger a retry
     */
    private boolean isRetryableError(Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            int statusCode = ex.getStatusCode().value();
            boolean retryable = statusCode == 409 || statusCode == 503;
            if (retryable) {
                log.debug("Identified retryable error: status={}, reason={}",
                    statusCode, statusCode == 409 ? "CONFLICT" : "SERVICE_UNAVAILABLE");
            }
            return retryable;
        }
        return false;
    }

    /**
     * Returns a normalized copy of the asset for same-check comparison (trims {@code name} and
     * {@code description}, normalizes empty {@code extraData} to {@code null}).
     * Does not mutate the original.
     */
    private com.backbase.stream.investment.Asset normalizeAsset(
        com.backbase.stream.investment.Asset asset) {
        if (asset == null) {
            return null;
        }
        // toBuilder() copies every field; only name and description are overridden with trimmed
        // versions. If Asset ever gains new fields they are automatically included in the copy.
        return asset.toBuilder()
            .name(asset.getName() != null ? asset.getName().trim() : null)
            .description(asset.getDescription() != null ? asset.getDescription().trim() : null)
            .extraData(asset.getExtraData() != null && !asset.getExtraData().isEmpty()
                ? asset.getExtraData() : null)
            .build();
    }

    /**
     * Order-insensitive comparison of two category code lists.
     *
     * @return {@code true} if both lists contain exactly the same codes
     */
    private boolean areCategoriesSame(List<String> existingCategories, List<String> desiredCategories) {
        List<String> existing = Objects.requireNonNullElse(existingCategories, List.of());
        List<String> desired = Objects.requireNonNullElse(desiredCategories, List.of());
        boolean same = existing.size() == desired.size()
            && new java.util.HashSet<>(existing).containsAll(desired);
        log.debug("Categories same check: existing={}, desired={}, same={}", existing, desired, same);
        return same;
    }

    /**
     * Returns {@code true} if the stored file does not need re-uploading.
     * No-ops when {@code desiredFilename} is {@code null}; requires upload when
     * {@code existingUri} is {@code null}; otherwise checks that the URI contains the filename.
     */
    private boolean isFileSame(URI existingUri, String desiredFilename) {
        if (desiredFilename == null) {
            return true;
        }
        if (existingUri == null) {
            return false;
        }
        boolean same = existingUri.toString().contains(desiredFilename);
        log.debug("File same check: desiredFilename='{}', existingUri='{}'", desiredFilename, existingUri);
        return same;
    }

    /**
     * Upserts an asset category by code: patches if changed, creates if not found.
     *
     * @param assetCategoryEntry the desired asset category state
     * @return the existing, patched, or newly created {@link AssetCategory}
     */
    public Mono<AssetCategory> upsertAssetCategory(AssetCategoryEntry assetCategoryEntry) {
        if (assetCategoryEntry == null) {
            return Mono.empty();
        }
        return assetUniverseApi.listAssetCategories(assetCategoryEntry.getCode(), 100,
                assetCategoryEntry.getName(), 0, assetCategoryEntry.getOrder(),
                assetCategoryEntry.getType())
            .flatMap(paginatedAssetCategoryList -> Optional.ofNullable(paginatedAssetCategoryList)
                .map(PaginatedAssetCategoryList::getResults)
                .filter(Predicate.not(List::isEmpty))
                .flatMap(l -> l.stream()
                    .filter(ac -> assetCategoryEntry.getCode().equals(ac.getCode()))
                    .findAny())
                .map(c -> {
                    log.info("Asset category already exists for code: {}", assetCategoryEntry.getCode());
                    // Compare content fields; uuid/image/imageResource are excluded and image is
                    // checked separately via isFileSame().
                    AssetCategoryEntry existingEntry = assetMapper.toAssetCategoryEntry(c);

                    if (isAssetCategorySame(assetCategoryEntry, existingEntry, c.getImage())) {
                        log.info("Skipping asset category patch - no changes detected for code: {}",
                            assetCategoryEntry.getCode());
                        // Return a non-empty Mono so that switchIfEmpty is NOT triggered.
                        // A lightweight AssetCategory carrying only the uuid is sufficient.
                        assetCategoryEntry.setUuid(c.getUuid());
                        return Mono.just(new AssetCategory(c.getUuid()));
                    }
                    log.info("Patching asset category for code: {}", assetCategoryEntry.getCode());
                    return investmentRestAssetUniverseService.patchAssetCategory(
                        c.getUuid(),
                        assetCategoryEntry, assetCategoryEntry.getImageResource())
                        .doOnSuccess(updatedCategory -> {
                            assetCategoryEntry.setUuid(updatedCategory.getUuid());
                            log.info("Updated asset category: code={}", assetCategoryEntry.getCode());
                        })
                        .doOnError(error -> {
                            if (error instanceof WebClientResponseException w) {
                                log.error("Error updating asset category: {} : HTTP {} -> {}",
                                    assetCategoryEntry.getCode(),
                                    w.getStatusCode(), w.getResponseBodyAsString());
                            } else {
                                log.error("Error updating asset category: {} : {}",
                                    assetCategoryEntry.getCode(),
                                    error.getMessage(), error);
                            }
                        })
                        .onErrorResume(e -> Mono.empty());
                })
                .orElseGet(() -> {
                    log.debug("No asset category exists for code: {}", assetCategoryEntry.getCode());
                    return Mono.empty();
                })
                .switchIfEmpty(
                    Mono.defer(() -> investmentRestAssetUniverseService
                        .createAssetCategory(assetCategoryEntry, assetCategoryEntry.getImageResource())
                        .doOnSuccess(createdCategory -> {
                            assetCategoryEntry.setUuid(createdCategory.getUuid());
                            log.info("Created asset category: code={}", assetCategoryEntry.getCode());
                        })
                        .doOnError(error -> {
                            if (error instanceof WebClientResponseException w) {
                                log.error("Error creating asset category: {} : HTTP {} -> {}",
                                    assetCategoryEntry.getCode(),
                                    w.getStatusCode(), w.getResponseBodyAsString());
                            } else {
                                log.error("Error creating asset category: {} : {}",
                                    assetCategoryEntry.getCode(),
                                    error.getMessage(), error);
                            }
                        })
                        .onErrorResume(e -> Mono.empty()))
                )
            );
    }

    /**
     * Upserts an asset category type by code: updates if changed, creates if not found.
     *
     * @param assetCategoryTypeRequest the desired asset category type state
     * @return the existing, updated, or newly created {@link AssetCategoryType}
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
                        log.info("Asset category type already exists for code: {}",
                            assetCategoryTypeRequest.getCode());
                        AssetCategoryType existingType = matchingType.get();
                        // Skip update if data is the same as what is stored.
                        if (isAssetCategoryTypeSame(assetCategoryTypeRequest, existingType)) {
                            log.info("Skipping asset category type update - no changes detected for code: {}",
                                assetCategoryTypeRequest.getCode());
                            return Mono.just(existingType);
                        }
                        return assetUniverseApi.updateAssetCategoryType(existingType.getUuid().toString(),
                                assetCategoryTypeRequest)
                            .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                                .filter(this::isRetryableError)
                                .doBeforeRetry(signal -> log.warn(
                                    "Retrying asset category type update: code={}, attempt={}",
                                    assetCategoryTypeRequest.getCode(), signal.totalRetries() + 1)))
                            .doOnSuccess(updatedType -> log.info("Updated asset category type: code={}",
                                assetCategoryTypeRequest.getCode()))
                            .doOnError(error -> {
                                if (error instanceof WebClientResponseException w) {
                                    log.error("Error updating asset category type: {} : HTTP {} -> {}",
                                        assetCategoryTypeRequest.getCode(), w.getStatusCode(),
                                        w.getResponseBodyAsString());
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
                Mono.defer(() -> assetUniverseApi.createAssetCategoryType(assetCategoryTypeRequest)
                    .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                        .filter(this::isRetryableError)
                        .doBeforeRetry(signal -> log.warn(
                            "Retrying asset category type create: code={}, attempt={}",
                            assetCategoryTypeRequest.getCode(), signal.totalRetries() + 1)))
                    .doOnSuccess(createdType -> log.info("Created asset category type: code={}",
                        assetCategoryTypeRequest.getCode()))
                    .doOnError(error -> {
                        if (error instanceof WebClientResponseException w) {
                            log.error("Error creating asset category type: {} : HTTP {} -> {}",
                                assetCategoryTypeRequest.getCode(),
                                w.getStatusCode(), w.getResponseBodyAsString());
                        } else {
                            log.error("Error creating asset category type: {} : {}",
                                assetCategoryTypeRequest.getCode(), error.getMessage(), error);
                        }
                    }))
            );
    }
}
