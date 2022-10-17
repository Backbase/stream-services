package com.backbase.stream.portfolio.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import com.backbase.stream.portfolio.api.PortfoliosApi;
import com.backbase.stream.portfolio.model.AllocationBundle;
import com.backbase.stream.portfolio.model.AssetClassBundle;
import com.backbase.stream.portfolio.model.Portfolio;
import com.backbase.stream.portfolio.model.RegionBundle;
import com.backbase.stream.portfolio.model.SubPortfolioBundle;
import com.backbase.stream.portfolio.model.WealthBundle;
import com.backbase.stream.portfolio.service.PortfolioService;
import com.backbase.stream.portfolio.service.WealthAssetsService;
import com.backbase.stream.portfolio.service.WealthPortfolioAllocationsService;
import com.backbase.stream.portfolio.service.WealthPortfolioService;
import com.backbase.stream.portfolio.service.WealthRegionsService;
import com.backbase.stream.portfolio.service.WealthSubPortfolioService;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Portfolios Controller.
 * 
 * @author Vladimir Kirchev
 *
 */
@RestController
@RequiredArgsConstructor
public class PortfoliosController implements PortfoliosApi {

    private final PortfolioService portfolioReactiveService;
    private final WealthRegionsService wealthRegionsReactiveService;
    private final WealthPortfolioAllocationsService wealthPortfolioAllocationsReactiveService;
    private final WealthAssetsService wealthAssetsReactiveService;
    private final WealthPortfolioService wealthPortfolioReactiveService;
    private final WealthSubPortfolioService wealthSubPortfolioReactiveService;

    @Override
    public Mono<ResponseEntity<Flux<WealthBundle>>> createPortfolios(Flux<WealthBundle> wealthBundle,
            ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(portfolioReactiveService.ingestWealthBundles(wealthBundle)));
    }

    @Override
    public Mono<ResponseEntity<Flux<RegionBundle>>> createPortfioliosRegionsBatch(Flux<RegionBundle> regionBundle,
            ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(wealthRegionsReactiveService.ingestRegionBundles(regionBundle)));
    }

    @Override
    public Mono<ResponseEntity<Flux<AllocationBundle>>> createAllocationsBatch(Flux<AllocationBundle> allocationBundle,
            ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(
                wealthPortfolioAllocationsReactiveService.ingestPortfolioAllocationBundles(allocationBundle)));
    }

    @Override
    public Mono<ResponseEntity<Flux<AssetClassBundle>>> createPortfioliosAssetClassesBatch(
            Flux<AssetClassBundle> assetClassBundle, ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(wealthAssetsReactiveService.ingestWealthAssets(assetClassBundle)));
    }

    @Override
    public Mono<ResponseEntity<Flux<Portfolio>>> createPortfioliosBatch(Flux<Portfolio> portfolio,
            ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(wealthPortfolioReactiveService.ingestWealthPortfolios(portfolio)));
    }

    @Override
    public Mono<ResponseEntity<Flux<SubPortfolioBundle>>> createSubPortfioliosBatch(
            Flux<SubPortfolioBundle> subPortfolioBundle, ServerWebExchange exchange) {
        return Mono.just(
                ResponseEntity.ok(wealthSubPortfolioReactiveService.ingestWealthSubPortfolios(subPortfolioBundle)));
    }

}
