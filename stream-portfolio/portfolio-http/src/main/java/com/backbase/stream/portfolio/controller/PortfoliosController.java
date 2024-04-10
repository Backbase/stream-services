package com.backbase.stream.portfolio.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import com.backbase.stream.portfolio.PortfoliosApi;
import com.backbase.stream.portfolio.model.AllocationBundle;
import com.backbase.stream.portfolio.model.AssetClassBundle;
import com.backbase.stream.portfolio.model.HierarchyBundle;
import com.backbase.stream.portfolio.model.InstrumentBundle;
import com.backbase.stream.portfolio.model.Portfolio;
import com.backbase.stream.portfolio.model.Position;
import com.backbase.stream.portfolio.model.RegionBundle;
import com.backbase.stream.portfolio.model.SubPortfolioBundle;
import com.backbase.stream.portfolio.model.TransactionBundle;
import com.backbase.stream.portfolio.model.TransactionCategory;
import com.backbase.stream.portfolio.model.ValuationsBundle;
import com.backbase.stream.portfolio.model.WealthBundle;
import com.backbase.stream.portfolio.service.InstrumentIngestionService;
import com.backbase.stream.portfolio.service.PortfolioIngestionService;
import com.backbase.stream.portfolio.service.PortfolioService;
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
    private final PortfolioIngestionService portfolioIngestionReactiveService;
    private final InstrumentIngestionService instrumentIngestionReactiveService;

    @Override
    public Mono<ResponseEntity<Flux<WealthBundle>>> createPortfolios(Flux<WealthBundle> wealthBundle,
            ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(portfolioReactiveService.ingestWealthBundles(wealthBundle)));
    }

    @Override
    public Mono<ResponseEntity<Flux<RegionBundle>>> createPortfioliosRegionsBatch(Flux<RegionBundle> regionBundle,
            ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(instrumentIngestionReactiveService.ingestRegionBundles(regionBundle)));
    }

    @Override
    public Mono<ResponseEntity<Flux<AllocationBundle>>> createAllocationsBatch(Flux<AllocationBundle> allocationBundle,
            ServerWebExchange exchange) {
        return Mono.just(ResponseEntity
                .ok(portfolioIngestionReactiveService.ingestPortfolioAllocationBundles(allocationBundle)));
    }

    @Override
    public Mono<ResponseEntity<Flux<AssetClassBundle>>> createPortfioliosAssetClassesBatch(
            Flux<AssetClassBundle> assetClassBundle, ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(instrumentIngestionReactiveService.ingestWealthAssets(assetClassBundle)));
    }

    @Override
    public Mono<ResponseEntity<Flux<Portfolio>>> createPortfioliosBatch(Flux<Portfolio> portfolio,
            ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(portfolioIngestionReactiveService.ingestWealthPortfolios(portfolio)));
    }

    @Override
    public Mono<ResponseEntity<Flux<SubPortfolioBundle>>> createSubPortfioliosBatch(
            Flux<SubPortfolioBundle> subPortfolioBundle, ServerWebExchange exchange) {
        return Mono.just(ResponseEntity
                .ok(portfolioIngestionReactiveService.ingestWealthSubPortfolios(subPortfolioBundle)));
    }

    @Override
    public Mono<ResponseEntity<Flux<InstrumentBundle>>> createInstrumentsBatch(Flux<InstrumentBundle> instrumentBundle,
            ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(instrumentIngestionReactiveService.ingestInstruments(instrumentBundle)));
    }

    @Override
    public Mono<ResponseEntity<Flux<TransactionCategory>>> createTransactionCategoriesBatch(
            Flux<TransactionCategory> transactionCategory, ServerWebExchange exchange) {
        return Mono.just(ResponseEntity
                .ok(portfolioIngestionReactiveService.ingestTransactionCategories(transactionCategory)));
    }

    @Override
    public Mono<ResponseEntity<Flux<ValuationsBundle>>> createValuationsBatch(Flux<ValuationsBundle> valuationsBundle,
            ServerWebExchange exchange) {
        return Mono
                .just(ResponseEntity.ok(portfolioIngestionReactiveService.ingestValuationsBundles(valuationsBundle)));
    }

    @Override
    public Mono<ResponseEntity<Flux<HierarchyBundle>>> createHierarchiesBatch(Flux<HierarchyBundle> hierarchyBundles,
            ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(portfolioIngestionReactiveService.ingestHierarchyBundles(hierarchyBundles)));
    }

    @Override
    public Mono<ResponseEntity<Flux<Position>>> createPositionsBatch(Flux<Position> position,
            ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(portfolioIngestionReactiveService.ingestPositions(position)));
    }

    @Override
    public Mono<ResponseEntity<Flux<TransactionBundle>>> createTransactionsBatch(
            Flux<TransactionBundle> transactionBundle, ServerWebExchange exchange) {
        return Mono
                .just(ResponseEntity.ok(portfolioIngestionReactiveService.ingestTransactionBundles(transactionBundle)));
    }
}
