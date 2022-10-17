package com.backbase.stream.portfolio.saga.wealth.region;

import static com.backbase.stream.LambdaAssertions.assertEqualsTo;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import com.backbase.stream.portfolio.model.RegionBundle;
import com.backbase.stream.portfolio.model.WealthRegionsBundle;
import com.backbase.stream.portfolio.service.InstrumentIntegrationService;
import com.backbase.stream.portfolio.util.PortfolioTestUtil;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * WealthRegionsSaga Test.
 * 
 * @author Vladimir Kirchev
 *
 */
@ExtendWith(MockitoExtension.class)
class WealthRegionsSagaTest {

    @Mock
    private InstrumentIntegrationService instrumentIntegrationService;

    @InjectMocks
    private WealthRegionsSaga wealthRegionsSaga;

    @Test
    void shouldExecuteTask() throws Exception {
        WealthRegionsBundle wealthRegionsBundle = PortfolioTestUtil.getWealthRegionsBundle();
        List<RegionBundle> regions = wealthRegionsBundle.getRegions();
        RegionBundle regionBundle = regions.get(0);
        WealthRegionsTask wealthRegionsTask = new WealthRegionsTask(regionBundle);

        List<RegionBundle> regionBundles = List.of(regionBundle);

        Mockito.when(instrumentIntegrationService.upsertRegions(regionBundles)).thenReturn(Mono.just(regionBundles));

        Mono<WealthRegionsTask> task = wealthRegionsSaga.executeTask(wealthRegionsTask);

        Assertions.assertNotNull(task);

        StepVerifier.create(task).assertNext(assertEqualsTo(wealthRegionsTask)).verifyComplete();
    }

    @Test
    void shouldRollBack() throws Exception {
        WealthRegionsBundle wealthRegionsBundle = PortfolioTestUtil.getWealthRegionsBundle();
        List<RegionBundle> regions = wealthRegionsBundle.getRegions();
        RegionBundle regionBundle = regions.get(0);
        WealthRegionsTask wealthRegionsTask = new WealthRegionsTask(regionBundle);

        Mono<WealthRegionsTask> mono = wealthRegionsSaga.rollBack(wealthRegionsTask);

        Assertions.assertNotNull(mono);

        StepVerifier.create(mono).assertNext(assertEqualsTo(wealthRegionsTask)).verifyComplete();
    }
}
