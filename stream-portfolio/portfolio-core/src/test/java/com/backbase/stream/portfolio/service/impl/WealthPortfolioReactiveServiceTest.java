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
import com.backbase.stream.portfolio.model.Portfolio;
import com.backbase.stream.portfolio.model.WealthPortfolioBundle;
import com.backbase.stream.portfolio.saga.wealth.portfolio.WealthPortfolioSaga;
import com.backbase.stream.portfolio.util.PortfolioTestUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * WealthPortfolioReactiveService Test.
 * 
 * @author Vladimir Kirchev
 *
 */
@ExtendWith(MockitoExtension.class)
class WealthPortfolioReactiveServiceTest {
    @Mock
    private PortfolioSagaProperties portfolioSagaProperties;

    @Mock
    private WealthPortfolioSaga wealthPortfolioSaga;

    @InjectMocks
    private WealthPortfolioReactiveService wealthPortfolioReactiveService;

    @Test
    void shouldIngestPortfolioAllocationBundles() throws Exception {
        WealthPortfolioBundle wealthPortfolioBundle = PortfolioTestUtil.getWealthPortfolioBundle();
        List<Portfolio> portfolios = wealthPortfolioBundle.getPortfolios();

        Portfolio portfolio0 = portfolios.get(0);
        Portfolio portfolio1 = portfolios.get(1);

        Mockito.when(portfolioSagaProperties.getTaskExecutors()).thenReturn(1);
        Mockito.when(wealthPortfolioSaga.executeTask(any())).thenAnswer(i -> Mono.just(i.getArgument(0)));

        Flux<Portfolio> ingestedWealthPortfolios =
                wealthPortfolioReactiveService.ingestWealthPortfolios(Flux.fromIterable(portfolios));

        Assertions.assertNotNull(ingestedWealthPortfolios);

        StepVerifier.create(ingestedWealthPortfolios).assertNext(assertEqualsTo(portfolio0))
                .assertNext(assertEqualsTo(portfolio1)).verifyComplete();
    }
}
