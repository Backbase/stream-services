package com.backbase.stream.portfolio.service.impl;

import java.util.Optional;
import com.backbase.stream.portfolio.configuration.PortfolioSagaProperties;
import com.backbase.stream.portfolio.model.AssetClass;
import com.backbase.stream.portfolio.model.AssetClassBundle;
import com.backbase.stream.portfolio.saga.wealth.asset.WealthAssetsSaga;
import com.backbase.stream.portfolio.saga.wealth.asset.WealthAssetsTask;
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
    private final WealthAssetsSaga wealthAssetsSaga;

    @Override
    public Flux<AssetClassBundle> ingestWealthAssets(Flux<AssetClassBundle> assetClassBundle) {
        return assetClassBundle.map(WealthAssetsTask::new)
                .flatMap(wealthAssetsSaga::executeTask, portfolioSagaProperties.getTaskExecutors())
                .map(WealthAssetsTask::getData).doOnNext(actual -> log.info("Finished Ingestion of asset, name: {}",
                        Optional.ofNullable(actual.getAssetClass()).map(AssetClass::getName).orElse("null")));
    }

}
