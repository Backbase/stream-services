package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.v1.AllocationsApi;
import com.backbase.investment.api.service.v1.AssetUniverseApi;
import com.backbase.investment.api.service.v1.InvestmentApi;
import com.backbase.investment.api.service.v1.InvestmentProductsApi;
import com.backbase.investment.api.service.v1.PortfolioApi;
import com.backbase.investment.api.service.v1.model.Method570Enum;
import com.backbase.investment.api.service.v1.model.OASAllocationCreateRequest;
import com.backbase.investment.api.service.v1.model.OASAllocationPositionCreateRequest;
import com.backbase.investment.api.service.v1.model.OASCreateOrderRequest;
import com.backbase.investment.api.service.v1.model.OASPortfolioAllocation;
import com.backbase.investment.api.service.v1.model.OASPrice;
import com.backbase.investment.api.service.v1.model.OrderTypeEnum;
import com.backbase.investment.api.service.v1.model.PaginatedOASPortfolioAllocationList;
import com.backbase.investment.api.service.v1.model.PaginatedOASPriceList;
import com.backbase.investment.api.service.v1.model.PortfolioList;
import com.backbase.investment.api.service.v1.model.PortfolioProduct;
import com.backbase.investment.api.service.v1.model.StatusA7dEnum;
import com.backbase.stream.investment.Allocation;
import com.backbase.stream.investment.Asset;
import com.backbase.stream.investment.InvestmentAssetData;
import com.backbase.stream.investment.ModelAsset;
import com.backbase.stream.investment.ModelPortfolio;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service wrapper around generated {@link PortfolioApi} and {@link InvestmentProductsApi} providing guarded
 * create/patch operations with logging, minimal idempotency helpers and consistent error handling.
 *
 * <p>This service manages:
 * <ul>
 *   <li>Investment product creation and updates (portfolio products)</li>
 *   <li>Investment portfolio creation and updates</li>
 *   <li>Client-to-portfolio associations via legal entity mappings</li>
 * </ul>
 *
 * <p>Design notes (see CODING_RULES_COPILOT.md):
 * <ul>
 *   <li>No direct manipulation of generated API classes beyond construction & mapping</li>
 *   <li>Side-effecting operations are logged at info (create) or debug (patch) levels</li>
 *   <li>Exceptions from the underlying WebClient are propagated (caller decides retry strategy)</li>
 *   <li>All reactive operations include proper success and error handlers for observability</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class InvestmentPortfolioAllocationService {

    private final AllocationsApi allocationsApi;
    private final AssetUniverseApi assetUniverseApi;
    private final InvestmentApi investmentApi;
    private final CustomIntegrationApiService customIntegrationApiService;

    public Mono<List<OASPortfolioAllocation>> generateAllocations(PortfolioList portfolio,
        List<PortfolioProduct> portfolioProducts, InvestmentAssetData investmentAssetData) {
        return getPortfolioModel(portfolio, portfolioProducts, investmentAssetData.getAssetByUuid())
            .flatMap(m -> {
                LocalDate endDat = LocalDate.now();
                LocalDate startAllocation = Optional.ofNullable(portfolio.getActivated())
                    .map(OffsetDateTime::toLocalDate)
                    .orElse(endDat).plusDays(1);

                List<LocalDate> days = Stream.iterate(startAllocation,
                        offsetDate -> offsetDate.isBefore(endDat),
                        offsetDateTime -> offsetDateTime.plusDays(1))
                    .toList();
                return getPriceDayByAssetKey(m, startAllocation, endDat)
                    .flatMap(priceDayByAssetKey -> upsertAllocations(portfolio.getUuid(),
                        startAllocation, m, days, priceDayByAssetKey));
            });
    }

    public Mono<List<OASPortfolioAllocation>> upsertAllocations(UUID portfolioId, LocalDate startDay,
        ModelPortfolio model, List<LocalDate> days, Map<String, Map<LocalDate, Double>> priceDayByAssetKey) {

        double initialCash = 10_000d;

        List<OASAllocationPositionCreateRequest> portfolioPositions = model.getAllocations().stream()
            .map(a -> {
                    Double price = getPrice(a.asset().getKeyString(), priceDayByAssetKey, startDay);
                    return new OASAllocationPositionCreateRequest()
                        .asset(a.asset().getAssetMap())
                        .shares((initialCash * a.weight()) / price)
                        .price(price);
                }
            )
            .toList();
        double totalTrade = calculateBalance(portfolioPositions);
        double cash = initialCash - totalTrade;
        AtomicReference<Double> balance = new AtomicReference<>(totalTrade);

        List<OASAllocationCreateRequest> allocations = days.stream()
            .map(d -> {
                    double newBalance = calculateBalance(portfolioPositions);
                    OASAllocationCreateRequest allocationDataRequest = new OASAllocationCreateRequest()
                        .invested(roundPrice(initialCash))
                        .tradeTotal(roundPrice(totalTrade))
                        .balance(roundPrice(newBalance))
                        .earnings(roundPrice(newBalance - balance.get()))
                        .cashActive(roundPrice(cash))
                        .valuationDate(d)
                        .positions(portfolioPositions.stream()
                            .map(p -> new OASAllocationPositionCreateRequest()
                                .asset(p.getAsset())
                                .shares(roundPrice(p.getShares()))
                                .price(roundPrice(p.getPrice())))
                            .toList());
                    balance.set(newBalance);
                    portfolioPositions.forEach(
                        p -> p.setPrice(getPrice(assetKey(p.getAsset()), priceDayByAssetKey, d)));
                    return allocationDataRequest;
                }
            ).toList();

        Mono<List<OASPortfolioAllocation>> upsertInvestmentPortfolioAllocation = allocationsApi.listPortfolioAllocations(
                portfolioId.toString(), null, null, null, null, null,
                null, null)
            .flatMapIterable(PaginatedOASPortfolioAllocationList::getResults)
            .flatMap(
                a -> allocationsApi.deletePortfolioAllocation(portfolioId.toString(), a.getValuationDate()))
            .collectList()
            .flatMapIterable(a -> allocations)
            .flatMap(a -> customIntegrationApiService.createPortfolioAllocation(
                portfolioId.toString(), a, null, null, null))
            .collectList()
            .doOnSuccess(created -> log.info(
                "Successfully upserted investment portfolio allocation"))
            .doOnError(throwable -> {
                if (throwable instanceof WebClientResponseException ex) {
                    log.error("Failed to upsert investment portfolio allocation: status={}, body={}",
                        ex.getStatusCode(),
                        ex.getResponseBodyAsString(), ex);
                } else {
                    log.error("Failed to upsert investment portfolio allocation", throwable);
                }
            });

        return Flux.fromIterable(portfolioPositions)
            .flatMap(p -> {
                // TODO: fix orders creation
                return investmentApi.createOrder(new OASCreateOrderRequest()
                            .asset(p.getAsset())
                            .orderType(OrderTypeEnum.BUY)
                            .portfolio(portfolioId)
                            .status(StatusA7dEnum.COMPLETED)
                            .method(Method570Enum.MARKET)
                            .completed(startDay.atTime(LocalTime.MIDNIGHT).atOffset(ZoneOffset.UTC))
                            .currency(currency(p.getAsset()))
                            .shares(p.getShares())
                            .priceAvg(p.getPrice())
                        , null, null, null)
                    .doOnSuccess(created -> log.info(
                        "Successfully upserted investment portfolio allocation"))
                    .doOnError(throwable -> {
                        if (throwable instanceof WebClientResponseException ex) {
                            log.error("Failed to upsert investment portfolio allocation: status={}, body={}",
                                ex.getStatusCode(),
                                ex.getResponseBodyAsString(), ex);
                        } else {
                            log.error("Failed to upsert investment portfolio allocation", throwable);
                        }
                    });
            })
            .then(upsertInvestmentPortfolioAllocation);
    }

    private Double getPrice(String assetKey, Map<String, Map<LocalDate, Double>> priceDayByAssetKey,
        LocalDate startAllocation) {
        Map<LocalDate, Double> priceByDay = priceDayByAssetKey.get(assetKey);
        return Optional.ofNullable(priceByDay).map(m -> m.get(startAllocation))
            // TODO: fix find day price
            .orElse(0.01d);
    }

    private Mono<Map<String, Map<LocalDate, Double>>> getPriceDayByAssetKey(ModelPortfolio model,
        LocalDate startDat, LocalDate endDay) {

        return Flux.fromIterable(model.getAllocations())
            .flatMap(a -> {
                ModelAsset asset = a.asset();
                return Objects.requireNonNull(assetUniverseApi
                    .listAssetClosePrices(asset.getCurrency(), startDat, endDay, null, null,
                        null, null, asset.getIsin(), null, asset.getMarket(), null, null)
                    .map(PaginatedOASPriceList::getResults)
                    .map(l -> Map.of(asset.getKeyString(), l.stream()
                        .collect(Collectors.toMap(dp -> dp.getDatetime().toLocalDate(), OASPrice::getAmount)))
                    ));
            })
            .collectList()
            .map(maps -> {
                Map<String, Map<LocalDate, Double>> dayPriceByAssetKey = new HashMap<>();
                maps.forEach(dayPriceByAssetKey::putAll);
                return dayPriceByAssetKey;
            });
    }

    private Mono<ModelPortfolio> getPortfolioModel(PortfolioList
            portfolio, List<PortfolioProduct> portfolioProducts,
        Map<UUID, Asset> assetByUuid) {

        return Mono.just(portfolioProducts.stream()
            .filter(m -> m.getUuid().equals(portfolio.getProduct()))
            .findFirst()
            .map(PortfolioProduct::getModelPortfolio)
            .map(m -> ModelPortfolio.builder()
                .cashWeight(m.getCashWeight())
                .allocations(m.getAllocation().stream()
                    .map(a -> new Allocation(
                        new ModelAsset(a.getAsset().getIsin(), a.getAsset().getMarket(),
                            a.getAsset().getCurrency()),
                        a.getWeight()))
                    .toList())
                .build())
            .orElse(ModelPortfolio.builder()
                .cashWeight(0.6)
                .allocations(assetByUuid.values().stream().limit(2)
                    .map(a -> new Allocation(new ModelAsset(a.getIsin(), a.getMarket(), a.currency()), 0.2))
                    .toList())
                .build()));
    }

    private String assetKey(Map<String, Object> params) {
        return isin(params) + "-" + market(params) + "-" + currency(params);
    }

    private String isin(Map<String, Object> params) {
        return params.get("isin").toString();
    }

    private String market(Map<String, Object> params) {
        return params.get("market").toString();
    }

    private String currency(Map<String, Object> params) {
        return params.get("currency").toString();
    }

    private static double roundPrice(double value) {
        return Math.round(value * 1000000.0) / 1000000.0;
    }

    private static Double calculateBalance(List<OASAllocationPositionCreateRequest> portfolioPositions) {
        return portfolioPositions.stream()
            .map(a -> a.getPrice() * a.getShares()).reduce(0.0, Double::sum);
    }

}
