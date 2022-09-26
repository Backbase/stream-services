package com.backbase.stream.portfolio.mapper;

import com.backbase.portfolio.instrument.integration.api.service.v1.model.AssetClassesPostRequest;
import com.backbase.portfolio.instrument.integration.api.service.v1.model.AssetClassesPutRequest;
import com.backbase.portfolio.instrument.integration.api.service.v1.model.CountriesPostRequest;
import com.backbase.portfolio.instrument.integration.api.service.v1.model.InstrumentHistoryPricesRequestItem;
import com.backbase.portfolio.instrument.integration.api.service.v1.model.InstrumentPutRequest;
import com.backbase.portfolio.instrument.integration.api.service.v1.model.InstrumentsPostRequest;
import com.backbase.portfolio.instrument.integration.api.service.v1.model.RegionsPostRequest;
import com.backbase.portfolio.instrument.integration.api.service.v1.model.SubAssetClassPutRequest;
import com.backbase.portfolio.instrument.integration.api.service.v1.model.SubAssetClassesPostRequest;
import com.backbase.stream.portfolio.model.AssetClass;
import com.backbase.stream.portfolio.model.Country;
import com.backbase.stream.portfolio.model.Instrument;
import com.backbase.stream.portfolio.model.InstrumentHistoryPrice;
import com.backbase.stream.portfolio.model.Region;
import com.backbase.stream.portfolio.model.SubAssetClass;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface InstrumentMapper {

    RegionsPostRequest mapRegion(Region region);

    @Mapping(target = ".", source = "country")
    @Mapping(target = "region", source = "regionCode")
    CountriesPostRequest mapCountry(String regionCode, Country country);

    InstrumentsPostRequest mapInstrument(Instrument instrument);

    InstrumentPutRequest mapPutInstrument(Instrument instrument);

    AssetClassesPostRequest mapAssetClass(AssetClass assetClass);

    AssetClassesPutRequest mapPutAssetClass(AssetClass assetClass);

    SubAssetClassesPostRequest mapSubAssetClass(SubAssetClass assetClass);

    SubAssetClassPutRequest mapPutSubAssetClass(SubAssetClass assetClass);

    List<InstrumentHistoryPricesRequestItem> mapHistoryPrices(List<InstrumentHistoryPrice> historyPrices);

}
