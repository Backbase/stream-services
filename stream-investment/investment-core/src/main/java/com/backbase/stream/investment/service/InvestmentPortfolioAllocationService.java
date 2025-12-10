package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.v1.AllocationsApi;
import com.backbase.investment.api.service.v1.AssetUniverseApi;
import com.backbase.investment.api.service.v1.InvestmentProductsApi;
import com.backbase.investment.api.service.v1.PortfolioApi;
import com.backbase.investment.api.service.v1.model.OASAllocationCreateRequest;
import com.backbase.investment.api.service.v1.model.OASAllocationPositionCreateRequest;
import com.backbase.investment.api.service.v1.model.OASPortfolioAllocation;
import com.backbase.investment.api.service.v1.model.PaginatedOASPortfolioAllocationList;
import com.backbase.investment.api.service.v1.model.PortfolioList;
import com.backbase.stream.investment.Asset;
import com.backbase.stream.investment.InvestmentAssetData;
import com.backbase.stream.investment.ModelPortfolio;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClientResponseException;
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

    private final SecureRandom random = new SecureRandom();

    private final AllocationsApi allocationsApi;
    private final AssetUniverseApi assetUniverseApi;
    private final CustomIntegrationApiService customIntegrationApiService;


    public Mono<List<OASPortfolioAllocation>> generateAllocations(PortfolioList portfolio,
        List<ModelPortfolio> modelPortfolios, InvestmentAssetData investmentAssetData) {

        LocalDate today = LocalDate.now();
        LocalDate startAllocation = Optional.ofNullable(portfolio.getActivated())
            .map(OffsetDateTime::toLocalDate)
            .orElse(today).plusDays(1);

        List<LocalDate> days = Stream.iterate(startAllocation,
                offsetDate -> offsetDate.isBefore(today),
                offsetDateTime -> offsetDateTime.plusDays(10))
            .toList();

        Map<UUID, Asset> assetByUuid = investmentAssetData.getAssetByUuid();

        AtomicReference<Double> invested = new AtomicReference<>(10_000d);

        List<OASAllocationPositionCreateRequest> defaultAssets = assetByUuid.values().stream().limit(2)
            .map(a -> new OASAllocationPositionCreateRequest()
                .asset(a.getAssetMap())
                .shares(invested.get() * 0.2)
                .price(random.nextDouble(10, 120)))
            .toList();

        List<OASAllocationPositionCreateRequest> portfolioPositions = modelPortfolios.stream()
            .filter(m -> m.getUuid().equals(portfolio.getProduct()))
            .findFirst()
            .map(ModelPortfolio::getAllocations)
            .map(as -> as.stream()
                .map(a -> new OASAllocationPositionCreateRequest()
                    .asset(a.asset().getAssetMap())
                    .shares(invested.get() * a.weight())
                    .price(random.nextDouble(10, 120))
                )
                .toList()
            )
            .orElse(defaultAssets);

        /*List<OASBulkAllocationDataRequest> allocationBulk = days.stream()
            .map(d -> {
                    OASBulkAllocationDataRequest positions = new OASBulkAllocationDataRequest()
                        .portfolio(portfolio.getUuid())
                        .invested(invested.get())
                        .tradeTotal(1_000d)
                        .balance(100d)
                        .earnings(10d)
                        .cashActive(11_000d)
                        .valuationDate(d.toString())
                        .positions(portfolioPositions.stream()
                            .map(p -> new OASAllocationPositionCreateRequest()
                                .asset(p.getAsset())
                                .shares(p.getShares())
                                .price(Math.round(p.getPrice() * 1000000.0) / 1000000.0))
                            .toList());
                    double price = random.nextDouble(10, 120);
                    portfolioPositions.forEach(p -> p.setPrice(price));
                    return positions;
                }
            ).toList();*/

        double totalTrade = calculateBalance(portfolioPositions);
        AtomicReference<Double> balance = new AtomicReference<>(totalTrade);
        List<OASAllocationCreateRequest> allocations = days.stream()
            .map(d -> {
                double newBalance = calculateBalance(portfolioPositions);
                OASAllocationCreateRequest allocationDataRequest = new OASAllocationCreateRequest()
                        .invested(roundPrice(invested.get()))
                        .tradeTotal(roundPrice(totalTrade))
                        .balance(roundPrice(newBalance))
                        .earnings(roundPrice(balance.get() - newBalance))
                        .cashActive(roundPrice(invested.get() - totalTrade))
                        .valuationDate(d)
                        .positions(portfolioPositions.stream()
                            .map(p -> new OASAllocationPositionCreateRequest()
                                .asset(p.getAsset())
                                .shares(p.getShares())
                                .price(roundPrice(p.getPrice())))
                            .toList());
                    double price = random.nextDouble(10, 120);
                    portfolioPositions.forEach(p -> p.setPrice(price));
                    return allocationDataRequest;
                }
            ).toList();

        /*List<Asset> assets = investmentAssetData.getAssets();
        Asset first = assets.getFirst();
        List<OASAllocationCreateRequest> allocations2 = days.stream().map(d -> new OASAllocationCreateRequest()
            .cashActive(10d)
            .valuationDate(d)
            .tradeTotal(10_000d)
            .invested(9_000d)
            .balance(1_100d)
            .earnings(100d)
            .positions(List.of(new OASAllocationPositionCreateRequest()
                .asset(first.getAssetMap())
                .price(100D)
                .shares(11d)))
        ).toList();

        Map<String, AssetPrice> priceByAsset = investmentAssetData.getPriceByAsset();*/

        /*return allocationsApi.listPortfolioAllocations(portfolio.getUuid().toString(), null, null, null, null, null,
                null, null)
            .flatMapIterable(PaginatedOASPortfolioAllocationList::getResults)
            .flatMap(
                a -> allocationsApi.deletePortfolioAllocation(portfolio.getUuid().toString(), a.getValuationDate()))
            .collectList()
            .thenMany(allocationsApi.createBulkAllocations(allocationBulk))
//        allocationsApi.createBulkAllocations(allocations)
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
            });*/

        return allocationsApi.listPortfolioAllocations(portfolio.getUuid().toString(), null, null, null, null, null,
                null, null)
            .flatMapIterable(PaginatedOASPortfolioAllocationList::getResults)
            .flatMap(
                a -> allocationsApi.deletePortfolioAllocation(portfolio.getUuid().toString(), a.getValuationDate()))
            .collectList()
            .flatMapIterable(a -> allocations)
//        return Flux.fromIterable(allocations2)
            .flatMap(a -> customIntegrationApiService.createPortfolioAllocation(
                portfolio.getUuid().toString(), a, null, null, null))
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

        /*return eur
            .map(PaginatedAssetList::getResults)
            .flatMap(r -> {
                OASAllocationCreateRequest oaSAllocationCreateRequest = new OASAllocationCreateRequest();
                oaSAllocationCreateRequest.valuationDate(LocalDate.now().minusDays(1));
                oaSAllocationCreateRequest.cashActive(10_000d);
                OASAllocationPositionCreateRequest p1 = new OASAllocationPositionCreateRequest();
                p1.asset(Asset.fromModel(r.get(0)).getAssetMap());
                p1.price(111D);
                p1.shares(11d);
                oaSAllocationCreateRequest.positions(List.of(p1));
                return customIntegrationApiService.createPortfolioAllocation(
                    "bc5d5af1-ebff-44b9-ba80-77205736ca63", oaSAllocationCreateRequest, null, null, null);
            })
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
            });*/
    }

    private static double roundPrice(double value) {
        return Math.round(value * 1000000.0) / 1000000.0;
    }

    private static Double calculateBalance(List<OASAllocationPositionCreateRequest> portfolioPositions) {
        return portfolioPositions.stream()
            .map(a -> a.getPrice() * a.getShares()).reduce(0.0, Double::sum);
    }

}
