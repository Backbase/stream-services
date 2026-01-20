package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.v1.AssetUniverseApi;
import com.backbase.investment.api.service.v1.model.GroupResult;
import com.backbase.investment.api.service.v1.model.OASCreatePriceRequest;
import com.backbase.investment.api.service.v1.model.TypeEnum;
import com.backbase.stream.investment.AssetLatestPrice;
import com.backbase.stream.investment.PaginatedExpandedAssetList;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service responsible for generating and ingesting intraday asset prices.
 *
 * <p>Responsibilities:
 * - Read assets with latest prices via {@link AssetUniverseApi}.
 * - Generate a series of intraday OHLC price points per asset.
 * - Submit intraday prices back to the Asset API using bulk create.
 *
 * <p>Notes:
 * - The generator uses a randomised model constrained to realistic percentage ranges.
 * - The service is reactive and non-blocking: ingestion returns a {@link Mono} that completes
 *   after all async submissions are triggered.
 *
 */
@Slf4j
@RequiredArgsConstructor
public class InvestmentIntradayAssetPriceService {

    private final AssetUniverseApi assetUniverseApi;

    /**
     * Generates and triggers ingestion of intraday prices for all assets that have a latest price.
     *
     * @return a {@link Mono} that emits the combined list of results for batch operations when available.
     */
    public Mono<List<GroupResult>> ingestIntradayPrices() {
        return generateIntradayPrices()
            .map(asyncTasks -> asyncTasks.stream().flatMap(Collection::stream).toList());
    }

    /**
     * Internal reactive pipeline that fetches assets and creates intraday price creation tasks.
     *
     * @return a {@link Mono} emitting a list of lists of created {@link GroupResult}s (one list per asset request).
     */
    @Nonnull
    private Mono<List<List<GroupResult>>> generateIntradayPrices() {
        log.info("Generating Intraday Prices for Assets");
        return assetUniverseApi.listAssetsWithResponseSpec(
                null, null, null, null,
                Collections.singletonList("latest_price"),
                null, null,
                "uuid,latest_price",
                null, null, null, null, null,
                null, null, null, null
            )
            .bodyToMono(PaginatedExpandedAssetList.class)
            .flatMap(paginatedAssetList -> {

                if (paginatedAssetList.getCount() == 0) {
                    log.warn("No assets found with latest prices to generate intraday prices");
                    return Mono.just(List.<List<GroupResult>>of());
                }

                return Flux.fromIterable(paginatedAssetList.getResults())
                    .flatMap(assetLatestPrice -> {
                        List<OASCreatePriceRequest> requests =
                            generateIntradayPricesForAsset(assetLatestPrice);

                        log.debug("Generated intraday price requests: {}", requests);

                        if (requests.isEmpty()) {
                            return Mono.empty();
                        }

                        return assetUniverseApi.bulkCreateIntradayAssetPrice(requests, null, null, null)
                            .collectList()
                            .doOnSuccess(created ->
                                log.info(
                                    "Successfully triggered creation of {} intraday prices for asset ({})",
                                    requests.size(),
                                    assetLatestPrice.uuid()
                                )
                            )
                            .doOnError(WebClientResponseException.class, ex ->
                                log.error(
                                    "Failed to create intraday prices for asset ({}): status={}, body={}",
                                    assetLatestPrice.uuid(),
                                    ex.getStatusCode(),
                                    ex.getResponseBodyAsString(),
                                    ex
                                )
                            )
                            .onErrorResume(e -> Mono.empty());
                    })
                    .collectList();
            })
            .doOnError(error -> {
                if (error instanceof WebClientResponseException w) {
                    log.error(
                        "Error generating intraday prices for assets: HTTP {} -> {}",
                        w.getStatusCode(),
                        w.getResponseBodyAsString()
                    );
                } else {
                    log.error(
                        "Error generating intraday prices for assets: {}",
                        error.getMessage(),
                        error
                    );
                }
            });

    }

    /**
     * Generate a list of intraday price create requests for a single asset.
     *
     * <p>Each request represents a 10-minute / 15-candle sequence starting around 10:30 UTC.
     *
     * @param assetLatestPrice the asset data with latest price information
     * @return a list of {@link OASCreatePriceRequest} ready to be submitted
     */
    private List<OASCreatePriceRequest> generateIntradayPricesForAsset(AssetLatestPrice assetLatestPrice) {
        List<OASCreatePriceRequest> requests = new ArrayList<>();

        // Base previous close
        Double previousClose = assetLatestPrice.latestPrice().previousClosePrice();

        // Today, starting at 10:30
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalTime time = LocalTime.of(10, 30).plusMinutes(ThreadLocalRandom.current().nextInt(0, 10));

        for (int i = 0; i < 15; i++) {

            // Generate intraday OHLC
            Map<String, Double> ohlc = generateIntradayOhlc(previousClose);

            OASCreatePriceRequest oasCreatePriceRequest = new OASCreatePriceRequest();

            try {
                oasCreatePriceRequest.amount(ohlc.get("close"));
                oasCreatePriceRequest.asset(Map.of("uuid", assetLatestPrice.uuid().toString()));
                oasCreatePriceRequest.datetime(
                    OffsetDateTime.of(today, time, ZoneOffset.UTC)
                );
                oasCreatePriceRequest.open(ohlc.get("open"));
                oasCreatePriceRequest.high(ohlc.get("high"));
                oasCreatePriceRequest.low(ohlc.get("low"));
                oasCreatePriceRequest.previousClose(previousClose);
                oasCreatePriceRequest.type(TypeEnum.INTRADAY);
            } catch (NoSuchMethodError ignored) {
                log.debug("AssetLatestPrice: {}", assetLatestPrice);
                log.warn("Failed to map intraday price for asset: {}", assetLatestPrice.uuid(), ignored);
            }

            requests.add(oasCreatePriceRequest);

            // Next candle starts from this close
            previousClose = ohlc.get("close");

            // Move time forward by 15 minutes
            time = time.plusMinutes(10);
        }
        return requests;
    }

    /**
     * Deterministic randomised OHLC generator based on a previous close.
     *
     * <p>Algorithm constraints:
     * - Total intraday range: 2–5% (implemented as 4–9 per mille in code).
     * - Opening gap: ±0.1–0.3%.
     * - Candle body: 0.2–1.2%.
     * - Wicks are larger than body and distributed to respect direction.
     *
     * @param previousClose the prior close price (must be positive)
     * @return a map with keys \"open\", \"high\", \"low\", \"close\" rounded to 6 decimal places
     * @throws IllegalArgumentException if previousClose is null or not positive
     */
    public static Map<String, Double> generateIntradayOhlc(Double previousClose) {
        if (previousClose == null || previousClose <= 0) {
            throw new IllegalArgumentException("Previous close must be positive");
        }

        ThreadLocalRandom r = ThreadLocalRandom.current();

        // Total intraday range: 2–5%
        double totalRangePct = r.nextDouble(4.0, 9.01) / 100.0;

        // Small opening gap: ±0.1–0.3%
        double openGapPct = r.nextDouble(-0.3, 0.31) / 100.0;
        double open = previousClose * (1 + openGapPct);

        // Direction
        boolean bullish = r.nextBoolean();

        // Candle body: 0.2–1.2%
        double bodyPct = r.nextDouble(0.2, 1.21) / 100.0;
        double close = bullish
            ? open * (1 + bodyPct)
            : open * (1 - bodyPct);

        // Wicks larger than body
        double wickBudget = Math.max(totalRangePct - bodyPct, totalRangePct * 0.6);

        double upperWickPct;
        double lowerWickPct;

        if (bullish) {
            upperWickPct = wickBudget * r.nextDouble(0.6, 0.9);
            lowerWickPct = wickBudget - upperWickPct;
        } else {
            lowerWickPct = wickBudget * r.nextDouble(0.6, 0.9);
            upperWickPct = wickBudget - lowerWickPct;
        }

        double high = Math.max(open, close) * (1 + upperWickPct);
        double low = Math.min(open, close) * (1 - lowerWickPct);

        return Map.of(
            "open", round6(open),
            "high", round6(high),
            "low", round6(low),
            "close", round6(close)
        );
    }

    private static double round6(double value) {
        return BigDecimal
            .valueOf(value)
            .setScale(6, RoundingMode.HALF_UP)
            .doubleValue();
    }

}
