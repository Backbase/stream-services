package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.v1.AssetUniverseApi;
import com.backbase.investment.api.service.v1.model.OASCreatePriceRequest;
import com.backbase.investment.api.service.v1.model.PaginatedOASPriceList;
import com.backbase.investment.api.service.v1.model.TypeEnum;
import com.backbase.stream.investment.Asset;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class InvestmentAssetPriceService {

    private final AssetUniverseApi assetUniverseApi;
    private final SecureRandom random = new SecureRandom();

    public Mono<List<Asset>> ingestPrices(List<Asset> assets, Map<String, Double> priceByAsset) {
        return Flux.fromIterable(Objects.requireNonNullElse(assets, List.of()))
            .flatMap(a -> {
                LocalDate to = LocalDate.now();
                LocalDate from = LocalDate.now().minusDays(10);
                return assetUniverseApi.listAssetClosePrices(a.currency(), from, to, null, null, null, null,
                        a.isin(),
                        100,
                        a.market(), null, null)
                    .filter(Objects::nonNull)
                    .map(PaginatedOASPriceList::getResults)
                    .filter(Objects::nonNull)
                    .flatMap(prices -> {
                        Double lastPrice = prices.isEmpty() ? priceByAsset.getOrDefault(
                            "%s_%s_%s".formatted(a.isin(), a.market(), a.currency()), 100d)
                            : prices.getLast().getAmount();
                        LocalDate lastDate = prices.isEmpty() ? from : prices.getLast().getDatetime().toLocalDate().plusDays(1);

                        List<LocalDate> daysToCreate = Stream.iterate(lastDate,
                                offsetDate -> offsetDate.isBefore(to),
                                offsetDateTime -> offsetDateTime.plusDays(1))
                            .sorted()
                            .toList();

                        List<OASCreatePriceRequest> oaSCreatePriceRequest = generateHistoryPrices(daysToCreate,
                            lastPrice, a);
//                        Flux<GroupResult> groupResultFlux = assetUniverseApi.bulkCreateAssetClosePrice(
//                            oaSCreatePriceRequest,
//                            null, null, null);
                        return Flux.fromIterable(oaSCreatePriceRequest)
                            .flatMap(p -> assetUniverseApi.createAssetClosePrices(p, null, null, null))
                            .collectList()
                            .doOnSuccess(created -> log.info(
                                "Successfully create prices: count={}", created.size()))
                            .doOnError(throwable -> {
                                if (throwable instanceof WebClientResponseException ex) {
                                    log.error("Failed to {} model portfolio: status={}, body={}", ex.getStatusCode(),
                                        ex.getResponseBodyAsString(), ex);
                                } else {
                                    log.error("Failed to {}}", throwable);
                                }
                            });
                    })
                    .map(o -> a);
            })
            .collectList();
    }

    private List<OASCreatePriceRequest> generateHistoryPrices(List<LocalDate> daysToCreate, double price, Asset asset) {
        AtomicReference<Double> startValuation = new AtomicReference<>(price);

        return daysToCreate.stream()
            .map(d -> {
                double rnd = random.nextDouble(0.99156, 1.011);
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

}