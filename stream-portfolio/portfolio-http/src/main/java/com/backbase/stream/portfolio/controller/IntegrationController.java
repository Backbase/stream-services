package com.backbase.stream.portfolio.controller;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import com.backbase.stream.portfolio.api.IntegrationApiApi;
import com.backbase.stream.portfolio.configuration.PortfolioSagaProperties;
import com.backbase.stream.portfolio.model.Region;
import com.backbase.stream.portfolio.model.RegionBundle;
import com.backbase.stream.portfolio.saga.region.RegionBundleSaga;
import com.backbase.stream.portfolio.saga.region.RegionBundleTask;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Integration Controller.
 * 
 * @author Vladimir Kirchev
 *
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class IntegrationController implements IntegrationApiApi {

	private final PortfolioSagaProperties portfolioSagaProperties;
	private final RegionBundleSaga regionBundleSaga;

	@Override
	public Mono<ResponseEntity<Flux<RegionBundle>>> createPortfiolioRegionsBatch(Flux<RegionBundle> regionBundle,
			ServerWebExchange exchange) {

		Flux<RegionBundle> flux = regionBundle.map(RegionBundleTask::new)
				.flatMap(regionBundleSaga::executeTask, portfolioSagaProperties.getTaskExecutors())
				.map(RegionBundleTask::getData).doOnNext(actual -> log.info("Finished Ingestion of region, name: {}",
						Optional.ofNullable(actual.getRegion()).map(Region::getName).orElse("null")));

		return Mono.just(ResponseEntity.ok(flux));
	}

}
