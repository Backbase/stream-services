package com.backbase.stream.portfolio.service;

import com.backbase.stream.portfolio.model.Portfolio;
import reactor.core.publisher.Flux;

/**
 * WealthPortfolioService Service.
 * 
 * @author Vladimir Kirchev
 *
 */
public interface WealthPortfolioService {

    /**
     * Ingest Portfolios.
     * 
     * @param portfolio The Flux of {@code Portfolio} to be ingested.
     * @return The Flux of ingested {@code Portfolio}.
     */
    Flux<Portfolio> ingestWealthPortfolios(Flux<Portfolio> portfolio);
}
