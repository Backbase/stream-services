package com.backbase.stream.portfolio.service;

import com.backbase.stream.portfolio.model.AllocationBundle;
import com.backbase.stream.portfolio.model.HierarchyBundle;
import com.backbase.stream.portfolio.model.Portfolio;
import com.backbase.stream.portfolio.model.SubPortfolioBundle;
import com.backbase.stream.portfolio.model.TransactionCategory;
import com.backbase.stream.portfolio.model.ValuationsBundle;
import reactor.core.publisher.Flux;

/**
 * WealthAssets Service.
 * 
 * @author Vladimir Kirchev
 *
 */
public interface PortfolioIngestionService {

    /**
     * Ingest PortfolioAllcoationsRegionBundles.
     * 
     * @param allocationBundle The {@code AllocationBundle} to be ingested.
     * @return The ingested {@code AllocationBundle}.
     */
    Flux<AllocationBundle> ingestPortfolioAllocationBundles(Flux<AllocationBundle> allocationBundle);

    /**
     * Ingest Portfolios.
     * 
     * @param portfolio The Flux of {@code Portfolio} to be ingested.
     * @return The Flux of ingested {@code Portfolio}.
     */
    Flux<Portfolio> ingestWealthPortfolios(Flux<Portfolio> portfolio);

    /**
     * Ingest ValuationsBundles.
     * 
     * @param valuationsBundles The {@code ValuationsBundle} to be ingested.
     * @return The ingested {@code ValuationsBundle}.
     */
    Flux<ValuationsBundle> ingestValuationsBundles(Flux<ValuationsBundle> valuationsBundles);

    /**
     * Ingest SubPortfolios.
     * 
     * @param portfolio The Flux of {@code SubPortfolioBundle} to be ingested.
     * @return The Flux of ingested {@code SubPortfolioBundle}.
     */
    Flux<SubPortfolioBundle> ingestWealthSubPortfolios(Flux<SubPortfolioBundle> subPortfolioBundles);

    /**
     * Ingest TransactionCategories.
     * 
     * @param transactionCategories The {@code TransactionCategory} to be ingested.
     * @return The ingested {@code TransactionCategory}.
     */
    Flux<TransactionCategory> ingestTransactionCategories(Flux<TransactionCategory> transactionCategories);
    
    /**
     * Ingest Hierarchiy Bundles.
     * 
     * @param hierarchyBundles The Flux of {@code HierarchyBundle} to be ingested.
     * @return The Flux of ingested {@code HierarchyBundle}.
     */
    Flux<HierarchyBundle> ingestHierarchyBundles(Flux<HierarchyBundle> hierarchyBundles);
}
