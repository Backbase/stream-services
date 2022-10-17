package com.backbase.stream.portfolio.saga.wealth.subportfolio;

import org.springframework.cloud.sleuth.annotation.ContinueSpan;
import org.springframework.cloud.sleuth.annotation.SpanTag;
import com.backbase.stream.portfolio.model.SubPortfolioBundle;
import com.backbase.stream.portfolio.service.PortfolioIntegrationService;
import com.backbase.stream.worker.StreamTaskExecutor;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * WealthSubPortfolio Saga.
 * 
 * @author Vladimir Kirchev
 *
 */
@RequiredArgsConstructor
public class WealthSubPortfolioSaga implements StreamTaskExecutor<WealthSubPortfolioTask> {
    private static final String SUBPORTFOLIO_ENTITY = "SUBPORTFOLIO_ENTITY";
    private static final String UPSERT = "upsert";
    private static final String UPSERT_SUBPORTFOLIOS = "upsert-subportfolios";

    private final PortfolioIntegrationService portfolioIntegrationService;

    @Override
    public Mono<WealthSubPortfolioTask> executeTask(WealthSubPortfolioTask streamTask) {
        return upsertSubPortfolios(streamTask);
    }

    @Override
    public Mono<WealthSubPortfolioTask> rollBack(WealthSubPortfolioTask streamTask) {
        return Mono.just(streamTask);
    }

    @ContinueSpan(log = UPSERT_SUBPORTFOLIOS)
    private Mono<WealthSubPortfolioTask> upsertSubPortfolios(
            @SpanTag(value = "streamTask") WealthSubPortfolioTask task) {
        task.info(SUBPORTFOLIO_ENTITY, UPSERT, null, null, null, "Upsert subportfolio");

        SubPortfolioBundle subPortfolioBundle = task.getData();

        return portfolioIntegrationService
                .upsertSubPortfolios(subPortfolioBundle.getSubPortfolios(), subPortfolioBundle.getPortfolioCode())
                .map(o -> task);
    }

}
