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
import com.backbase.investment.api.service.v1.model.ProductTypeEnum;
import com.backbase.investment.api.service.v1.model.StatusA7dEnum;
import com.backbase.investment.api.service.v1.model.InvestorModelPortfolio;
import com.backbase.stream.configuration.IngestConfigProperties;
import com.backbase.stream.investment.Allocation;
import com.backbase.stream.investment.Asset;
import com.backbase.stream.investment.InvestmentAssetData;
import com.backbase.stream.investment.ModelAsset;
import com.backbase.stream.investment.ModelPortfolio;
import com.backbase.stream.investment.ProductPortfolio;
import com.backbase.stream.investment.model.InvestmentPortfolio;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
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

    private static final String ADIDAS_ISIN = "DE000A1EWWW0";
    private static final double DEFAULT_SELF_TRADING_ALLOCATION_WEIGHT = 0.2;
    private static final double DEFAULT_SELF_TRADING_CASH_WEIGHT = 0.6;
    private static final double DEFAULT_SAVINGS_PLAN_ALLOCATION_WEIGHT = 1.0;
    private static final double DEFAULT_SAVINGS_PLAN_CASH_WEIGHT = 0.0;
    private static final int DEFAULT_GENERIC_ALLOCATION_ASSET_COUNT = 2;

    /**
     * Savings-plan products no longer carry a model portfolio in BSJ bootstrap data; orders and allocations must
     * target a single underlying asset per product.
     */
    private static final Map<String, String> SAVINGS_PLAN_UNDERLYING_TICKER_BY_PRODUCT_NAME = Map.of(
        "Opportunity Horizon Fund", "LYQY",
        "Balanced Growth Fund", "VOO",
        "Safe Investment Fund", "EUNH"
    );

    private final AllocationsApi allocationsApi;
    private final AssetUniverseApi assetUniverseApi;
    private final InvestmentApi investmentApi;
    private final IngestConfigProperties ingestProperties;

    public Mono<Void> removeAllocations(PortfolioList portfolio) {
        String portfolioUuid = portfolio.getUuid().toString();
        return allocationsApi.listPortfolioAllocations(portfolioUuid, null, null, null, null, null, null, null)
            .flatMapIterable(PaginatedOASPortfolioAllocationList::getResults)
            .flatMap(a -> allocationsApi.deletePortfolioAllocation(portfolioUuid, a.getValuationDate())).collectList()
            .flatMap(v -> Mono.empty());
    }

    public Mono<List<OASPortfolioAllocation>> generateAllocations(InvestmentPortfolio investmentPortfolio,
        List<PortfolioProduct> portfolioProducts, InvestmentAssetData investmentAssetData) {
        return generateAllocations(investmentPortfolio, portfolioProducts, investmentAssetData, List.of());
    }

    public Mono<List<OASPortfolioAllocation>> generateAllocations(InvestmentPortfolio investmentPortfolio,
        List<PortfolioProduct> portfolioProducts, InvestmentAssetData investmentAssetData,
        List<ProductPortfolio> productTemplates) {
        PortfolioList portfolio = investmentPortfolio.getPortfolio();
        double initAmount = investmentPortfolio.getInitialCashOrDefault(
            ingestProperties.getAllocation().getDefaultAmount());
        return getPortfolioModel(portfolio, portfolioProducts, investmentAssetData.getAssetByUuid(), productTemplates)
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
                            orderPositions(portfolio.getUuid(), portfolio.getActivated(), workDays(startDay, endDay), m,
                                priceDayByAssetKey, initAmount)
                                .flatMap(dp -> generateAllocations(priceDayByAssetKey, dp, initAmount)))
                        .flatMap(allocations -> this.upsertAllocations(portfolio.getUuid().toString(), allocations)));
            })
            .onErrorResume(ex -> {
                log.warn("Portfolio allocation generation failed: portfolio={}", portfolio.getUuid(), ex);
                return Mono.empty();
            });
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
        Pair<List<LocalDate>, List<OASAllocationPositionCreateRequest>> dayPositions, double initialCash) {

        List<OASAllocationPositionCreateRequest> portfolioPositions = List.copyOf(dayPositions.getSecond());

        double totalTrade = calculateTrades(portfolioPositions);

        double cash = initialCash - totalTrade;
        return Mono.just(generateAllocations(priceDayByAssetKey, dayPositions, initialCash, cash, totalTrade));
    }

    private Mono<List<OASPortfolioAllocation>> upsertAllocations(String portfolioId,
        List<OASAllocationCreateRequest> allocations) {
        return Flux.fromIterable(allocations)
            .flatMap(a -> allocationsApi.createPortfolioAllocation(portfolioId, a, null, null, null),
                ingestProperties.getAllocation().getAllocationConcurrency())

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
        OffsetDateTime portfolioActivated, List<LocalDate> days, ModelPortfolio model,
        Map<String, Map<LocalDate, Double>> priceDayByAssetKey, double initialCash) {
        OffsetDateTime orderCompletedAt = WorkDayService.offsetAtBusinessDaysAfter(portfolioActivated, 1);
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
                                        .completed(orderCompletedAt)
                                        .currency(currency(p.getAsset())).shares(roundPrice(p.getShares()))
                                        .priceAvg(roundPrice(p.getPrice())), null, null, null)
                                .doOnSuccess(created -> log.info(
                                    "Created positions for portfolio : portfolio={}, asset={}, shares={}",
                                    portfolioId, assetKey(p.getAsset()), roundPrice(p.getShares())))
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
                        asset.getIsin(), null, asset.getMarket(), null, null)
                    .map(PaginatedOASPriceList::getResults)
                    .map(l -> Map.of(asset.getKeyString(), l.stream()
                        .collect(Collectors.toMap(dp -> dp.getDatetime().toLocalDate(), OASPrice::getAmount)))));
        }).collectList().map(maps -> {
            Map<String, Map<LocalDate, Double>> dayPriceByAssetKey = new HashMap<>();
            maps.forEach(dayPriceByAssetKey::putAll);
            return dayPriceByAssetKey;
        });
    }

    private Mono<ModelPortfolio> getPortfolioModel(PortfolioList portfolio, List<PortfolioProduct> portfolioProducts,
        Map<UUID, Asset> assetByUuid, List<ProductPortfolio> productTemplates) {
        return Mono.just(portfolioProducts.stream()
            .filter(m -> m.getUuid().equals(portfolio.getProduct()))
            .findFirst()
            .map(product -> resolvePortfolioModel(portfolio, product, assetByUuid, productTemplates))
            .orElseGet(() -> buildGenericDefaultModel(assetByUuid)));
    }

    private ModelPortfolio resolvePortfolioModel(PortfolioList portfolio, PortfolioProduct product,
        Map<UUID, Asset> assetByUuid, List<ProductPortfolio> productTemplates) {
        if (hasModelAllocations(product.getModelPortfolio())) {
            return buildModelPortfolioFromInvestorModel(product.getModelPortfolio());
        }
        if (isRoboProduct(product)) {
            Optional<ModelPortfolio> bootstrapModel = findBootstrapModelForProduct(product, productTemplates);
            if (bootstrapModel.isPresent()) {
                ModelPortfolio model = bootstrapModel.get();
                log.debug("Resolved robo model from bootstrap template: productUuid={}, name={}, allocationCount={}",
                    product.getUuid(), product.getName(), model.getAllocations().size());
                return model;
            }
        }
        if (isSavingsPlanProduct(product)) {
            Optional<ModelPortfolio> savingsPlanModel = buildSavingsPlanModel(product, assetByUuid);
            if (savingsPlanModel.isPresent()) {
                ModelPortfolio model = savingsPlanModel.get();
                log.debug("Resolved savings plan single-asset model: productUuid={}, name={}, underlyingIsin={}",
                    product.getUuid(), product.getName(), model.getAllocations().getFirst().asset().getIsin());
                return model;
            }
        }
        if (isSelfTradingProduct(product)) {
            return buildSelfTradingDefaultModel(portfolio, assetByUuid);
        }
        return buildGenericDefaultModel(assetByUuid);
    }

    private static Optional<ModelPortfolio> findBootstrapModelForProduct(PortfolioProduct product,
        List<ProductPortfolio> productTemplates) {
        if (CollectionUtils.isEmpty(productTemplates)) {
            return Optional.empty();
        }
        return productTemplates.stream()
            .filter(template -> matchesProductTemplate(product, template))
            .map(ProductPortfolio::getModelPortfolio)
            .filter(InvestmentPortfolioAllocationService::hasStreamModelAllocations)
            .findFirst();
    }

    private static boolean matchesProductTemplate(PortfolioProduct product, ProductPortfolio template) {
        if (product.getUuid() != null && template.getUuid() != null && product.getUuid().equals(template.getUuid())) {
            return true;
        }
        return product.getName() != null && product.getName().equals(template.getName());
    }

    private static boolean hasStreamModelAllocations(ModelPortfolio model) {
        return model != null && !CollectionUtils.isEmpty(model.getAllocations());
    }

    private static boolean isRoboProduct(PortfolioProduct product) {
        return ProductTypeEnum.ROBO_ADVISOR == product.getProductType();
    }

    private static boolean hasModelAllocations(InvestorModelPortfolio model) {
        return model != null && model.getAllocation() != null && !model.getAllocation().isEmpty();
    }

    private ModelPortfolio buildModelPortfolioFromInvestorModel(InvestorModelPortfolio model) {
        return ModelPortfolio.builder().cashWeight(model.getCashWeight())
            .allocations(model.getAllocation().stream().map(a -> new Allocation(
                new ModelAsset(a.getAsset().getIsin(), a.getAsset().getMarket(), a.getAsset().getCurrency()),
                a.getWeight())).toList()).build();
    }

    private ModelPortfolio buildGenericDefaultModel(Map<UUID, Asset> assetByUuid) {
        return ModelPortfolio.builder().cashWeight(DEFAULT_SELF_TRADING_CASH_WEIGHT).allocations(
            assetByUuid.values().stream().limit(DEFAULT_GENERIC_ALLOCATION_ASSET_COUNT)
                .map(a -> new Allocation(new ModelAsset(a.getIsin(), a.getMarket(), a.getCurrency()),
                    DEFAULT_SELF_TRADING_ALLOCATION_WEIGHT))
                .toList()).build();
    }

    private ModelPortfolio buildSelfTradingDefaultModel(PortfolioList portfolio, Map<UUID, Asset> assetByUuid) {
        String portfolioCurrency = Optional.ofNullable(portfolio.getCurrency())
            .orElse(ingestProperties.getPortfolio().getDefaultCurrency());
        List<Asset> currencyMatchedAssets = assetByUuid.values().stream()
            .filter(asset -> portfolioCurrency.equalsIgnoreCase(asset.getCurrency()))
            .toList();

        List<Asset> selectedAssets = new ArrayList<>();
        currencyMatchedAssets.stream()
            .filter(asset -> ADIDAS_ISIN.equals(asset.getIsin()))
            .findFirst()
            .ifPresent(selectedAssets::add);
        currencyMatchedAssets.stream()
            .filter(asset -> selectedAssets.stream().noneMatch(selected -> selected.getUuid().equals(asset.getUuid())))
            .limit(Math.max(0, DEFAULT_GENERIC_ALLOCATION_ASSET_COUNT - selectedAssets.size()))
            .forEach(selectedAssets::add);

        if (selectedAssets.isEmpty()) {
            return buildGenericDefaultModel(assetByUuid);
        }

        return ModelPortfolio.builder().cashWeight(DEFAULT_SELF_TRADING_CASH_WEIGHT)
            .allocations(selectedAssets.stream()
                .map(asset -> new Allocation(
                    new ModelAsset(asset.getIsin(), asset.getMarket(), asset.getCurrency()),
                    DEFAULT_SELF_TRADING_ALLOCATION_WEIGHT))
                .toList())
            .build();
    }

    private static boolean isSelfTradingProduct(PortfolioProduct product) {
        return ProductTypeEnum.SELF_TRADING == product.getProductType();
    }

    private static boolean isSavingsPlanProduct(PortfolioProduct product) {
        return ProductTypeEnum.SAVINGS_PLAN == product.getProductType();
    }

    private Optional<ModelPortfolio> buildSavingsPlanModel(PortfolioProduct product,
        Map<UUID, Asset> assetByUuid) {
        String productName = product.getName();
        if (productName == null) {
            log.warn("Savings plan product has no name; cannot resolve underlying asset: productUuid={}",
                product.getUuid());
            return Optional.empty();
        }
        String ticker = SAVINGS_PLAN_UNDERLYING_TICKER_BY_PRODUCT_NAME.get(productName);
        if (ticker == null) {
            log.warn("No underlying asset mapping for savings plan product: productUuid={}, name={}",
                product.getUuid(), productName);
            return Optional.empty();
        }
        Optional<Asset> underlyingAsset = assetByUuid.values().stream()
            .filter(asset -> ticker.equalsIgnoreCase(asset.getTicker()))
            .findFirst();
        if (underlyingAsset.isEmpty()) {
            log.warn(
                "Underlying asset not found in asset universe for savings plan: productUuid={}, name={}, ticker={}",
                product.getUuid(), productName, ticker);
            return Optional.empty();
        }
        Asset asset = underlyingAsset.get();
        return Optional.of(ModelPortfolio.builder()
            .cashWeight(DEFAULT_SAVINGS_PLAN_CASH_WEIGHT)
            .allocations(List.of(new Allocation(
                new ModelAsset(asset.getIsin(), asset.getMarket(), asset.getCurrency()),
                DEFAULT_SAVINGS_PLAN_ALLOCATION_WEIGHT)))
            .build());
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

    private static OASAllocationCreateRequest buildCashOnlyAllocation(double amount, LocalDate valuationDate) {
        double roundedAmount = roundPrice(amount);
        return new OASAllocationCreateRequest()
            .cashActive(roundedAmount)
            .tradeTotal(0.0)
            .balance(roundedAmount)
            .invested(roundedAmount)
            .earnings(0.0)
            .valuationDate(valuationDate);
    }

    public Mono<Deposit> createDepositAllocation(Deposit deposit) {
        String portfolioId = deposit.getPortfolio().toString();
        LocalDate valuationDate = Optional.ofNullable(deposit.getCompletedAt()).map(OffsetDateTime::toLocalDate)
            .orElse(LocalDate.now());
        return getAllocations(portfolioId, valuationDate.minusDays(4), valuationDate.plusDays(5), 10)
            .filter(Predicate.not(l -> l.stream().filter(a -> CollectionUtils.isEmpty(a.getPositions()))
                .toList().isEmpty()))
            .switchIfEmpty(upsertAllocations(portfolioId, List.of(buildCashOnlyAllocation(deposit.getAmount(),
                valuationDate)))
                .onErrorResume(ex -> Mono.empty())
            )
            .onErrorResume(ex -> {
                log.warn("Failed to create deposit allocation", ex);
                return Mono.empty();
            })
            .map(l -> deposit);
    }

}
