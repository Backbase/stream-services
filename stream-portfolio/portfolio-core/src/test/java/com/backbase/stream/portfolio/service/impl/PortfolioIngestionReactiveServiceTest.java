package com.backbase.stream.portfolio.service.impl;

import static com.backbase.stream.LambdaAssertions.assertEqualsTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import com.backbase.stream.portfolio.configuration.PortfolioSagaProperties;
import com.backbase.stream.portfolio.model.AllocationBundle;
import com.backbase.stream.portfolio.model.HierarchyBundle;
import com.backbase.stream.portfolio.model.Portfolio;
import com.backbase.stream.portfolio.model.SubPortfolioBundle;
import com.backbase.stream.portfolio.model.TransactionCategory;
import com.backbase.stream.portfolio.model.ValuationsBundle;
import com.backbase.stream.portfolio.model.WealthPortfolioAllocationsBundle;
import com.backbase.stream.portfolio.model.WealthPortfolioBundle;
import com.backbase.stream.portfolio.model.WealthPortfolioPositionHierarchyBundle;
import com.backbase.stream.portfolio.model.WealthPortfolioValuationsBundle;
import com.backbase.stream.portfolio.model.WealthSubPortfolioBundle;
import com.backbase.stream.portfolio.model.WealthTransactionCategoriesBundle;
import com.backbase.stream.portfolio.service.PortfolioIntegrationService;
import com.backbase.stream.portfolio.util.PortfolioTestUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * PortfolioIngestionReactiveService Test.
 * 
 * @author Vladimir Kirchev
 *
 */
@ExtendWith(MockitoExtension.class)
class PortfolioIngestionReactiveServiceTest {
    @Mock
    private PortfolioSagaProperties portfolioSagaProperties;

    @Mock
    private PortfolioIntegrationService portfolioIntegrationService;

    @InjectMocks
    private PortfolioIngestionReactiveService portfolioIngestionReactiveService;

    @Test
    void shouldIngestPortfolioAllocationBundles() throws Exception {
        WealthPortfolioAllocationsBundle wealthPortfolioAllocationsBundle =
                PortfolioTestUtil.getWealthPortfolioAllocationsBundle();
        List<AllocationBundle> batchPortfolioAllocations =
                wealthPortfolioAllocationsBundle.getBatchPortfolioAllocations();

        AllocationBundle allocationBundle0 = batchPortfolioAllocations.get(0);
        AllocationBundle allocationBundle1 = batchPortfolioAllocations.get(1);

        Mockito.when(portfolioSagaProperties.getTaskExecutors()).thenReturn(1);
        Mockito.when(portfolioIntegrationService.upsertAllocations(any(), any()))
                .thenAnswer(i -> Mono.just(i.getArgument(0)));

        Flux<AllocationBundle> ingestedPortfolioAllocationBundles = portfolioIngestionReactiveService
                .ingestPortfolioAllocationBundles(Flux.fromIterable(batchPortfolioAllocations));

        Assertions.assertNotNull(ingestedPortfolioAllocationBundles);

        StepVerifier.create(ingestedPortfolioAllocationBundles)
                .assertNext(assertEqualsTo(allocationBundle0))
                .assertNext(assertEqualsTo(allocationBundle1))
                .verifyComplete();
    }

    @Test
    void shouldIngestWealthPortfolioBundles() throws Exception {
        WealthPortfolioBundle wealthPortfolioBundle = PortfolioTestUtil.getWealthPortfolioBundle();
        List<Portfolio> portfolios = wealthPortfolioBundle.getPortfolios();

        Portfolio portfolio0 = portfolios.get(0);
        Portfolio portfolio1 = portfolios.get(1);

        Mockito.when(portfolioSagaProperties.getTaskExecutors()).thenReturn(1);
        Mockito.when(portfolioIntegrationService.upsertPortfolio(any(Portfolio.class)))
                .thenAnswer(i -> Mono.just(i.getArgument(0)));

        Flux<Portfolio> ingestedWealthPortfolios =
                portfolioIngestionReactiveService.ingestWealthPortfolios(Flux.fromIterable(portfolios));

        Assertions.assertNotNull(ingestedWealthPortfolios);

        StepVerifier.create(ingestedWealthPortfolios)
                .assertNext(assertEqualsTo(portfolio0))
                .assertNext(assertEqualsTo(portfolio1))
                .verifyComplete();
    }

    @Test
    void shouldIngestWealthPortfolioValuationsBundles() throws Exception {
        WealthPortfolioValuationsBundle wealthPortfolioValuationsBundle =
                PortfolioTestUtil.getWealthPortfolioValuationsBundle();
        List<ValuationsBundle> batchPortfolioValuations = wealthPortfolioValuationsBundle.getBatchPortfolioValuations();

        ValuationsBundle valuationsBundle0 = batchPortfolioValuations.get(0);

        Mockito.when(portfolioSagaProperties.getTaskExecutors()).thenReturn(1);
        Mockito.when(portfolioIntegrationService.upsertPortfolioValuations(any(), any()))
                .thenAnswer(i -> Mono.just(i.getArgument(0)));

        Flux<ValuationsBundle> ingestedValuationsBundles =
                portfolioIngestionReactiveService.ingestValuationsBundles(Flux.fromIterable(batchPortfolioValuations));

        Assertions.assertNotNull(ingestedValuationsBundles);

        StepVerifier.create(ingestedValuationsBundles).assertNext(assertEqualsTo(valuationsBundle0)).verifyComplete();
    }

    @Test
    void shouldIngestWealthSubPortfolioBundles() throws Exception {
        WealthSubPortfolioBundle wealthSubPortfolioBundle = PortfolioTestUtil.getWealthSubPortfolioBundle();
        List<SubPortfolioBundle> batchSubPortfolios = wealthSubPortfolioBundle.getBatchSubPortfolios();

        SubPortfolioBundle subPortfolioBundle0 = batchSubPortfolios.get(0);
        SubPortfolioBundle subPortfolioBundle1 = batchSubPortfolios.get(1);

        Mockito.when(portfolioSagaProperties.getTaskExecutors()).thenReturn(1);
        Mockito.when(portfolioIntegrationService.upsertSubPortfolios(any(), any()))
                .thenAnswer(i -> Mono.just(i.getArgument(0)));

        Flux<SubPortfolioBundle> ingestedWealthSubPortfolios =
                portfolioIngestionReactiveService.ingestWealthSubPortfolios(Flux.fromIterable(batchSubPortfolios));

        Assertions.assertNotNull(ingestedWealthSubPortfolios);

        StepVerifier.create(ingestedWealthSubPortfolios)
                .assertNext(assertEqualsTo(subPortfolioBundle0))
                .assertNext(assertEqualsTo(subPortfolioBundle1))
                .verifyComplete();
    }

    @Test
    void shouldIngestWealthTransactionCategoriesBundles() throws Exception {
        WealthTransactionCategoriesBundle wealthTransactionCategoriesBundle =
                PortfolioTestUtil.getWealthTransactionCategoriesBundle();
        List<TransactionCategory> transactionCategories = wealthTransactionCategoriesBundle.getTransactionCategories();

        TransactionCategory transactionCategory0 = transactionCategories.get(0);
        TransactionCategory transactionCategory1 = transactionCategories.get(1);

        Mockito.when(portfolioSagaProperties.getTaskExecutors()).thenReturn(1);
        Mockito.when(portfolioIntegrationService.upsertTransactionCategories(any()))
                .thenAnswer(i -> Mono.just(i.getArgument(0)));

        Flux<TransactionCategory> ingestedTransactionCategories =
                portfolioIngestionReactiveService.ingestTransactionCategories(Flux.fromIterable(transactionCategories));

        Assertions.assertNotNull(ingestedTransactionCategories);

        StepVerifier.create(ingestedTransactionCategories)
                .assertNext(assertEqualsTo(transactionCategory0))
                .assertNext(assertEqualsTo(transactionCategory1))
                .verifyComplete();
    }

    @Test
    void shouldIngestWealthPortfolioPositionHierarchyBundles() throws Exception {
        WealthPortfolioPositionHierarchyBundle wealthPortfolioPositionHierarchyBundle =
                PortfolioTestUtil.getWealthPortfolioPositionHierarchyBundle();
        List<HierarchyBundle> batchPortfolioPositionsHierarchies =
                wealthPortfolioPositionHierarchyBundle.getBatchPortfolioPositionsHierarchies();

        HierarchyBundle hierarchyBundle0 = batchPortfolioPositionsHierarchies.get(0);

        Mockito.when(portfolioSagaProperties.getTaskExecutors()).thenReturn(1);
        Mockito.when(portfolioIntegrationService.upsertHierarchies(any(), anyString()))
                .thenAnswer(i -> Mono.just(i.getArgument(0)));

        Flux<HierarchyBundle> ingestedHierarchyBundles = portfolioIngestionReactiveService
                .ingestHierarchyBundles(Flux.fromIterable(batchPortfolioPositionsHierarchies));

        Assertions.assertNotNull(ingestedHierarchyBundles);

        StepVerifier.create(ingestedHierarchyBundles).assertNext(assertEqualsTo(hierarchyBundle0)).verifyComplete();
    }
}
