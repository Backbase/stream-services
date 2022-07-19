package com.backbase.stream.portfolio.service;

import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentAssetClassManagementApi;
import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentCountryManagementApi;
import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentManagementApi;
import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentPriceManagementApi;
import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentRegionManagementApi;
import com.backbase.portfolio.instrument.integration.api.service.v1.model.InstrumentPricesHistoryPutRequest;
import com.backbase.stream.portfolio.mapper.InstrumentMapper;
import com.backbase.stream.portfolio.model.AssetClass;
import com.backbase.stream.portfolio.model.AssetClassBundle;
import com.backbase.stream.portfolio.model.Instrument;
import com.backbase.stream.portfolio.model.InstrumentBundle;
import com.backbase.stream.portfolio.model.Region;
import com.backbase.stream.portfolio.model.RegionBundle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@Service
public class InstrumentIntegrationService {

    private final InstrumentMapper instrumentMapper = Mappers.getMapper(InstrumentMapper.class);
    private final InstrumentRegionManagementApi instrumentRegionManagementApi;
    private final InstrumentCountryManagementApi instrumentCountryManagementApi;
    private final InstrumentAssetClassManagementApi assetClassManagementApi;
    private final InstrumentManagementApi instrumentManagementApi;
    private final InstrumentPriceManagementApi instrumentPriceManagementApi;

    /**
     * Create Regions and all reference models that are related.
     * @param regionBundle gang for region
     * @return Flux of {@link Region}
     */
    public Flux<Region> createRegion(RegionBundle regionBundle) {
        Region region = regionBundle.getRegion();
        return instrumentRegionManagementApi.postRegion(instrumentMapper.mapRegion(region))
            .thenMany(createCountries(regionBundle, region))
            .map(at -> region)
            .doOnError(WebClientResponseException.class, ReactiveStreamHandler::handleWebClientResponseException)
            .onErrorResume(WebClientResponseException.class,
                ReactiveStreamHandler.error(region, "Failed to create Region"))
            .onErrorStop();
    }

    /**
     * Create Asset classes and all reference models that are related.
     * @param assetClassBundle gang for asset class
     * @return Flux of {@link AssetClass}
     */
    public Flux<AssetClass> createAssetClass(AssetClassBundle assetClassBundle) {
        AssetClass assetClass = assetClassBundle.getAssetClass();
        return assetClassManagementApi.postAssetClass(instrumentMapper.mapAssetClass(assetClass))
            .thenMany(createSubAssetClasses(assetClassBundle, assetClass))
            .map(at -> assetClass)
            .doOnError(WebClientResponseException.class, ReactiveStreamHandler::handleWebClientResponseException)
            .onErrorResume(WebClientResponseException.class,
                ReactiveStreamHandler.error(assetClass, "Failed to create Asset Class"))
            .onErrorStop();
    }

    /**
     * Create Instruments and all reference models that are related.
     * @param instrumentBundle gang for instrument
     * @return Mono {@link InstrumentBundle}
     */
    public Mono<InstrumentBundle> createInstrument(InstrumentBundle instrumentBundle) {
        Instrument instrument = instrumentBundle.getInstrument();
        return instrumentManagementApi
            .postInstrument(instrumentMapper.mapInstrument(instrument))
            .then(createHistoryPrices(instrumentBundle, instrument))
            .map(at -> instrumentBundle)
            .doOnError(WebClientResponseException.class, ReactiveStreamHandler::handleWebClientResponseException)
            .onErrorResume(WebClientResponseException.class,
                ReactiveStreamHandler.error(instrumentBundle, "Failed to create Instrument"))
            .onErrorStop();
    }

    @NotNull
    private Flux<Void> createSubAssetClasses(AssetClassBundle assetClassBundle, AssetClass assetClass) {
        return ReactiveStreamHandler.getFluxStream(assetClassBundle.getSubAssetClasses())
            .flatMap(sac -> assetClassManagementApi
                .postSubAssetClass(assetClass.getCode(), instrumentMapper.mapSubAssetClass(sac))
                .doOnError(WebClientResponseException.class, ReactiveStreamHandler::handleWebClientResponseException)
                .onErrorResume(WebClientResponseException.class,
                    ReactiveStreamHandler.error(assetClass, "Failed to create Sub Asset Class"))
                .onErrorStop());
    }

    @NotNull
    private Mono<Void> createHistoryPrices(InstrumentBundle instrumentBundle, Instrument instrument) {
        return Mono.justOrEmpty(instrumentBundle.getHistoryPrices())
            .flatMap(hps -> instrumentPriceManagementApi
                .putInstrumentHistoryPrices(instrument.getId(), new InstrumentPricesHistoryPutRequest()
                    .priceData(instrumentMapper.mapHistoryPrices(hps)))
                .doOnError(WebClientResponseException.class, ReactiveStreamHandler::handleWebClientResponseException)
                .onErrorResume(WebClientResponseException.class,
                    ReactiveStreamHandler.error(hps, "Failed to create History Prices"))
                .onErrorStop());
    }

    @NotNull
    private Flux<Void> createCountries(RegionBundle regionBundle, Region region) {
        return ReactiveStreamHandler.getFluxStream(regionBundle.getCountries())
            .flatMap(country -> instrumentCountryManagementApi
                .postCountry(instrumentMapper.mapCountry(region.getCode(), country))
                .doOnError(WebClientResponseException.class,
                    ReactiveStreamHandler::handleWebClientResponseException)
                .onErrorResume(WebClientResponseException.class,
                    ReactiveStreamHandler.error(country, "Failed to create Country"))
                .onErrorStop()
            );
    }

}
