package com.backbase.stream.portfolio.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentAssetClassManagementApi;
import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentCountryManagementApi;
import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentManagementApi;
import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentPriceManagementApi;
import com.backbase.portfolio.instrument.integration.api.service.v1.InstrumentRegionManagementApi;
import com.backbase.portfolio.instrument.integration.api.service.v1.model.AssetClassesPostRequest;
import com.backbase.portfolio.instrument.integration.api.service.v1.model.CountriesGetItem;
import com.backbase.portfolio.instrument.integration.api.service.v1.model.CountriesGetRequest;
import com.backbase.portfolio.instrument.integration.api.service.v1.model.CountriesPostRequest;
import com.backbase.portfolio.instrument.integration.api.service.v1.model.InstrumentHistoryPricesRequestItem;
import com.backbase.portfolio.instrument.integration.api.service.v1.model.InstrumentPricesHistoryPutRequest;
import com.backbase.portfolio.instrument.integration.api.service.v1.model.InstrumentsPostRequest;
import com.backbase.portfolio.instrument.integration.api.service.v1.model.RegionsGetItem;
import com.backbase.portfolio.instrument.integration.api.service.v1.model.RegionsGetRequest;
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
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
        Mockito.when(instrumentRegionManagementApi.getRegion(0, Integer.MAX_VALUE))
            .thenReturn(Mono.empty());
        Mockito.when(instrumentCountryManagementApi.getCountriesByRegion(anyString(), eq(0), eq(Integer.MAX_VALUE)))
            .thenReturn(Mono.empty());

        instrumentIntegrationService.upsertRegions(List.of(regionBundle)).block();

        Mockito.verify(instrumentRegionManagementApi)
            .postRegion(new RegionsPostRequest().code(regionCode).name(regionName));
        Mockito.verify(instrumentCountryManagementApi)
            .postCountry(new CountriesPostRequest().code(uaCode).name(uaName).region(regionCode));
    }

    @Test
    void updateRegion() {
        String regionName = "EU test ";
        String regionCode = "154";
        String uaCode = "UA";
        String uaName = "Ukraine";
        String testName = "new test name";
        RegionBundle regionBundle = new RegionBundle()
            .region(new Region().code(regionCode).name(testName))
            .countries(List.of(new Country().code(uaCode).name(testName)));

        Mockito.when(instrumentRegionManagementApi.putRegion(anyString(), anyString(), anyString()))
            .thenReturn(Mono.empty());
        Mockito.when(instrumentCountryManagementApi.putCountry(anyString(), anyString(), anyString()))
            .thenReturn(Mono.empty());

        Mockito.when(instrumentRegionManagementApi.getRegion(0, Integer.MAX_VALUE))
            .thenReturn(Mono.just(new RegionsGetRequest()
                .addRegionsItem(new RegionsGetItem().code(regionCode).name(regionName))));
        Mockito.when(instrumentCountryManagementApi.getCountriesByRegion(anyString(), eq(0), eq(Integer.MAX_VALUE)))
            .thenReturn(Mono.just(new CountriesGetRequest()
                .addCoutriesItem(new CountriesGetItem().code(uaCode).name(uaName))));

        instrumentIntegrationService.upsertRegions(List.of(regionBundle)).block();

        Mockito.verify(instrumentRegionManagementApi, Mockito.never())
            .postRegion(any(RegionsPostRequest.class));
        Mockito.verify(instrumentCountryManagementApi, Mockito.never())
            .postCountry(any(CountriesPostRequest.class));
        Mockito.verify(instrumentRegionManagementApi)
            .putRegion(regionCode, testName, regionCode);
        Mockito.verify(instrumentCountryManagementApi)
            .putCountry(uaCode, testName, uaCode);

    }

    @Test
    void updateSameEntriesRegion() {
        String regionName = "EU";
        String regionCode = "154";
        String uaCode = "UA";
        String uaName = "Ukraine";
        RegionBundle regionBundle = new RegionBundle()
            .region(new Region().code(regionCode).name(regionName))
            .countries(List.of(new Country().code(uaCode).name(uaName)));

        Mockito.when(instrumentRegionManagementApi.getRegion(0, Integer.MAX_VALUE))
            .thenReturn(Mono.just(new RegionsGetRequest()
                .addRegionsItem(new RegionsGetItem().code(regionCode).name(regionName))));
        Mockito.when(instrumentCountryManagementApi.getCountriesByRegion(anyString(), eq(0), eq(Integer.MAX_VALUE)))
            .thenReturn(Mono.just(new CountriesGetRequest()
                .addCoutriesItem(new CountriesGetItem().code(uaCode).name(uaName))));

        instrumentIntegrationService.upsertRegions(List.of(regionBundle)).block();

        Mockito.verify(instrumentRegionManagementApi, Mockito.never())
            .postRegion(any(RegionsPostRequest.class));
        Mockito.verify(instrumentCountryManagementApi, Mockito.never())
            .postCountry(any(CountriesPostRequest.class));
        Mockito.verify(instrumentRegionManagementApi, Mockito.never())
            .putRegion(regionCode, regionName, regionCode);
        Mockito.verify(instrumentCountryManagementApi, Mockito.never())
            .putCountry(uaCode, uaName, uaCode);

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
        Mockito.when(assetClassManagementApi.getAssetClasses(0, Integer.MAX_VALUE))
            .thenReturn(Mono.empty());
        Mockito.when(assetClassManagementApi.getSubAssetClasses(anyString(), eq(0), eq(Integer.MAX_VALUE)))
            .thenReturn(Mono.empty());

        instrumentIntegrationService.upsertAssetClass(List.of(assetClassBundle)).block();

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
        Mockito.when(instrumentManagementApi.getInstrument(anyString()))
            .thenReturn(Mono.empty());

        instrumentIntegrationService.upsertInstrument(instrumentBundle).block();

        Mockito.verify(instrumentManagementApi).postInstrument(new InstrumentsPostRequest().id(instrumentId));
        Mockito.verify(instrumentPriceManagementApi)
            .putInstrumentHistoryPrices(instrumentId, new InstrumentPricesHistoryPutRequest()
                .priceData(List.of(new InstrumentHistoryPricesRequestItem()
                    .date(OffsetDateTime.of(localDate, LocalTime.NOON, ZoneOffset.UTC))
                    .price(new com.backbase.portfolio.instrument.integration.api.service.v1.model.Money()
                        .amount(BigDecimal.ONE)
                        .currencyCode("USD"))
                    .priceType(InstrumentHistoryPricesRequestItem.PriceTypeEnum.OPEN))));

    }

}
