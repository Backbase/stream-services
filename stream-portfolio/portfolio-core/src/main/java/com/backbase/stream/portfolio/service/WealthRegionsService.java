package com.backbase.stream.portfolio.service;

import com.backbase.stream.portfolio.model.RegionBundle;

import reactor.core.publisher.Flux;

/**
 * WealthRegionsService.
 * 
 * @author Vladimir Kirchev
 *
 */
public interface WealthRegionsService {

	/**
	 * Ingest Region Bundles.
	 * 
	 * @param regionBundle The Flux of {@code RegionBundle} to be ingested.
	 * @return The Flux of ingested {@code RegionBundle}.
	 */
	Flux<RegionBundle> ingestRegionBundles(Flux<RegionBundle> regionBundle);
}
