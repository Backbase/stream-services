package com.backbase.stream.portfolio.service.impl;

import com.backbase.stream.portfolio.configuration.PortfolioSagaProperties;
import com.backbase.stream.portfolio.model.Portfolio;
import com.backbase.stream.portfolio.saga.wealth.portfolio.WealthPortfolioSaga;
import com.backbase.stream.portfolio.saga.wealth.portfolio.WealthPortfolioTask;
import com.backbase.stream.portfolio.service.WealthPortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * WealthPortfolioReactiveService.
 * 
 * @author Vladimir Kirchev
 *
 */
@Slf4j
@RequiredArgsConstructor
public class WealthPortfolioReactiveService implements WealthPortfolioService {

    private final PortfolioSagaProperties portfolioSagaProperties;
    private final WealthPortfolioSaga wealthPortfolioSaga;

    @Override
    public Flux<Portfolio> ingestWealthPortfolios(Flux<Portfolio> portfolio) {
        return portfolio.map(WealthPortfolioTask::new)
                .flatMap(wealthPortfolioSaga::executeTask, portfolioSagaProperties.getTaskExecutors())
                .map(WealthPortfolioTask::getData)
                .doOnNext(actual -> log.info("Finished Ingestion of portfolio, name: {}", actual.getName()));
    }

}
