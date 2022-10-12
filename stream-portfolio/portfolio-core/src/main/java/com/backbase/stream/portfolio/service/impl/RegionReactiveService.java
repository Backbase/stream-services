package com.backbase.stream.portfolio.service.impl;

import java.util.Optional;

import com.backbase.stream.portfolio.configuration.PortfolioSagaProperties;
import com.backbase.stream.portfolio.model.Region;
import com.backbase.stream.portfolio.model.RegionBundle;
import com.backbase.stream.portfolio.saga.region.RegionBundleSaga;
import com.backbase.stream.portfolio.saga.region.RegionBundleTask;
import com.backbase.stream.portfolio.service.RegionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * Reactive implementation of {@code RegionService}.
 * 
 * @author Vladimir Kirchev
 *
 */
@Slf4j
@RequiredArgsConstructor
public class RegionReactiveService implements RegionService {

	private final PortfolioSagaProperties portfolioSagaProperties;
	private final RegionBundleSaga regionBundleSaga;

	@Override
	public Flux<RegionBundle> ingestRegionBundles(Flux<RegionBundle> regionBundle) {
		return regionBundle.map(RegionBundleTask::new)
				.flatMap(regionBundleSaga::executeTask, portfolioSagaProperties.getTaskExecutors())
				.map(RegionBundleTask::getData).doOnNext(actual -> log.info("Finished Ingestion of region, name: {}",
						Optional.ofNullable(actual.getRegion()).map(Region::getName).orElse("null")));
	}

}
