package com.backbase.stream.investment.service;

import static com.backbase.stream.investment.service.WorkDayService.nextWorkDay;
import static com.backbase.stream.investment.service.WorkDayService.workDays;

import com.backbase.investment.api.service.v1.AllocationsApi;
import com.backbase.investment.api.service.v1.AssetUniverseApi;
import com.backbase.investment.api.service.v1.InvestmentApi;
import com.backbase.investment.api.service.v1.InvestmentProductsApi;
import com.backbase.investment.api.service.v1.PortfolioApi;
import com.backbase.investment.api.service.v1.model.Deposit;
import com.backbase.investment.api.service.v1.model.Method570Enum;
import com.backbase.investment.api.service.v1.model.OASAllocationCreateRequest;
import com.backbase.investment.api.service.v1.model.OASAllocationPositionCreateRequest;
import com.backbase.investment.api.service.v1.model.OASCreateOrderRequest;
import com.backbase.investment.api.service.v1.model.OASPortfolioAllocation;
import com.backbase.investment.api.service.v1.model.OASPrice;
import com.backbase.investment.api.service.v1.model.OrderTypeEnum;
import com.backbase.investment.api.service.v1.model.PaginatedOASOrderList;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.util.CollectionUtils;
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

    public Mono<Void> removeAllocations(PortfolioList portfolio) {
        String portfolioUuid = portfolio.getUuid().toString();
        return allocationsApi.listPortfolioAllocations(portfolioUuid, null, null, null, null, null, null, null)
            .flatMapIterable(PaginatedOASPortfolioAllocationList::getResults)
            .flatMap(a -> allocationsApi.deletePortfolioAllocation(portfolioUuid, a.getValuationDate())).collectList()
            .flatMap(v -> Mono.empty());
    }

    public Mono<List<OASPortfolioAllocation>> generateAllocations(PortfolioList portfolio,
        List<PortfolioProduct> portfolioProducts, InvestmentAssetData investmentAssetData) {
        return getPortfolioModel(portfolio, portfolioProducts, investmentAssetData.getAssetByUuid())
            .flatMap(m -> {
                LocalDate endDay = LocalDate.now();
                LocalDate startDay = nextWorkDay(portfolio.getActivated(), endDay, 4);

                return getPriceDayByAssetKey(m, startDay, endDay).flatMap(
                    priceDayByAssetKey -> getAllocations(portfolio.getUuid().toString(), startDay, endDay)
                        .filter(Objects::nonNull)
                        .map(a -> a.stream().filter(Predicate.not(aa -> aa.getPositions().isEmpty())).toList())
                        .filter(Predicate.not(List::isEmpty))
                        .map(portfolioAllocations -> {
                            OASPortfolioAllocation lastValuation = portfolioAllocations.getFirst();
                            LocalDate nextValuationDate = lastValuation.getValuationDate().plusDays(1);
                            List<LocalDate> days = workDays(nextValuationDate, endDay);
                            if (days.isEmpty()) {
                                return List.<OASAllocationCreateRequest>of();
                            }
                            Map<UUID, Asset> assetByUuid = investmentAssetData.getAssetByUuid();
                            List<OASAllocationPositionCreateRequest> list = lastValuation.getPositions().stream().map(
                                p -> new OASAllocationPositionCreateRequest().asset(
                                    assetByUuid.get(p.getAsset()).getAssetMap()).shares(p.getShares()).price(
                                    getPrice(assetKey(assetByUuid.get(p.getAsset()).getAssetMap()), priceDayByAssetKey,
                                        nextValuationDate).orElse(p.getPrice()))).toList();
                            return generateAllocations(priceDayByAssetKey, Pair.of(days, list),
                                lastValuation.getInvested(),
                                lastValuation.getCashActive(), lastValuation.getTradeTotal());
                        }).switchIfEmpty(
                            orderPositions(portfolio.getUuid(), workDays(startDay, endDay), m, priceDayByAssetKey,
                                10_000d)
                                .flatMap(dp -> generateAllocations(priceDayByAssetKey, dp)))
                        .flatMap(allocations -> this.upsertAllocations(portfolio.getUuid().toString(), allocations)));
            })
            .onErrorResume(ex -> Mono.empty());
    }

    private Mono<List<OASPortfolioAllocation>> getAllocations(String portfolioId,
        LocalDate startDay, LocalDate endDay) {
        return getAllocations(portfolioId, startDay, endDay, 1);
    }

    private Mono<List<OASPortfolioAllocation>> getAllocations(String portfolioId,
        LocalDate startDay, LocalDate endDay, Integer limit) {
        return allocationsApi.listPortfolioAllocations(portfolioId, null,
                null, limit, null, null, startDay, endDay)
            .filter(Objects::nonNull)
            .map(PaginatedOASPortfolioAllocationList::getResults)
            .filter(Objects::nonNull);
    }

    private Mono<List<OASAllocationCreateRequest>> generateAllocations(
        Map<String, Map<LocalDate, Double>> priceDayByAssetKey,
        Pair<List<LocalDate>, List<OASAllocationPositionCreateRequest>> dayPositions) {

        List<OASAllocationPositionCreateRequest> portfolioPositions = List.copyOf(dayPositions.getSecond());

        double totalTrade = calculateTrades(portfolioPositions);
        double initialCash = 10_000d;
        double cash = initialCash - totalTrade;
        return Mono.just(generateAllocations(priceDayByAssetKey, dayPositions, initialCash, cash, totalTrade));
    }

    private Mono<List<OASPortfolioAllocation>> upsertAllocations(String portfolioId,
        List<OASAllocationCreateRequest> allocations) {
        return Flux.fromIterable(allocations)
            .flatMap(a -> customIntegrationApiService.createPortfolioAllocation(portfolioId, a, null, null, null))
            .collectList().doOnSuccess(
                created -> log.info("Successfully upserted investment portfolio allocation: count {}", created.size()))
            .doOnError(throwable -> {
                if (throwable instanceof WebClientResponseException ex) {
                    log.error("Failed to upsert investment portfolio allocation: status={}, body={}",
                        ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
                } else {
                    log.error("Failed to upsert investment portfolio allocation", throwable);
                }
            });
    }

    private List<OASAllocationCreateRequest> generateAllocations(Map<String, Map<LocalDate, Double>> priceDayByAssetKey,
        Pair<List<LocalDate>, List<OASAllocationPositionCreateRequest>> dayPositions, double initialCash, double cash,
        double totalTrade) {
        List<OASAllocationPositionCreateRequest> portfolioPositions = List.copyOf(dayPositions.getSecond());

        return dayPositions.getFirst().stream().map(d -> {
            double newTrades = calculateTrades(portfolioPositions);
            OASAllocationCreateRequest allocationDataRequest = new OASAllocationCreateRequest().invested(
                    roundPrice(initialCash)).tradeTotal(roundPrice(totalTrade)).balance(roundPrice(newTrades + cash))
                .earnings(roundPrice(newTrades - totalTrade)).cashActive(roundPrice(cash)).valuationDate(d).positions(
                    portfolioPositions.stream().map(p -> new OASAllocationPositionCreateRequest().asset(p.getAsset())
                        .shares(roundPrice(p.getShares())).price(roundPrice(p.getPrice()))).toList());
            portfolioPositions.forEach(p -> {
                Optional<Double> price = getPrice(assetKey(p.getAsset()), priceDayByAssetKey, d);
                price.ifPresent(p::setPrice);
                if (price.isEmpty()) {
                    log.warn("No price found for asset {} on day {}", p.getAsset(), d);
                }
            });
            return allocationDataRequest;
        }).filter(Objects::nonNull).toList();
    }

    @Nonnull
    private Mono<Pair<List<LocalDate>, List<OASAllocationPositionCreateRequest>>> orderPositions(UUID portfolioId,
        List<LocalDate> days, ModelPortfolio model, Map<String, Map<LocalDate, Double>> priceDayByAssetKey,
        double initialCash) {
        return days.stream().map(day -> Pair.of(day, model.getAllocations().stream().map(
                a -> getPrice(a.asset().getKeyString(), priceDayByAssetKey, day).map(
                    price -> new OASAllocationPositionCreateRequest().asset(a.asset().getAssetMap())
                        .shares((initialCash * a.weight()) / price).price(price))).toList()))
            .filter(al -> al.getSecond().stream().allMatch(Optional::isPresent))
            .map(al -> Pair.of(al.getFirst(), al.getSecond().stream().map(Optional::get).toList())).findFirst()
            .map(portfolioPositionsOfDay -> {
                LocalDate startDay = portfolioPositionsOfDay.getFirst();
                List<OASAllocationPositionCreateRequest> positions = portfolioPositionsOfDay.getSecond();
                return Flux.fromIterable(positions).flatMap(
                    p -> investmentApi.listOrders(null, assetKey(p.getAsset()), null, null, null, null, null, null,
                            null, null, null, portfolioId.toString(), null).filter(Objects::nonNull)
                        .map(PaginatedOASOrderList::getResults).filter(Objects::nonNull)
                        .flatMap(r -> r.isEmpty() ? Mono.empty() : Mono.just(r.getFirst())).switchIfEmpty(
                            investmentApi.createOrder(
                                    new OASCreateOrderRequest()
                                        .asset(p.getAsset())
                                        .orderType(OrderTypeEnum.BUY)
                                        .portfolio(portfolioId)
                                        .status(StatusA7dEnum.COMPLETED)
                                        .method(Method570Enum.MARKET)
                                        .completed(startDay.atTime(LocalTime.MIDNIGHT).atOffset(ZoneOffset.UTC))
                                        .currency(currency(p.getAsset())).shares(roundPrice(p.getShares()))
                                        .priceAvg(roundPrice(p.getPrice())), null, null, null).doOnSuccess(
                                    created -> log.info("Successfully upserted investment portfolio allocation"))
                                .doOnError(throwable -> {
                                    if (throwable instanceof WebClientResponseException ex) {
                                        log.error(
                                            "Failed to upsert investment portfolio allocation: status={}, body={}",
                                            ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
                                    } else {
                                        log.error("Failed to upsert investment portfolio allocation", throwable);
                                    }
                                }))).collectList().flatMap(o -> Mono.just(
                    Pair.of(days.stream().filter(d -> d.isAfter(startDay) || d.equals(startDay)).toList(), positions)));
            }).orElse(Mono.empty());
    }

    private Optional<Double> getPrice(String assetKey, Map<String, Map<LocalDate, Double>> priceDayByAssetKey,
        LocalDate startAllocation) {
        Map<LocalDate, Double> priceByDay = priceDayByAssetKey.get(assetKey);
        return Optional.ofNullable(priceByDay).map(m -> m.get(startAllocation));
    }

    private Mono<Map<String, Map<LocalDate, Double>>> getPriceDayByAssetKey(ModelPortfolio model, LocalDate startDat,
        LocalDate endDay) {

        return Flux.fromIterable(model.getAllocations()).flatMap(a -> {
            ModelAsset asset = a.asset();
            return Objects.requireNonNull(
                assetUniverseApi.listAssetClosePrices(asset.getCurrency(), startDat, endDay, null, null, null, null,
                    asset.getIsin(), null, asset.getMarket(), null, null).map(PaginatedOASPriceList::getResults).map(
                    l -> Map.of(asset.getKeyString(), l.stream()
                        .collect(Collectors.toMap(dp -> dp.getDatetime().toLocalDate(), OASPrice::getAmount)))));
        }).collectList().map(maps -> {
            Map<String, Map<LocalDate, Double>> dayPriceByAssetKey = new HashMap<>();
            maps.forEach(dayPriceByAssetKey::putAll);
            return dayPriceByAssetKey;
        });
    }

    private Mono<ModelPortfolio> getPortfolioModel(PortfolioList portfolio, List<PortfolioProduct> portfolioProducts,
        Map<UUID, Asset> assetByUuid) {

        return Mono.just(portfolioProducts.stream().filter(m -> m.getUuid().equals(portfolio.getProduct())).findFirst()
            .map(PortfolioProduct::getModelPortfolio).map(m -> ModelPortfolio.builder().cashWeight(m.getCashWeight())
                .allocations(m.getAllocation().stream().map(a -> new Allocation(
                    new ModelAsset(a.getAsset().getIsin(), a.getAsset().getMarket(), a.getAsset().getCurrency()),
                    a.getWeight())).toList()).build()).orElse(ModelPortfolio.builder().cashWeight(0.6).allocations(
                    assetByUuid.values().stream().limit(2)
                        .map(a -> new Allocation(new ModelAsset(a.getIsin(), a.getMarket(), a.currency()), 0.2)).toList())
                .build()));
    }

    private String assetKey(Map<String, Object> params) {
        return isin(params) + "_" + market(params) + "_" + currency(params);
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

    private static Double calculateTrades(List<OASAllocationPositionCreateRequest> portfolioPositions) {
        return portfolioPositions.stream().map(a -> a.getPrice() * a.getShares()).reduce(0.0, Double::sum);
    }

    public Mono<Deposit> createDepositAllocation(Deposit deposit) {
        String portfolioId = deposit.getPortfolio().toString();
        LocalDate valuationDate = Optional.ofNullable(deposit.getCompletedAt()).map(OffsetDateTime::toLocalDate)
            .orElse(LocalDate.now());
        return getAllocations(portfolioId, valuationDate.minusDays(4), valuationDate.plusDays(5), 10)
            .filter(Predicate.not(l -> l.stream().filter(a -> CollectionUtils.isEmpty(a.getPositions()))
                .toList().isEmpty()))
            .switchIfEmpty(upsertAllocations(portfolioId, List.of(
                new OASAllocationCreateRequest()
                    .cashActive(deposit.getAmount())
                    .valuationDate(valuationDate)))
                .onErrorResume(ex -> Mono.empty())
            )
            .onErrorResume(ex -> {
                log.warn("Failed to create deposit allocation", ex);
                return Mono.empty();
            })
            .map(l -> deposit);
    }

}
