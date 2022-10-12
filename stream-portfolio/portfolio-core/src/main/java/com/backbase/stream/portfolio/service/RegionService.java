package com.backbase.stream.portfolio.service;

import com.backbase.stream.portfolio.model.RegionBundle;

import reactor.core.publisher.Flux;

public interface RegionService {

	Flux<RegionBundle> ingestRegionBundles(Flux<RegionBundle> regionBundle);
}
