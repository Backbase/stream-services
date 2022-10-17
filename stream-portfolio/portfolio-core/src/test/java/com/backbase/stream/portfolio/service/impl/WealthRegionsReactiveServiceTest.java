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
import com.backbase.stream.portfolio.model.RegionBundle;
import com.backbase.stream.portfolio.model.WealthRegionsBundle;
import com.backbase.stream.portfolio.saga.wealth.region.WealthRegionsSaga;
import com.backbase.stream.portfolio.util.PortfolioTestUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * WealthRegionsReactiveService Test.
 * 
 * @author Vladimir Kirchev
 *
 */
@ExtendWith(MockitoExtension.class)
class WealthRegionsReactiveServiceTest {

    @Mock
    private PortfolioSagaProperties portfolioSagaProperties;

    @Mock
    private WealthRegionsSaga wealthRegionsSaga;

    @InjectMocks
    private WealthRegionsReactiveService wealthRegionsReactiveService;

    @Test
    void shouldIngestRegionBundles() throws Exception {
        WealthRegionsBundle wealthRegionsBundle = PortfolioTestUtil.getWealthRegionsBundle();
        List<RegionBundle> regionBundles = wealthRegionsBundle.getRegions();

        RegionBundle regionBundle0 = regionBundles.get(0);
        RegionBundle regionBundle1 = regionBundles.get(1);

        Mockito.when(portfolioSagaProperties.getTaskExecutors()).thenReturn(1);
        Mockito.when(wealthRegionsSaga.executeTask(any())).thenAnswer(i -> Mono.just(i.getArgument(0)));

        Flux<RegionBundle> ingestedRegionBundles =
                wealthRegionsReactiveService.ingestRegionBundles(Flux.fromIterable(regionBundles));

        Assertions.assertNotNull(ingestedRegionBundles);

        StepVerifier.create(ingestedRegionBundles).assertNext(assertEqualsTo(regionBundle0))
                .assertNext(assertEqualsTo(regionBundle1)).verifyComplete();
    }
}
