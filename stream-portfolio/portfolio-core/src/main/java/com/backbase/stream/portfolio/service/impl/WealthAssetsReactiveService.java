package com.backbase.stream.portfolio.service.impl;

import java.util.List;
import java.util.Optional;
import com.backbase.stream.portfolio.configuration.PortfolioSagaProperties;
import com.backbase.stream.portfolio.model.AssetClass;
import com.backbase.stream.portfolio.model.AssetClassBundle;
import com.backbase.stream.portfolio.service.InstrumentIntegrationService;
import com.backbase.stream.portfolio.service.WealthAssetsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * Reactive implementation of {@code WealthAssetsService}.
 * 
 * @author Vladimir Kirchev
 *
 */
@Slf4j
@RequiredArgsConstructor
public class WealthAssetsReactiveService implements WealthAssetsService {
    private final PortfolioSagaProperties portfolioSagaProperties;
    private final InstrumentIntegrationService instrumentIntegrationService;

    @Override
    public Flux<AssetClassBundle> ingestWealthAssets(Flux<AssetClassBundle> assetClassBundle) {
        return assetClassBundle.map(List::of)
                .flatMap(instrumentIntegrationService::upsertAssetClass, portfolioSagaProperties.getTaskExecutors())
                .flatMapIterable(i -> i).doOnNext(actual -> log.info("Finished Ingestion of asset, name: {}",
                        Optional.ofNullable(actual.getAssetClass()).map(AssetClass::getName).orElse("null")));
    }

}
