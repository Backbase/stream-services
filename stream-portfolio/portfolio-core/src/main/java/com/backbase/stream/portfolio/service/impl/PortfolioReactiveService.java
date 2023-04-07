package com.backbase.stream.portfolio.service.impl;

import com.backbase.stream.portfolio.PortfolioSaga;
import com.backbase.stream.portfolio.PortfolioTask;
import com.backbase.stream.portfolio.configuration.PortfolioSagaProperties;
import com.backbase.stream.portfolio.model.WealthBundle;
import com.backbase.stream.portfolio.service.PortfolioService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import reactor.core.publisher.Flux;

/**
 * Reactive implementation of {@code PortfolioService}.
 *
 * @author Vladimir Kirchev
 */
@Slf4j
@RequiredArgsConstructor
public class PortfolioReactiveService implements PortfolioService {

    private final PortfolioSagaProperties portfolioSagaProperties;
    private final PortfolioSaga portfolioSaga;

    @Override
    public Flux<WealthBundle> ingestWealthBundles(Flux<WealthBundle> wealthBundles) {
        return wealthBundles
                .map(PortfolioTask::new)
                .flatMap(portfolioSaga::executeTask, portfolioSagaProperties.getTaskExecutors())
                .map(PortfolioTask::getData)
                .doOnNext(
                        actual ->
                                log.info(
                                        "Finished Ingestion of portfolio, wealthBundle: {}",
                                        actual));
    }
}
