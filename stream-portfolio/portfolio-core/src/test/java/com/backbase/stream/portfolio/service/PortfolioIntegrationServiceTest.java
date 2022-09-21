package com.backbase.stream.portfolio.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.portfolio.api.service.integration.v1.model.AggregatePortfoliosPostRequest;
import com.backbase.portfolio.api.service.integration.v1.model.AllocationClassifierType;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioAllocationsParentItem;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioAllocationsPutRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioBenchmarkPostRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioCumulativePerformancesItem;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioCumulativePerformancesPutRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioPositionTransactionsPostItem;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioPositionsHierarchyItem;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioPositionsHierarchyItem.ItemTypeEnum;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioPositionsHierarchyPutRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioTransactionsPostItem;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioTransactionsPostRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioValuationsItem;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioValuationsPutRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PortfoliosPostRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PositionsPostRequest;
import com.backbase.portfolio.api.service.integration.v1.model.SubPortfoliosPostRequest;
import com.backbase.portfolio.api.service.integration.v1.model.TransactionCategoryPostRequest;
import com.backbase.portfolio.integration.api.service.v1.AggregatePortfolioManagementApi;
import com.backbase.portfolio.integration.api.service.v1.PortfolioBenchmarksManagementApi;
import com.backbase.portfolio.integration.api.service.v1.PortfolioCumulativePerformanceManagementApi;
import com.backbase.portfolio.integration.api.service.v1.PortfolioManagementApi;
import com.backbase.portfolio.integration.api.service.v1.PortfolioPositionsHierarchyManagementApi;
import com.backbase.portfolio.integration.api.service.v1.PortfolioValuationManagementApi;
import com.backbase.portfolio.integration.api.service.v1.PositionManagementApi;
import com.backbase.portfolio.integration.api.service.v1.SubPortfolioManagementApi;
import com.backbase.portfolio.integration.api.service.v1.TransactionCategoryManagementApi;
import com.backbase.portfolio.integration.api.service.v1.TransactionManagementApi;
import com.backbase.stream.portfolio.mapper.PortfolioMapper;
import com.backbase.stream.portfolio.model.AggregatePortfolio;
import com.backbase.stream.portfolio.model.Allocation;
import com.backbase.stream.portfolio.model.Portfolio;
import com.backbase.stream.portfolio.model.PortfolioBenchmark;
import com.backbase.stream.portfolio.model.PortfolioBundle;
import com.backbase.stream.portfolio.model.PortfolioCumulativePerformances;
import com.backbase.stream.portfolio.model.PortfolioPositionsHierarchy;
import com.backbase.stream.portfolio.model.PortfolioValuation;
import com.backbase.stream.portfolio.model.Position;
import com.backbase.stream.portfolio.model.PositionBundle;
import com.backbase.stream.portfolio.model.PositionTransaction;
import com.backbase.stream.portfolio.model.SubPortfolio;
import com.backbase.stream.portfolio.model.TransactionCategory;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class PortfolioIntegrationServiceTest {

    @SuppressWarnings("unused")
    @Spy
    private PortfolioMapper portfolioMapper;
    @Mock
    private PortfolioManagementApi portfolioManagementApi;
    @Mock
    private PositionManagementApi positionManagementApi;
    @Mock
    private TransactionManagementApi transactionManagementApi;
    @Mock
    private TransactionCategoryManagementApi transactionCategoryManagementApi;
    @Mock
    private SubPortfolioManagementApi subPortfolioManagementApi;
    @Mock
    private PortfolioPositionsHierarchyManagementApi portfolioPositionsHierarchyManagementApi;
    @Mock
    private PortfolioCumulativePerformanceManagementApi portfolioCumulativePerformanceManagementApi;
    @Mock
    private PortfolioBenchmarksManagementApi portfolioBenchmarksManagementApi;
    @Mock
    private PortfolioValuationManagementApi portfolioValuationManagementApi;
    @Mock
    private AggregatePortfolioManagementApi aggregatePortfolioManagementApi;

    @InjectMocks
    private PortfolioIntegrationService portfolioIntegrationService;

    @Test
    void createAggregatePortfolio() {
        String arrangementId = "arrangementId";
        AggregatePortfolio aggregatePortfolios = new AggregatePortfolio().id(arrangementId);

        when(aggregatePortfolioManagementApi
            .postAggregatePortfolios(any(AggregatePortfoliosPostRequest.class)))
            .thenReturn(Mono.empty());

        portfolioIntegrationService.createAggregatePortfolio(aggregatePortfolios).block();

        verify(aggregatePortfolioManagementApi).postAggregatePortfolios(new AggregatePortfoliosPostRequest()
            .id(arrangementId));

    }

    @Test
    void createPosition() {
        String positionId = "positionId";
        String instrumentId = "instrumentId";
        String portfolioId = "portfolioId";
        String subPortfolioId = "subPortfolioId";
        String transactionCategoryKey = "transactionCategoryKey";
        String categoryAlias = "categoryAlias";
        String exchange = "exchange";

        PositionBundle positionBundle = new PositionBundle()
            .portfolioId(portfolioId)
            .subPortfolioId(subPortfolioId)
            .position(new Position().instrumentId(instrumentId).externalId(positionId))
            .addTransactionCategoriesItem(new TransactionCategory().key(transactionCategoryKey).alias(categoryAlias))
            .transactions(List.of(new PositionTransaction().exchange(exchange)));

        when(positionManagementApi.postPositions(any(PositionsPostRequest.class)))
            .thenReturn(Mono.empty());
        when(transactionCategoryManagementApi.postTransactionCategory(any(TransactionCategoryPostRequest.class)))
            .thenReturn(Mono.empty());
        when(transactionManagementApi.postPortfolioTransactions(anyString(),
            any(PortfolioTransactionsPostRequest.class))).thenReturn(Mono.empty());

        portfolioIntegrationService.createPosition(positionBundle).block();

        verify(positionManagementApi).postPositions(new PositionsPostRequest()
            .portfolioCode(portfolioId)
            .subPortfolioCode(subPortfolioId)
            .externalId(positionId)
            .instrumentId(instrumentId));
        verify(transactionCategoryManagementApi).postTransactionCategory(new TransactionCategoryPostRequest()
            .key(transactionCategoryKey)
            .alias(categoryAlias));
        verify(transactionManagementApi)
            .postPortfolioTransactions(portfolioId, new PortfolioTransactionsPostRequest()
                .transactions(List.of(new PortfolioTransactionsPostItem()
                    .positionId(positionId)
                    .transactions(List.of(new PortfolioPositionTransactionsPostItem()
                        .exchange(exchange))))));

    }

    @Test
    void createPortfolio() {

        String benchmarkName = "benchmarkName";
        String portfolioId = "portfolioId";
        String iban = "iban";
        String subPortfolioCode = "subPortfolio";
        PortfolioBundle portfolioBundle = new PortfolioBundle()
            .portfolio(new Portfolio().iban(iban).code(portfolioId))
            .addSubPortfoliosItem(new SubPortfolio().code(subPortfolioCode))
            .addAllocationsItem(new Allocation().classifierType("ASSET_CLASS"))
            .addHierarchiesItem(new PortfolioPositionsHierarchy()
                .itemType(PortfolioPositionsHierarchy.ItemTypeEnum.ASSET_CLASS))
            .addCumulativePerformancesItem(new PortfolioCumulativePerformances().valuePct(BigDecimal.TEN))
            .benchmark(new PortfolioBenchmark().name(benchmarkName))
            .addValuationsItem(new PortfolioValuation().valuePct(BigDecimal.ONE));

        when(portfolioManagementApi.postPortfolios(any(PortfoliosPostRequest.class))).thenReturn(Mono.empty());
        when(subPortfolioManagementApi.postSubPortfolios(anyString(), any(SubPortfoliosPostRequest.class)))
            .thenReturn(Mono.empty());
        when(portfolioManagementApi
            .putPortfolioAllocations(anyString(), any(PortfolioAllocationsPutRequest.class)))
            .thenReturn(Mono.empty());
        when(portfolioPositionsHierarchyManagementApi
            .putPortfolioPositionsHierarchy(anyString(), any(PortfolioPositionsHierarchyPutRequest.class)))
            .thenReturn(Mono.empty());
        when(portfolioCumulativePerformanceManagementApi
            .putPortfolioCumulativePerformance(anyString(), any(PortfolioCumulativePerformancesPutRequest.class)))
            .thenReturn(Mono.empty());
        when(portfolioBenchmarksManagementApi
            .postPortfolioBenchmark(any(PortfolioBenchmarkPostRequest.class)))
            .thenReturn(Mono.empty());
        when(portfolioValuationManagementApi
            .putPortfolioValuations(anyString(), any(PortfolioValuationsPutRequest.class)))
            .thenReturn(Mono.empty());

        portfolioIntegrationService.createPortfolio(portfolioBundle).blockLast();

        verify(portfolioManagementApi).postPortfolios(new PortfoliosPostRequest()
            .code(portfolioId)
            .iban(iban));
        verify(subPortfolioManagementApi).postSubPortfolios(portfolioId, new SubPortfoliosPostRequest()
            .code(subPortfolioCode));
        verify(portfolioManagementApi).putPortfolioAllocations(portfolioId, new PortfolioAllocationsPutRequest()
            .addAllocationsItem(new PortfolioAllocationsParentItem()
                .classifierType(AllocationClassifierType.ASSET_CLASS)));
        verify(portfolioPositionsHierarchyManagementApi).putPortfolioPositionsHierarchy(portfolioId,
            new PortfolioPositionsHierarchyPutRequest().addItemsItem(new PortfolioPositionsHierarchyItem()
                .itemType(ItemTypeEnum.ASSET_CLASS)));
        verify(portfolioCumulativePerformanceManagementApi).putPortfolioCumulativePerformance(portfolioId,
            new PortfolioCumulativePerformancesPutRequest()
                .addCumulativePerformanceItem(new PortfolioCumulativePerformancesItem().valuePct(BigDecimal.TEN)));
        verify(portfolioBenchmarksManagementApi).postPortfolioBenchmark(new PortfolioBenchmarkPostRequest()
            .id(portfolioId)
            .name(benchmarkName));
        verify(portfolioValuationManagementApi).putPortfolioValuations(portfolioId, new PortfolioValuationsPutRequest()
            .addValuationsItem(new PortfolioValuationsItem().valuePct(BigDecimal.ONE)));

    }

}