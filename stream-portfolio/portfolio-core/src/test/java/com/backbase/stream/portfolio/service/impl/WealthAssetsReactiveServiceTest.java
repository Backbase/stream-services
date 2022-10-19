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
import com.backbase.stream.portfolio.model.WealthAssetBundle;
import com.backbase.stream.portfolio.service.InstrumentIntegrationService;
import com.backbase.stream.portfolio.util.PortfolioTestUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * WealthAssetsReactiveService Test.
 * 
 * @author Vladimir Kirchev
 *
 */
@ExtendWith(MockitoExtension.class)
class WealthAssetsReactiveServiceTest {
    @Mock
    private PortfolioSagaProperties portfolioSagaProperties;

    @Mock
    private InstrumentIntegrationService instrumentIntegrationService;

    @InjectMocks
    private WealthAssetsReactiveService wealthAssetsReactiveService;

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
                wealthAssetsReactiveService.ingestWealthAssets(Flux.fromIterable(assetClasses));

        Assertions.assertNotNull(ingestedWealthAssets);

        StepVerifier.create(ingestedWealthAssets).assertNext(assertEqualsTo(assetClassBundle0))
                .assertNext(assertEqualsTo(assetClassBundle1)).assertNext(assertEqualsTo(assetClassBundle2))
                .assertNext(assertEqualsTo(assetClassBundle3)).verifyComplete();
    }
}
