package com.backbase.stream.portfolio.saga.wealth.subportfolio;

import static com.backbase.stream.LambdaAssertions.assertEqualsTo;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import com.backbase.stream.portfolio.model.SubPortfolio;
import com.backbase.stream.portfolio.model.SubPortfolioBundle;
import com.backbase.stream.portfolio.model.WealthSubPortfolioBundle;
import com.backbase.stream.portfolio.service.PortfolioIntegrationService;
import com.backbase.stream.portfolio.util.PortfolioTestUtil;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * WealthSubPortfolioSaga Test.
 * 
 * @author Vladimir Kirchev
 *
 */
@ExtendWith(MockitoExtension.class)
class WealthSubPortfolioSagaTest {
    @Mock
    private PortfolioIntegrationService portfolioIntegrationService;

    @InjectMocks
    private WealthSubPortfolioSaga wealthSubPortfolioSaga;

    @Test
    void shouldExecuteTask() throws Exception {
        WealthSubPortfolioBundle wealthSubPortfolioBundle = PortfolioTestUtil.getWealthSubPortfolioBundle();
        List<SubPortfolioBundle> batchSubPortfolios = wealthSubPortfolioBundle.getBatchSubPortfolios();
        SubPortfolioBundle subPortfolioBundle = batchSubPortfolios.get(0);
        WealthSubPortfolioTask wealthSubPortfolioTask = new WealthSubPortfolioTask(subPortfolioBundle);

        List<SubPortfolio> subPortfolios = subPortfolioBundle.getSubPortfolios();
        String portfolioCode = subPortfolioBundle.getPortfolioCode();

        Mockito.when(portfolioIntegrationService.upsertSubPortfolios(subPortfolios, portfolioCode))
                .thenReturn(Mono.just(subPortfolios));

        Mono<WealthSubPortfolioTask> task = wealthSubPortfolioSaga.executeTask(wealthSubPortfolioTask);

        Assertions.assertNotNull(task);

        StepVerifier.create(task).assertNext(assertEqualsTo(wealthSubPortfolioTask)).verifyComplete();
    }

    @Test
    void shouldRollBack() throws Exception {
        WealthSubPortfolioBundle wealthSubPortfolioBundle = PortfolioTestUtil.getWealthSubPortfolioBundle();
        List<SubPortfolioBundle> batchSubPortfolios = wealthSubPortfolioBundle.getBatchSubPortfolios();
        SubPortfolioBundle subPortfolioBundle = batchSubPortfolios.get(0);
        WealthSubPortfolioTask wealthSubPortfolioTask = new WealthSubPortfolioTask(subPortfolioBundle);

        Mono<WealthSubPortfolioTask> mono = wealthSubPortfolioSaga.rollBack(wealthSubPortfolioTask);

        Assertions.assertNotNull(mono);

        StepVerifier.create(mono).assertNext(assertEqualsTo(wealthSubPortfolioTask)).verifyComplete();
    }
}
