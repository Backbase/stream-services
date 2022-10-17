package com.backbase.stream.portfolio.service;

import com.backbase.stream.portfolio.model.SubPortfolioBundle;
import reactor.core.publisher.Flux;

/**
 * WealthSubPortfolio Service.
 * 
 * @author Vladimir Kirchev
 *
 */
public interface WealthSubPortfolioService {

    /**
     * Ingest SubPortfolios.
     * 
     * @param portfolio The Flux of {@code SubPortfolioBundle} to be ingested.
     * @return The Flux of ingested {@code SubPortfolioBundle}.
     */
    Flux<SubPortfolioBundle> ingestWealthSubPortfolios(Flux<SubPortfolioBundle> subPortfolioBundles);
}
