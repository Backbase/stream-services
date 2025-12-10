package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.v1.AllocationsApi;
import com.backbase.investment.api.service.v1.AssetUniverseApi;
import com.backbase.investment.api.service.v1.InvestmentProductsApi;
import com.backbase.investment.api.service.v1.PortfolioApi;
import com.backbase.investment.api.service.v1.model.OASAllocationCreateRequest;
import com.backbase.investment.api.service.v1.model.OASAllocationPositionCreateRequest;
import com.backbase.investment.api.service.v1.model.OASBulkAllocationDataRequest;
import com.backbase.investment.api.service.v1.model.OASPortfolioAllocation;
import com.backbase.investment.api.service.v1.model.PaginatedOASPortfolioAllocationList;
import com.backbase.investment.api.service.v1.model.PortfolioList;
import com.backbase.stream.investment.Asset;
import com.backbase.stream.investment.AssetPrice;
import com.backbase.stream.investment.InvestmentAssetData;
import com.backbase.stream.investment.ModelPortfolio;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private final AllocationsApi allocationsApi;
    private final AssetUniverseApi assetUniverseApi;
    private final CustomIntegrationApiService customIntegrationApiService;


    public Mono<List<OASPortfolioAllocation>> generateAllocations(PortfolioList portfolio,
        List<ModelPortfolio> modelPortfolios, InvestmentAssetData investmentAssetData) {

        LocalDate today = LocalDate.now();
        LocalDate startAllocation = Optional.ofNullable(portfolio.getActivated())
            .map(OffsetDateTime::toLocalDate)
            .orElse(today).plusDays(1);

        List<LocalDate> days = Stream.iterate(today.minusDays(2),
                offsetDate -> offsetDate.isBefore(today),
                offsetDateTime -> offsetDateTime.plusDays(10))
            .toList();

        List<OASBulkAllocationDataRequest> allocations = days.stream().map(d -> new OASBulkAllocationDataRequest()
            .portfolio(portfolio.getUuid())
            .cashActive(10_000d)
            .valuationDate(d.toString())
        ).toList();

        List<Asset> assets = investmentAssetData.getAssets();
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

        Map<String, AssetPrice> priceByAsset = investmentAssetData.getPriceByAsset();

        /*return allocationsApi.createBulkAllocations(allocations)
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
            .flatMapIterable(a -> allocations2)
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

}
