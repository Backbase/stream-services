package com.backbase.stream.portfolio.controller;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import com.backbase.stream.portfolio.api.PortfoliosApi;
import com.backbase.stream.portfolio.configuration.PortfolioSagaProperties;
import com.backbase.stream.portfolio.model.Region;
import com.backbase.stream.portfolio.model.RegionBundle;
import com.backbase.stream.portfolio.model.WealthBundle;
import com.backbase.stream.portfolio.saga.portfolio.PortfolioSaga;
import com.backbase.stream.portfolio.saga.portfolio.PortfolioTask;
import com.backbase.stream.portfolio.saga.region.RegionBundleSaga;
import com.backbase.stream.portfolio.saga.region.RegionBundleTask;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class PortfoliosController implements PortfoliosApi {

	private final PortfolioSagaProperties portfolioSagaProperties;
	private final PortfolioSaga portfolioSaga;
	private final RegionBundleSaga regionBundleSaga;

	@Override
	public Mono<ResponseEntity<Flux<WealthBundle>>> createPortfolios(Flux<WealthBundle> wealthBundle,
			ServerWebExchange exchange) {
		Flux<WealthBundle> flux = wealthBundle.map(PortfolioTask::new)
				.flatMap(portfolioSaga::executeTask, portfolioSagaProperties.getTaskExecutors())
				.map(PortfolioTask::getData)
				.doOnNext(actual -> log.info("Finished Ingestion of portfolio, wealthBundle: {}", actual));

		return Mono.just(ResponseEntity.ok(flux));
	}

	@Override
	public Mono<ResponseEntity<Flux<RegionBundle>>> createPortfioliosRegionsBatch(Flux<RegionBundle> regionBundle,
			ServerWebExchange exchange) {

		Flux<RegionBundle> flux = regionBundle.map(RegionBundleTask::new)
				.flatMap(regionBundleSaga::executeTask, portfolioSagaProperties.getTaskExecutors())
				.map(RegionBundleTask::getData).doOnNext(actual -> log.info("Finished Ingestion of region, name: {}",
						Optional.ofNullable(actual.getRegion()).map(Region::getName).orElse("null")));

		return Mono.just(ResponseEntity.ok(flux));
	}

}
