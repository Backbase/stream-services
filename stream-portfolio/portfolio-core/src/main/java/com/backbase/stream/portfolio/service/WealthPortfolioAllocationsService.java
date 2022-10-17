package com.backbase.stream.portfolio.service;

import com.backbase.stream.portfolio.model.AllocationBundle;
import reactor.core.publisher.Flux;

/**
 * WealthPortfolioAllocations Service.
 * 
 * @author Vladimir Kirchev
 *
 */
public interface WealthPortfolioAllocationsService {

    /**
     * Ingest PortfolioAllcoationsRegionBundles.
     * 
     * @param allocationBundle The {@code AllocationBundle} to be ingested.
     * @return The ingested {@code AllocationBundle}.
     */
    Flux<AllocationBundle> ingestPortfolioAllocationBundles(Flux<AllocationBundle> allocationBundle);
}
