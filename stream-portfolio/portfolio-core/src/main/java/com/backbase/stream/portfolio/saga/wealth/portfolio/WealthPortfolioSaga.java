package com.backbase.stream.portfolio.saga.wealth.portfolio;

import org.springframework.cloud.sleuth.annotation.ContinueSpan;
import org.springframework.cloud.sleuth.annotation.SpanTag;
import com.backbase.stream.portfolio.service.PortfolioIntegrationService;
import com.backbase.stream.worker.StreamTaskExecutor;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * WealthPortfolio Saga.
 * 
 * @author Vladimir Kirchev
 *
 */
@RequiredArgsConstructor
public class WealthPortfolioSaga implements StreamTaskExecutor<WealthPortfolioTask> {
    private static final String PORTFOLIO_ENTITY = "PORTFOLIO_ENTITY";
    private static final String UPSERT = "upsert";
    private static final String UPSERT_PORTFOLIOS = "upsert-portfolios";

    private final PortfolioIntegrationService portfolioIntegrationService;

    @Override
    public Mono<WealthPortfolioTask> executeTask(WealthPortfolioTask streamTask) {
        return upsertPortfolios(streamTask);
    }

    @Override
    public Mono<WealthPortfolioTask> rollBack(WealthPortfolioTask streamTask) {
        return Mono.just(streamTask);
    }

    @ContinueSpan(log = UPSERT_PORTFOLIOS)
    private Mono<WealthPortfolioTask> upsertPortfolios(@SpanTag(value = "streamTask") WealthPortfolioTask task) {
        task.info(PORTFOLIO_ENTITY, UPSERT, null, null, null, "Upsert portfolio");

        return portfolioIntegrationService.upsertPortfolio(task.getData()).map(o -> task);
    }

}
