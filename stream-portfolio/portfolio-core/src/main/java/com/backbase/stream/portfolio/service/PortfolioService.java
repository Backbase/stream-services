package com.backbase.stream.portfolio.service;

import com.backbase.stream.portfolio.model.WealthBundle;
import reactor.core.publisher.Flux;

/**
 * Portfolio Service.
 * 
 * @author Vladimir Kirchev
 *
 */
public interface PortfolioService {

    /**
     * Ingest Wealth Bundles.
     * 
     * @param wealthBundles The {@code WealthBundle} to be ingested.
     * @return The ingested {@code WealthBundle}.
     */
    Flux<WealthBundle> ingestWealthBundles(Flux<WealthBundle> wealthBundles);
}
