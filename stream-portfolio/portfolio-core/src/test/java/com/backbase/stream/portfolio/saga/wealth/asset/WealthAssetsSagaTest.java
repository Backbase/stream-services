package com.backbase.stream.portfolio.saga.wealth.asset;

import static com.backbase.stream.LambdaAssertions.assertEqualsTo;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import com.backbase.stream.portfolio.model.AssetClassBundle;
import com.backbase.stream.portfolio.model.WealthAssetBundle;
import com.backbase.stream.portfolio.service.InstrumentIntegrationService;
import com.backbase.stream.portfolio.util.PortfolioTestUtil;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * WealthAssetsSaga Test.
 * 
 * @author Vladimir Kirchev
 *
 */
@ExtendWith(MockitoExtension.class)
class WealthAssetsSagaTest {
    @Mock
    private InstrumentIntegrationService instrumentIntegrationService;

    @InjectMocks
    private WealthAssetsSaga wealthAssetsSaga;

    @Test
    void shouldExecuteTask() throws Exception {
        WealthAssetBundle wealthAssetBundle = PortfolioTestUtil.getWealthAssetBundle();
        List<AssetClassBundle> assetClasses = wealthAssetBundle.getAssetClasses();
        AssetClassBundle assetClassBundle = assetClasses.get(0);
        WealthAssetsTask wealthAssetsTask = new WealthAssetsTask(assetClassBundle);

        List<AssetClassBundle> assetClassBundles = List.of(assetClassBundle);

        Mockito.when(instrumentIntegrationService.upsertAssetClass(assetClassBundles))
                .thenReturn(Mono.just(assetClassBundles));

        Mono<WealthAssetsTask> task = wealthAssetsSaga.executeTask(wealthAssetsTask);

        Assertions.assertNotNull(task);

        StepVerifier.create(task).assertNext(assertEqualsTo(wealthAssetsTask)).verifyComplete();
    }

    @Test
    void shouldRollBack() throws Exception {
        WealthAssetBundle wealthAssetBundle = PortfolioTestUtil.getWealthAssetBundle();
        List<AssetClassBundle> assetClasses = wealthAssetBundle.getAssetClasses();
        AssetClassBundle assetClassBundle = assetClasses.get(0);
        WealthAssetsTask wealthAssetsTask = new WealthAssetsTask(assetClassBundle);

        Mono<WealthAssetsTask> mono = wealthAssetsSaga.rollBack(wealthAssetsTask);

        Assertions.assertNotNull(mono);

        StepVerifier.create(mono).assertNext(assertEqualsTo(wealthAssetsTask)).verifyComplete();
    }
}
