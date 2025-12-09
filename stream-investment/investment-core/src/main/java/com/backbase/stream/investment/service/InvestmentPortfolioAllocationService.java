package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.v1.AllocationsApi;
import com.backbase.investment.api.service.v1.AssetUniverseApi;
import com.backbase.investment.api.service.v1.InvestmentProductsApi;
import com.backbase.investment.api.service.v1.PortfolioApi;
import com.backbase.investment.api.service.v1.model.OASAllocationCreateRequest;
import com.backbase.investment.api.service.v1.model.OASAllocationPositionCreateRequest;
import com.backbase.investment.api.service.v1.model.OASPortfolioAllocation;
import com.backbase.investment.api.service.v1.model.PaginatedAssetList;
import com.backbase.investment.api.service.v1.model.PortfolioList;
import com.backbase.stream.investment.Asset;
import java.time.LocalDate;
import java.util.List;
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


    public Mono<OASPortfolioAllocation> generateAllocations(PortfolioList portfolioList, List<Asset> assets) {

        Mono<PaginatedAssetList> eur = assetUniverseApi.listAssets(null, null, null, "EUR", null,
            null, null, null, null, 1, null, null, null,
            null, null, null, null);

        return eur
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
            });
    }

}
