package com.backbase.stream.portfolio.service;

import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentAssetClassManagementApi;
import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentCountryManagementApi;
import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentManagementApi;
import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentPriceManagementApi;
import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentRegionManagementApi;
import com.backbase.portfolio.instrument.integration.api.service.v1.model.AssetClassesGetItem;
import com.backbase.portfolio.instrument.integration.api.service.v1.model.CountriesGetItem;
import com.backbase.portfolio.instrument.integration.api.service.v1.model.InstrumentPricesHistoryPutRequest;
import com.backbase.portfolio.instrument.integration.api.service.v1.model.RegionsGetItem;
import com.backbase.portfolio.instrument.integration.api.service.v1.model.SubAssetClassesGetItem;
import com.backbase.stream.portfolio.mapper.InstrumentMapper;
import com.backbase.stream.portfolio.model.AssetClass;
import com.backbase.stream.portfolio.model.AssetClassBundle;
import com.backbase.stream.portfolio.model.Country;
import com.backbase.stream.portfolio.model.Instrument;
import com.backbase.stream.portfolio.model.InstrumentBundle;
import com.backbase.stream.portfolio.model.Region;
import com.backbase.stream.portfolio.model.RegionBundle;
import com.backbase.stream.portfolio.model.SubAssetClass;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
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
     * Create/Update Regions and all reference models that are related.
     *
     * @param regionBundles gang for regions
     * @return Mono represent all upsert is done
     */
    public Mono<List<RegionBundle>> upsertRegions(List<RegionBundle> regionBundles) {
        return instrumentRegionManagementApi
            .getRegion(0, Integer.MAX_VALUE)
            .map(
                rr ->
                    rr.getRegions().stream()
                        .collect(Collectors.toMap(RegionsGetItem::getCode, RegionsGetItem::getName)))
            .switchIfEmpty(Mono.just(Collections.emptyMap()))
            .flatMap(
                regionNameByCode ->
                    ReactiveStreamHandler.getFluxStream(regionBundles)
                        .flatMap(
                            rb -> {
                                Region region = rb.getRegion();
                                log.debug("Region to process: {}", region);
                                return upsertRegion(regionNameByCode, region)
                                    .then(upsertCountries(rb, region))
                                    .doOnError(
                                        WebClientResponseException.class,
                                        ReactiveStreamHandler::handleWebClientResponseException)
                                    .onErrorResume(
                                        WebClientResponseException.class,
                                        ReactiveStreamHandler.error(
                                            region, "Failed to upsert" + " Region"))
                                    .onErrorStop()
                                    .map(o -> rb);
                            })
                        .collectList());
    }

    /**
     * Create Asset classes and all reference models that are related.
     *
     * @param assetClassBundles gang for asset classes
     * @return Mono of list of {@link AssetClassBundle}
     */
    public Mono<List<AssetClassBundle>> upsertAssetClass(List<AssetClassBundle> assetClassBundles) {
        return assetClassManagementApi
            .getAssetClasses(0, Integer.MAX_VALUE)
            .map(
                ac ->
                    ac.getAssetClasses().stream()
                        .collect(Collectors.toMap(AssetClassesGetItem::getCode, Function.identity())))
            .switchIfEmpty(Mono.just(Collections.emptyMap()))
            .flatMap(
                acByCode ->
                    ReactiveStreamHandler.getFluxStream(assetClassBundles)
                        .flatMap(
                            ab -> {
                                AssetClass assetClass = ab.getAssetClass();
                                return upsertAssetClassItem(assetClass, acByCode)
                                    .thenMany(upsertSubAssetClasses(ab, assetClass))
                                    .map(at -> assetClass)
                                    .doOnError(
                                        WebClientResponseException.class,
                                        ReactiveStreamHandler::handleWebClientResponseException)
                                    .onErrorResume(
                                        WebClientResponseException.class,
                                        ReactiveStreamHandler.error(
                                            assetClass, "Failed to create Asset" + " Class"))
                                    .onErrorStop();
                            })
                        .collectList())
            .flatMap(o -> Mono.just(assetClassBundles));
    }

    /**
     * Create Instruments and all reference models that are related.
     *
     * @param instrumentBundle gang for instrument
     * @return Mono {@link InstrumentBundle}
     */
    public Mono<InstrumentBundle> upsertInstrument(InstrumentBundle instrumentBundle) {
        Instrument instrument = instrumentBundle.getInstrument();
        log.debug("Upsert Instrument: {}", instrument);
        return instrumentManagementApi
            .getInstrument(instrument.getId())
            .onErrorResume(WebClientResponseException.NotFound.class, throwable -> Mono.empty())
            .flatMap(
                i ->
                    instrumentManagementApi
                        .putInstrument(
                            instrument.getId(), instrumentMapper.mapPutInstrument(instrument))
                        .then(Mono.just(instrument)))
            .switchIfEmpty(
                instrumentManagementApi
                    .postInstrument(instrumentMapper.mapInstrument(instrument))
                    .then(Mono.just(instrument)))
            .then(updateHistoryPrices(instrumentBundle, instrument))
            .map(o -> instrumentBundle)
            .doOnError(
                WebClientResponseException.class,
                ReactiveStreamHandler::handleWebClientResponseException)
            .onErrorResume(
                WebClientResponseException.class,
                ReactiveStreamHandler.error(instrumentBundle, "Failed to create Instrument"))
            .onErrorStop();
    }

    @NotNull
    private Mono<List<SubAssetClass>> upsertSubAssetClasses(
        AssetClassBundle assetClassBundle, AssetClass assetClass) {
        return assetClassManagementApi
            .getSubAssetClasses(assetClass.getCode(), 0, Integer.MAX_VALUE)
            .map(
                ac ->
                    ac.getSubAssetClasses().stream()
                        .collect(
                            Collectors.toMap(SubAssetClassesGetItem::getCode, Function.identity())))
            .switchIfEmpty(Mono.just(Collections.emptyMap()))
            .flatMap(
                sacByCode ->
                    ReactiveStreamHandler.getFluxStream(assetClassBundle.getSubAssetClasses())
                        .flatMap(
                            sac ->
                                upsertSubAssetClassItem(assetClass.getCode(), sac, sacByCode)
                                    .doOnError(
                                        WebClientResponseException.class,
                                        ReactiveStreamHandler::handleWebClientResponseException)
                                    .onErrorResume(
                                        WebClientResponseException.class,
                                        ReactiveStreamHandler.error(
                                            assetClass, "Failed to create" + " Asset" + " Class"))
                                    .onErrorStop())
                        .collectList());
    }

    @NotNull
    private Mono<Void> updateHistoryPrices(InstrumentBundle instrumentBundle, Instrument instrument) {
        return Mono.justOrEmpty(instrumentBundle.getHistoryPrices())
            .flatMap(
                hps -> {
                    log.debug("Update HistoryPrices: {}", hps);
                    return instrumentPriceManagementApi
                        .putInstrumentHistoryPrices(
                            instrument.getId(),
                            new InstrumentPricesHistoryPutRequest()
                                .priceData(instrumentMapper.mapHistoryPrices(hps)))
                        .doOnError(
                            WebClientResponseException.class,
                            ReactiveStreamHandler::handleWebClientResponseException)
                        .onErrorResume(
                            WebClientResponseException.class,
                            ReactiveStreamHandler.error(hps, "Failed to create History Prices"))
                        .onErrorStop();
                });
    }

    @NotNull
    private Mono<Void> upsertCountries(RegionBundle regionBundle, Region region) {
        return instrumentCountryManagementApi
            .getCountriesByRegion(region.getCode(), 0, Integer.MAX_VALUE)
            .map(
                cr ->
                    cr.getCoutries().stream()
                        .collect(
                            Collectors.toMap(CountriesGetItem::getCode, CountriesGetItem::getName)))
            .switchIfEmpty(Mono.just(Collections.emptyMap()))
            .flatMap(
                countryNameByCode ->
                    ReactiveStreamHandler.getFluxStream(regionBundle.getCountries())
                        .flatMap(
                            c ->
                                upsertCountry(countryNameByCode, region, c)
                                    .doOnError(
                                        WebClientResponseException.class,
                                        ReactiveStreamHandler::handleWebClientResponseException)
                                    .onErrorResume(
                                        WebClientResponseException.class,
                                        ReactiveStreamHandler.error(c, "Failed to create" + " Country"))
                                    .onErrorStop())
                        .collectList())
            .flatMap(o -> Mono.empty());
    }

    private Mono<Region> upsertRegion(Map<String, String> regionNameByCode, Region region) {
        if (regionNameByCode.containsKey(region.getCode())) {
            if (Objects.requireNonNullElse(region.getName(), "")
                .equals(regionNameByCode.get(region.getCode()))) {
                return Mono.just(region);
            }
            log.debug("Update Region: {}", region);
            return instrumentRegionManagementApi
                .putRegion(region.getCode(), region.getName(), region.getCode())
                .map(r -> region);
        }
        log.debug("Create Region: {}", region);
        return instrumentRegionManagementApi
            .postRegion(instrumentMapper.mapRegion(region))
            .map(r -> region);
    }

    private Mono<Country> upsertCountry(
        Map<String, String> countryNameByCode, Region region, Country country) {
        if (countryNameByCode.containsKey(country.getCode())) {
            if (Objects.requireNonNullElse(country.getName(), "")
                .equals(countryNameByCode.get(country.getCode()))) {
                return Mono.just(country);
            }
            log.debug("Update Country: {}", country);
            return instrumentCountryManagementApi
                .putCountry(country.getCode(), country.getName(), country.getCode())
                .map(r -> country);
        }
        log.debug("Create Country: {}", country);
        return instrumentCountryManagementApi
            .postCountry(instrumentMapper.mapCountry(region.getCode(), country))
            .map(r -> country);
    }

    private Mono<AssetClass> upsertAssetClassItem(
        AssetClass assetClass, Map<String, AssetClassesGetItem> acByCode) {
        if (acByCode.containsKey(assetClass.getCode())) {
            log.debug("Update AssetClass: {}", assetClass);
            if (Objects.requireNonNullElse(assetClass.getName(), "")
                .equals(acByCode.get(assetClass.getCode()).getName())) {
                return Mono.just(assetClass);
            }
            return assetClassManagementApi
                .putAssetClass(assetClass.getCode(), instrumentMapper.mapPutAssetClass(assetClass))
                .map(o -> assetClass);
        } else {
            log.debug("Create AssetClass: {}", assetClass);
            return assetClassManagementApi
                .postAssetClass(instrumentMapper.mapAssetClass(assetClass))
                .map(o -> assetClass);
        }
    }

    private Mono<SubAssetClass> upsertSubAssetClassItem(
        String assetClassCode,
        SubAssetClass subAssetClass,
        Map<String, SubAssetClassesGetItem> acByCode) {
        if (acByCode.containsKey(subAssetClass.getCode())) {
            log.debug("Update SubAssetClass: {}", subAssetClass);
            if (Objects.requireNonNullElse(subAssetClass.getName(), "")
                .equals(acByCode.get(subAssetClass.getCode()).getName())) {
                return Mono.just(subAssetClass);
            }
            return assetClassManagementApi
                .putSubAssetClass(
                    subAssetClass.getCode(), instrumentMapper.mapPutSubAssetClass(subAssetClass))
                .map(o -> subAssetClass);
        } else {
            log.debug("Create SubAssetClass: {}", subAssetClass);
            return assetClassManagementApi
                .postSubAssetClass(assetClassCode, instrumentMapper.mapSubAssetClass(subAssetClass))
                .map(o -> subAssetClass);
        }
    }
}
