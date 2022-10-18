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
import com.backbase.stream.portfolio.model.SubPortfolioBundle;
import com.backbase.stream.portfolio.model.WealthSubPortfolioBundle;
import com.backbase.stream.portfolio.service.PortfolioIntegrationService;
import com.backbase.stream.portfolio.util.PortfolioTestUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * WealthSubPortfolioReactiveService Test.
 * 
 * @author Vladimir Kirchev
 *
 */
@ExtendWith(MockitoExtension.class)
class WealthSubPortfolioReactiveServiceTest {
    @Mock
    private PortfolioSagaProperties portfolioSagaProperties;

    @Mock
    private PortfolioIntegrationService portfolioIntegrationService;

    @InjectMocks
    private WealthSubPortfolioReactiveService wealthSubPortfolioReactiveService;

    @Test
    void shouldIngestPortfolioAllocationBundles() throws Exception {
        WealthSubPortfolioBundle wealthSubPortfolioBundle = PortfolioTestUtil.getWealthSubPortfolioBundle();
        List<SubPortfolioBundle> batchSubPortfolios = wealthSubPortfolioBundle.getBatchSubPortfolios();

        SubPortfolioBundle subPortfolioBundle0 = batchSubPortfolios.get(0);
        SubPortfolioBundle subPortfolioBundle1 = batchSubPortfolios.get(1);

        Mockito.when(portfolioSagaProperties.getTaskExecutors()).thenReturn(1);
        Mockito.when(portfolioIntegrationService.upsertSubPortfolios(any(), any()))
                .thenAnswer(i -> Mono.just(i.getArgument(0)));

        Flux<SubPortfolioBundle> ingestedWealthSubPortfolios =
                wealthSubPortfolioReactiveService.ingestWealthSubPortfolios(Flux.fromIterable(batchSubPortfolios));

        Assertions.assertNotNull(ingestedWealthSubPortfolios);

        StepVerifier.create(ingestedWealthSubPortfolios).assertNext(assertEqualsTo(subPortfolioBundle0))
                .assertNext(assertEqualsTo(subPortfolioBundle1)).verifyComplete();
    }
}
