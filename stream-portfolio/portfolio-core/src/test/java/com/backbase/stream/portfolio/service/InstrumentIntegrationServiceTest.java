package com.backbase.stream.portfolio.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentAssetClassManagementApi;
import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentCountryManagementApi;
import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentManagementApi;
import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentPriceManagementApi;
import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentRegionManagementApi;
import com.backbase.portfolio.instrument.integration.api.service.v1.model.AssetClassesPostRequest;
import com.backbase.portfolio.instrument.integration.api.service.v1.model.CountriesPostRequest;
import com.backbase.portfolio.instrument.integration.api.service.v1.model.InstrumentHistoryPricesRequestItem;
import com.backbase.portfolio.instrument.integration.api.service.v1.model.InstrumentPricesHistoryPutRequest;
import com.backbase.portfolio.instrument.integration.api.service.v1.model.InstrumentsPostRequest;
import com.backbase.portfolio.instrument.integration.api.service.v1.model.RegionsPostRequest;
import com.backbase.portfolio.instrument.integration.api.service.v1.model.SubAssetClassesPostRequest;
import com.backbase.stream.portfolio.mapper.InstrumentMapper;
import com.backbase.stream.portfolio.model.AssetClass;
import com.backbase.stream.portfolio.model.AssetClassBundle;
import com.backbase.stream.portfolio.model.Country;
import com.backbase.stream.portfolio.model.Instrument;
import com.backbase.stream.portfolio.model.InstrumentBundle;
import com.backbase.stream.portfolio.model.InstrumentHistoryPrice;
import com.backbase.stream.portfolio.model.InstrumentHistoryPrice.PriceTypeEnum;
import com.backbase.stream.portfolio.model.Money;
import com.backbase.stream.portfolio.model.Region;
import com.backbase.stream.portfolio.model.RegionBundle;
import com.backbase.stream.portfolio.model.SubAssetClass;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class InstrumentIntegrationServiceTest {

    @SuppressWarnings("unused")
    @Spy
    private InstrumentMapper instrumentMapper;
    @Mock
    private InstrumentRegionManagementApi instrumentRegionManagementApi;
    @Mock
    private InstrumentCountryManagementApi instrumentCountryManagementApi;
    @Mock
    private InstrumentAssetClassManagementApi assetClassManagementApi;
    @Mock
    private InstrumentManagementApi instrumentManagementApi;
    @Mock
    private InstrumentPriceManagementApi instrumentPriceManagementApi;

    @InjectMocks
    private InstrumentIntegrationService instrumentIntegrationService;

    @Test
    void createRegion() {
        String regionName = "EU";
        String regionCode = "154";
        String uaName = "Ukraine";
        String uaCode = "UA";
        RegionBundle regionBundle = new RegionBundle()
            .region(new Region().code(regionCode).name(regionName))
            .countries(List.of(new Country().code(uaCode).name(uaName)));

        Mockito.when(instrumentRegionManagementApi.postRegion(any(RegionsPostRequest.class)))
            .thenReturn(Mono.empty());
        Mockito.when(instrumentCountryManagementApi.postCountry(any(CountriesPostRequest.class)))
            .thenReturn(Mono.empty());

        instrumentIntegrationService.createRegion(regionBundle).blockLast();

        Mockito.verify(instrumentRegionManagementApi)
            .postRegion(new RegionsPostRequest().code(regionCode).name(regionName));
        Mockito.verify(instrumentCountryManagementApi)
            .postCountry(new CountriesPostRequest().code(uaCode).name(uaName).region(regionCode));

    }

    @Test
    void createAssetClass() {
        String assetCode = "123";
        String assetName = "Test";
        String subAssetCode = "sub-123";
        String subAssetName = "sub test";

        AssetClassBundle assetClassBundle = new AssetClassBundle()
            .assetClass(new AssetClass().code(assetCode).name(assetName))
            .subAssetClasses(List.of(new SubAssetClass().code(subAssetCode).name(subAssetName)));

        Mockito.when(assetClassManagementApi.postAssetClass(any(AssetClassesPostRequest.class)))
            .thenReturn(Mono.empty());
        Mockito.when(assetClassManagementApi.postSubAssetClass(anyString(), any(SubAssetClassesPostRequest.class)))
            .thenReturn(Mono.empty());

        instrumentIntegrationService.createAssetClass(assetClassBundle).blockLast();

        Mockito.verify(assetClassManagementApi).postAssetClass(new AssetClassesPostRequest()
            .code(assetCode).name(assetName));
        Mockito.verify(assetClassManagementApi).postSubAssetClass(assetCode, new SubAssetClassesPostRequest()
            .code(subAssetCode).name(subAssetName));

    }

    @Test
    void createInstrument() {
        String instrumentId = "externalId";
        LocalDate localDate = LocalDate.now();

        Money money = new Money()
            .amount(BigDecimal.ONE)
            .currencyCode("USD");
        InstrumentBundle instrumentBundle = new InstrumentBundle()
            .instrument(new Instrument().id(instrumentId))
            .historyPrices(List.of(new InstrumentHistoryPrice()
                .date(localDate)
                .price(money)
                .priceType(PriceTypeEnum.OPEN)));

        Mockito.when(instrumentManagementApi.postInstrument(any(InstrumentsPostRequest.class)))
            .thenReturn(Mono.empty());
        Mockito.when(instrumentPriceManagementApi
                .putInstrumentHistoryPrices(anyString(), any(InstrumentPricesHistoryPutRequest.class)))
            .thenReturn(Mono.empty());

        instrumentIntegrationService.createInstrument(instrumentBundle).block();

        Mockito.verify(instrumentManagementApi).postInstrument(new InstrumentsPostRequest().id(instrumentId));
        Mockito.verify(instrumentPriceManagementApi)
            .putInstrumentHistoryPrices(instrumentId, new InstrumentPricesHistoryPutRequest()
                .priceData(List.of(new InstrumentHistoryPricesRequestItem()
                    .date(localDate)
                    .price(new com.backbase.portfolio.instrument.integration.api.service.v1.model.Money()
                        .amount(BigDecimal.ONE)
                        .currencyCode("USD"))
                    .priceType(InstrumentHistoryPricesRequestItem.PriceTypeEnum.OPEN))));

    }

}