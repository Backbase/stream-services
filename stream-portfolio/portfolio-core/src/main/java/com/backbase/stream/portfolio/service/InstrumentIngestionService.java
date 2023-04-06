package com.backbase.stream.portfolio.service;

import com.backbase.stream.portfolio.model.AssetClassBundle;
import com.backbase.stream.portfolio.model.InstrumentBundle;
import com.backbase.stream.portfolio.model.RegionBundle;
import reactor.core.publisher.Flux;

/**
 * WealthAssets Service.
 *
 * @author Vladimir Kirchev
 *
 */
public interface InstrumentIngestionService {

    /**
     * Ingest Wealth Assets.
     *
     * @param assetClassBundle The {@code AssetClassBundle} to be ingested.
     * @return The ingested {@code AssetClassBundle}.
     */
    Flux<AssetClassBundle> ingestWealthAssets(Flux<AssetClassBundle> assetClassBundle);

    /**
     * Ingest InstrumentBundles.
     *
     * @param instrumentBundles The {@code InstrumentBundle} to be ingested.
     * @return The ingested {@code InstrumentBundle}.
     */
    Flux<InstrumentBundle> ingestInstruments(Flux<InstrumentBundle> instrumentBundles);

    /**
     * Ingest Region Bundles.
     *
     * @param regionBundle The Flux of {@code RegionBundle} to be ingested.
     * @return The Flux of ingested {@code RegionBundle}.
     */
    Flux<RegionBundle> ingestRegionBundles(Flux<RegionBundle> regionBundle);
}
