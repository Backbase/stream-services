package com.backbase.stream.portfolio.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import com.backbase.stream.portfolio.api.PortfoliosApi;
import com.backbase.stream.portfolio.model.RegionBundle;
import com.backbase.stream.portfolio.model.WealthBundle;
import com.backbase.stream.portfolio.service.PortfolioService;
import com.backbase.stream.portfolio.service.RegionService;

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
	private final RegionService regionReactiveService;

	@Override
	public Mono<ResponseEntity<Flux<WealthBundle>>> createPortfolios(Flux<WealthBundle> wealthBundle,
			ServerWebExchange exchange) {
		return Mono.just(ResponseEntity.ok(portfolioReactiveService.ingestWealthBundles(wealthBundle)));
	}

	@Override
	public Mono<ResponseEntity<Flux<RegionBundle>>> createPortfioliosRegionsBatch(Flux<RegionBundle> regionBundle,
			ServerWebExchange exchange) {
		return Mono.just(ResponseEntity.ok(regionReactiveService.ingestRegionBundles(regionBundle)));
	}

}
