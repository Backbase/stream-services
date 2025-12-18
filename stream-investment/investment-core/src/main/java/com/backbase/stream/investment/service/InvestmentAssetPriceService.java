package com.backbase.stream.investment.service;

import static com.backbase.stream.investment.service.WorkDayService.workDays;

import com.backbase.investment.api.service.v1.AssetUniverseApi;
import com.backbase.investment.api.service.v1.model.GroupResult;
import com.backbase.investment.api.service.v1.model.OASCreatePriceRequest;
import com.backbase.investment.api.service.v1.model.OASPrice;
import com.backbase.investment.api.service.v1.model.PaginatedOASPriceList;
import com.backbase.investment.api.service.v1.model.TypeEnum;
import com.backbase.stream.investment.Asset;
import com.backbase.stream.investment.AssetPrice;
import com.backbase.stream.investment.RandomParam;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class InvestmentAssetPriceService {

    public static final double DEFAULT_START_PRICE = 100d;
    public static final RandomParam defaultRandomParam = new RandomParam(0.99156, 1.011);
    private final SecureRandom random = new SecureRandom();

    private final AssetUniverseApi assetUniverseApi;

    public Mono<List<GroupResult>> ingestPrices(List<Asset> assets, Map<String, AssetPrice> priceByAsset) {
        return generatePrices(assets, priceByAsset)
            .map(asyncTasks -> asyncTasks.stream().flatMap(Collection::stream).toList());
    }

    @Nonnull
    private Mono<List<List<GroupResult>>> generatePrices(List<Asset> assets, Map<String, AssetPrice> priceByAsset) {
        return Flux.fromIterable(Objects.requireNonNullElse(assets, List.of()))
            .flatMap(asset -> {
                LocalDate now = LocalDate.now();
                LocalDate from = now.minusYears(1);
                // we get only last days price
                int daysOfPrices = 10;
                return assetUniverseApi.listAssetClosePrices(asset.currency(), now.minusDays(daysOfPrices), now, null,
                        null, null, null,
                        asset.isin(), daysOfPrices + 1, asset.market(), null, null)
                    .filter(Objects::nonNull)
                    .map(PaginatedOASPriceList::getResults)
                    .filter(Objects::nonNull)
                    .flatMap(prices -> {
                        RandomPriceParam priceParam = findPrice(priceByAsset, asset, getLastPrice(prices));
                        LocalDate lastDate =
                            prices.isEmpty() ? from : prices.getLast().getDatetime().toLocalDate().plusDays(1);

                        List<LocalDate> daysToCreate = workDays(lastDate, now);
                        List<OASCreatePriceRequest> oaSCreatePriceRequest = generateHistoryPrices(daysToCreate,
                            priceParam, asset);
                        if (oaSCreatePriceRequest.isEmpty()) {
                            return Mono.empty();
                        }
                        log.debug("Generated prices for asset ({}) [{}]", asset.getKeyString(), oaSCreatePriceRequest);
                        return assetUniverseApi.bulkCreateAssetClosePrice(oaSCreatePriceRequest,
                                null, null, null)
                            .collectList()
                            .doOnSuccess(created -> {
                                log.info(
                                    "Successfully create prices: count={} for asset ({})", created.size(),
                                    asset.getKeyString());
                                log.debug("Async tasks to create prices: for asset ({}) [{}]", asset.getKeyString(),
                                    created);
                            })
                            .doOnError(throwable -> {
                                if (throwable instanceof WebClientResponseException ex) {
                                    log.error("Failed to create asset({}) price: status={}, body={}",
                                        asset.getKeyString(),
                                        ex.getStatusCode(),
                                        ex.getResponseBodyAsString(), ex);
                                } else {
                                    log.error("Failed prices creation for asset({})", asset.getKeyString(), throwable);
                                }
                            });
                    })
                    .filter(Predicate.not(CollectionUtils::isEmpty));
            })
            .collectList();
    }

    private static Double getLastPrice(List<OASPrice> prices) {
        return Optional.ofNullable(prices)
            .filter(Predicate.not(List::isEmpty))
            .map(List::getLast)
            .map(OASPrice::getAmount)
            .orElse(null);
    }

    private static RandomPriceParam findPrice(Map<String, AssetPrice> priceByAsset, Asset a, Double lastPrice) {
        AssetPrice configAssetPrice = priceByAsset.get(a.getKeyString());
        RandomParam randomParam = Optional.ofNullable(configAssetPrice)
            .map(AssetPrice::randomParam)
            .orElse(defaultRandomParam);
        double price = Optional.ofNullable(lastPrice)
            .or(() -> Optional.ofNullable(configAssetPrice).map(AssetPrice::price)
                .or(() -> Optional.of(a).map(Asset::defaultPrice)))
            .orElse(DEFAULT_START_PRICE);
        return new RandomPriceParam(price, randomParam);
    }

    private List<OASCreatePriceRequest> generateHistoryPrices(List<LocalDate> daysToCreate,
        RandomPriceParam priceParam, Asset asset) {
        RandomParam randomParam = priceParam.randomParam();
        AtomicReference<Double> startValuation = new AtomicReference<>(priceParam.price());
        return daysToCreate.stream()
            .sorted(Comparator.reverseOrder())
            .map(d -> {
                double rnd = random.nextDouble(randomParam.origin(), randomParam.bound());
                double prev = startValuation.get();
                double newValue = prev / rnd;
                startValuation.set(newValue);

                return new OASCreatePriceRequest()
                    .asset(asset.getAssetMap())
                    // limit for 6 digits
                    .amount(Math.round(newValue * 1000000.0) / 1000000.0)
                    .datetime(d.atTime(LocalTime.MIDNIGHT).atOffset(ZoneOffset.UTC))
                    .type(TypeEnum.CLOSE);
            })
            .toList();
    }

    private record RandomPriceParam(double price, RandomParam randomParam) {

    }

}