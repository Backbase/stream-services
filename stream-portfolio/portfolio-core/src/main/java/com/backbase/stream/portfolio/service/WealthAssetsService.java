package com.backbase.stream.portfolio.service;

import com.backbase.stream.portfolio.model.AssetClassBundle;
import reactor.core.publisher.Flux;

/**
 * WealthAssets Service.
 * 
 * @author Vladimir Kirchev
 *
 */
public interface WealthAssetsService {

    /**
     * Ingest Wealth Assets.
     * 
     * @param assetClassBundle The {@code AssetClassBundle} to be ingested.
     * @return The ingested {@code AssetClassBundle}.
     */
    Flux<AssetClassBundle> ingestWealthAssets(Flux<AssetClassBundle> assetClassBundle);
}
