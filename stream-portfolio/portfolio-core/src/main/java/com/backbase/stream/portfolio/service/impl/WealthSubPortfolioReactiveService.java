package com.backbase.stream.portfolio.service.impl;

import com.backbase.stream.portfolio.configuration.PortfolioSagaProperties;
import com.backbase.stream.portfolio.model.SubPortfolioBundle;
import com.backbase.stream.portfolio.service.PortfolioIntegrationService;
import com.backbase.stream.portfolio.service.WealthSubPortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    private final PortfolioIntegrationService portfolioIntegrationService;

    @Override
    public Flux<SubPortfolioBundle> ingestWealthSubPortfolios(Flux<SubPortfolioBundle> subPortfolioBundles) {
        return subPortfolioBundles.flatMap(this::upsertSubPortfolios, portfolioSagaProperties.getTaskExecutors())
                .doOnNext(actual -> log.info("Finished Ingestion of subPortfolio, portfolioCode: {}",
                        actual.getPortfolioCode()));
    }

    private Mono<SubPortfolioBundle> upsertSubPortfolios(SubPortfolioBundle subPortfolioBundle) {
        return portfolioIntegrationService
                .upsertSubPortfolios(subPortfolioBundle.getSubPortfolios(), subPortfolioBundle.getPortfolioCode())
                .map(m -> subPortfolioBundle);
    }

}
