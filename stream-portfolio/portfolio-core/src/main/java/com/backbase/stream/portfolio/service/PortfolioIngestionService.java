package com.backbase.stream.portfolio.service;

import com.backbase.stream.portfolio.model.AllocationBundle;
import com.backbase.stream.portfolio.model.HierarchyBundle;
import com.backbase.stream.portfolio.model.Portfolio;
import com.backbase.stream.portfolio.model.Position;
import com.backbase.stream.portfolio.model.SubPortfolioBundle;
import com.backbase.stream.portfolio.model.TransactionBundle;
import com.backbase.stream.portfolio.model.TransactionCategory;
import com.backbase.stream.portfolio.model.ValuationsBundle;
import reactor.core.publisher.Flux;

/**
 * PortfolioIngestion Service.
 * 
 * @author Vladimir Kirchev
 *
 */
public interface PortfolioIngestionService {

    /**
     * Ingest PortfolioAllcoationsRegionBundles.
     * 
     * @param allocationBundle The Flux of {@code AllocationBundle} to be ingested.
     * @return The Flux of ingested {@code AllocationBundle}.
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
     * @param valuationsBundles The Flux of {@code ValuationsBundle} to be ingested.
     * @return The Flux of ingested {@code ValuationsBundle}.
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
     * @param transactionCategories The Flux of {@code TransactionCategory} to be ingested.
     * @return The Flux of ingested {@code TransactionCategory}.
     */
    Flux<TransactionCategory> ingestTransactionCategories(Flux<TransactionCategory> transactionCategories);

    /**
     * Ingest Hierarchiy Bundles.
     * 
     * @param hierarchyBundles The Flux of {@code HierarchyBundle} to be ingested.
     * @return The Flux of ingested {@code HierarchyBundle}.
     */
    Flux<HierarchyBundle> ingestHierarchyBundles(Flux<HierarchyBundle> hierarchyBundles);

    /**
     * Ingest Position.
     * 
     * @param positions The Flux of {@code Position} to be ingested.
     * @return The Flux of the ingested {@code Position}.
     */
    Flux<Position> ingestPositions(Flux<Position> positions);
    
    /**
     * Ingest TransactionBundles.
     * 
     * @param transactionBundles The Flux of {@code TransactionBundle} to be ingested.
     * @return The Flux of the ingested {@code TransactionBundle}.
     */
    Flux<TransactionBundle> ingestTransactionBundles(Flux<TransactionBundle> transactionBundles);
}
