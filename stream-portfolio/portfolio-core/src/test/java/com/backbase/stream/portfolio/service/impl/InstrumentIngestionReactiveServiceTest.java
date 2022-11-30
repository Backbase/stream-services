package com.backbase.stream.portfolio.service.impl;

import static com.backbase.stream.LambdaAssertions.assertEqualsTo;
import static org.mockito.ArgumentMatchers.any;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import com.backbase.stream.portfolio.configuration.PortfolioSagaProperties;
import com.backbase.stream.portfolio.model.AssetClassBundle;
import com.backbase.stream.portfolio.model.InstrumentBundle;
import com.backbase.stream.portfolio.model.RegionBundle;
import com.backbase.stream.portfolio.model.WealthAssetBundle;
import com.backbase.stream.portfolio.model.WealthInstrumentBundle;
import com.backbase.stream.portfolio.model.WealthRegionsBundle;
import com.backbase.stream.portfolio.service.InstrumentIntegrationService;
import com.backbase.stream.portfolio.util.PortfolioTestUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * InstrumentIngestionReactiveService Test.
 * 
 * @author Vladimir Kirchev
 *
 */
@ExtendWith(MockitoExtension.class)
class InstrumentIngestionReactiveServiceTest {
    @Mock
    private PortfolioSagaProperties portfolioSagaProperties;

    @Mock
    private InstrumentIntegrationService instrumentIntegrationService;

    @InjectMocks
    private InstrumentIngestionReactiveService instrumentIngestionReactiveService;

    @Test
    void shouldIngestWealthAssets() throws Exception {
        WealthAssetBundle wealthAssetBundle = PortfolioTestUtil.getWealthAssetBundle();
        List<AssetClassBundle> assetClasses = wealthAssetBundle.getAssetClasses();

        AssetClassBundle assetClassBundle0 = assetClasses.get(0);
        AssetClassBundle assetClassBundle1 = assetClasses.get(1);
        AssetClassBundle assetClassBundle2 = assetClasses.get(2);
        AssetClassBundle assetClassBundle3 = assetClasses.get(3);

        Mockito.when(portfolioSagaProperties.getTaskExecutors()).thenReturn(1);
        Mockito.when(instrumentIntegrationService.upsertAssetClass(any())).thenAnswer(i -> Mono.just(i.getArgument(0)));

        Flux<AssetClassBundle> ingestedWealthAssets =
                instrumentIngestionReactiveService.ingestWealthAssets(Flux.fromIterable(assetClasses));

        Assertions.assertNotNull(ingestedWealthAssets);

        StepVerifier.create(ingestedWealthAssets).assertNext(assertEqualsTo(assetClassBundle0))
                .assertNext(assertEqualsTo(assetClassBundle1)).assertNext(assertEqualsTo(assetClassBundle2))
                .assertNext(assertEqualsTo(assetClassBundle3)).verifyComplete();
    }

    @Test
    void shouldIngestealthInstrumentBundles() throws Exception {
        WealthInstrumentBundle wealthInstrumentBundle = PortfolioTestUtil.getWealthInstrumentBundle();
        List<InstrumentBundle> instruments = wealthInstrumentBundle.getInstruments();

        InstrumentBundle instrumentBundle0 = instruments.get(0);

        Mockito.when(portfolioSagaProperties.getTaskExecutors()).thenReturn(1);
        Mockito.when(instrumentIntegrationService.upsertInstrument(any())).thenAnswer(i -> Mono.just(i.getArgument(0)));

        Flux<InstrumentBundle> ingestedInstruments =
                instrumentIngestionReactiveService.ingestInstruments(Flux.fromIterable(instruments));

        Assertions.assertNotNull(ingestedInstruments);

        StepVerifier.create(ingestedInstruments).assertNext(assertEqualsTo(instrumentBundle0)).verifyComplete();
    }

    @Test
    void shouldIngestRegionBundles() throws Exception {
        WealthRegionsBundle wealthRegionsBundle = PortfolioTestUtil.getWealthRegionsBundle();
        List<RegionBundle> regionBundles = wealthRegionsBundle.getRegions();

        RegionBundle regionBundle0 = regionBundles.get(0);
        RegionBundle regionBundle1 = regionBundles.get(1);

        Mockito.when(portfolioSagaProperties.getTaskExecutors()).thenReturn(1);
        Mockito.when(instrumentIntegrationService.upsertRegions(any())).thenAnswer(i -> Mono.just(i.getArgument(0)));

        Flux<RegionBundle> ingestedRegionBundles =
                instrumentIngestionReactiveService.ingestRegionBundles(Flux.fromIterable(regionBundles));

        Assertions.assertNotNull(ingestedRegionBundles);

        StepVerifier.create(ingestedRegionBundles).assertNext(assertEqualsTo(regionBundle0))
                .assertNext(assertEqualsTo(regionBundle1)).verifyComplete();
    }
}
