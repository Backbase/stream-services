package com.backbase.stream.portfolio.service.impl;

import com.backbase.stream.portfolio.configuration.PortfolioSagaProperties;
import com.backbase.stream.portfolio.model.SubPortfolioBundle;
import com.backbase.stream.portfolio.saga.wealth.subportfolio.WealthSubPortfolioSaga;
import com.backbase.stream.portfolio.saga.wealth.subportfolio.WealthSubPortfolioTask;
import com.backbase.stream.portfolio.service.WealthSubPortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * Reactive implementation of WealthSubPortfolioService.
 * 
 * @author Vladimir Kirchev
 *
 */
@Slf4j
@RequiredArgsConstructor
public class WealthSubPortfolioReactiveService implements WealthSubPortfolioService {

    private final PortfolioSagaProperties portfolioSagaProperties;
    private final WealthSubPortfolioSaga wealthSubPortfolioSaga;

    @Override
    public Flux<SubPortfolioBundle> ingestWealthSubPortfolios(Flux<SubPortfolioBundle> subPortfolioBundles) {
        return subPortfolioBundles.map(WealthSubPortfolioTask::new)
                .flatMap(wealthSubPortfolioSaga::executeTask, portfolioSagaProperties.getTaskExecutors())
                .map(WealthSubPortfolioTask::getData).doOnNext(actual -> log
                        .info("Finished Ingestion of subPortfolio, portfolioCode: {}", actual.getPortfolioCode()));
    }

}
