package com.backbase.stream.portfolio.saga.wealth.portfolio;

import static com.backbase.stream.LambdaAssertions.assertEqualsTo;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import com.backbase.stream.portfolio.model.Portfolio;
import com.backbase.stream.portfolio.model.WealthPortfolioBundle;
import com.backbase.stream.portfolio.service.PortfolioIntegrationService;
import com.backbase.stream.portfolio.util.PortfolioTestUtil;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * WealthPortfolioSaga Test.
 * 
 * @author Vladimir Kirchev
 *
 */
@ExtendWith(MockitoExtension.class)
class WealthPortfolioSagaTest {
    @Mock
    private PortfolioIntegrationService portfolioIntegrationService;

    @InjectMocks
    private WealthPortfolioSaga wealthPortfolioSaga;

    @Test
    void shouldExecuteTask() throws Exception {
        WealthPortfolioBundle wealthPortfolioBundle = PortfolioTestUtil.getWealthPortfolioBundle();
        List<Portfolio> portfolios = wealthPortfolioBundle.getPortfolios();
        Portfolio portfolio = portfolios.get(0);
        WealthPortfolioTask wealthPortfolioTask = new WealthPortfolioTask(portfolio);

        Mockito.when(portfolioIntegrationService.upsertPortfolio(portfolio)).thenReturn(Mono.just(portfolio));

        Mono<WealthPortfolioTask> task = wealthPortfolioSaga.executeTask(wealthPortfolioTask);

        Assertions.assertNotNull(task);

        StepVerifier.create(task).assertNext(assertEqualsTo(wealthPortfolioTask)).verifyComplete();
    }

    @Test
    void shouldRollBack() throws Exception {
        WealthPortfolioBundle wealthPortfolioBundle = PortfolioTestUtil.getWealthPortfolioBundle();
        List<Portfolio> portfolios = wealthPortfolioBundle.getPortfolios();
        Portfolio portfolio = portfolios.get(0);
        WealthPortfolioTask wealthPortfolioTask = new WealthPortfolioTask(portfolio);

        Mono<WealthPortfolioTask> mono = wealthPortfolioSaga.rollBack(wealthPortfolioTask);

        Assertions.assertNotNull(mono);

        StepVerifier.create(mono).assertNext(assertEqualsTo(wealthPortfolioTask)).verifyComplete();
    }
}
