package com.backbase.stream.portfolio.service.impl;

import com.backbase.stream.portfolio.configuration.PortfolioSagaProperties;
import com.backbase.stream.portfolio.model.AssetClass;
import com.backbase.stream.portfolio.model.AssetClassBundle;
import com.backbase.stream.portfolio.model.Instrument;
import com.backbase.stream.portfolio.model.InstrumentBundle;
import com.backbase.stream.portfolio.model.Region;
import com.backbase.stream.portfolio.model.RegionBundle;
import com.backbase.stream.portfolio.service.InstrumentIngestionService;
import com.backbase.stream.portfolio.service.InstrumentIntegrationService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * Reactive implementation of {@code InstrumentIngestionService}.
 *
 * @author Vladimir Kirchev
 */
@Slf4j
@RequiredArgsConstructor
public class InstrumentIngestionReactiveService implements InstrumentIngestionService {

  private final PortfolioSagaProperties portfolioSagaProperties;
  private final InstrumentIntegrationService instrumentIntegrationService;

  @Override
  public Flux<AssetClassBundle> ingestWealthAssets(Flux<AssetClassBundle> assetClassBundle) {
    return assetClassBundle
        .map(List::of)
        .flatMap(
            instrumentIntegrationService::upsertAssetClass,
            portfolioSagaProperties.getTaskExecutors())
        .flatMapIterable(i -> i)
        .doOnNext(
            actual ->
                log.info(
                    "Finished Ingestion of asset, name: {}",
                    Optional.ofNullable(actual.getAssetClass())
                        .map(AssetClass::getName)
                        .orElse("null")));
  }

  @Override
  public Flux<InstrumentBundle> ingestInstruments(Flux<InstrumentBundle> instrumentBundles) {
    return instrumentBundles
        .flatMap(
            instrumentIntegrationService::upsertInstrument,
            portfolioSagaProperties.getTaskExecutors())
        .doOnNext(
            actual ->
                log.info(
                    "Finished Ingestion of Instrument, name: {}",
                    Optional.ofNullable(actual.getInstrument())
                        .map(Instrument::getName)
                        .orElse("null")));
  }

  @Override
  public Flux<RegionBundle> ingestRegionBundles(Flux<RegionBundle> regionBundles) {
    return regionBundles
        .map(List::of)
        .flatMap(
            instrumentIntegrationService::upsertRegions, portfolioSagaProperties.getTaskExecutors())
        .flatMapIterable(i -> i)
        .doOnNext(
            actual ->
                log.info(
                    "Finished Ingestion of region, name: {}",
                    Optional.ofNullable(actual.getRegion()).map(Region::getName).orElse("null")));
  }
}
