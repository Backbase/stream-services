package com.backbase.stream.portfolio.service.impl;

import java.util.List;
import java.util.Optional;
import com.backbase.stream.portfolio.configuration.PortfolioSagaProperties;
import com.backbase.stream.portfolio.model.Region;
import com.backbase.stream.portfolio.model.RegionBundle;
import com.backbase.stream.portfolio.service.InstrumentIntegrationService;
import com.backbase.stream.portfolio.service.WealthRegionsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * Reactive implementation of {@code WealthRegionsService}.
 * 
 * @author Vladimir Kirchev
 *
 */
@Slf4j
@RequiredArgsConstructor
public class WealthRegionsReactiveService implements WealthRegionsService {

    private final PortfolioSagaProperties portfolioSagaProperties;
    private final InstrumentIntegrationService instrumentIntegrationService;

    @Override
    public Flux<RegionBundle> ingestRegionBundles(Flux<RegionBundle> regionBundles) {
        return regionBundles.map(List::of)
                .flatMap(instrumentIntegrationService::upsertRegions, portfolioSagaProperties.getTaskExecutors())
                .flatMapIterable(i -> i).doOnNext(actual -> log.info("Finished Ingestion of region, name: {}",
                        Optional.ofNullable(actual.getRegion()).map(Region::getName).orElse("null")));
    }

}
