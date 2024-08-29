package com.backbase.stream.portfolio.service;

import static com.backbase.stream.portfolio.util.PortfolioTestUtil.EUR_CURRENCY_CODE;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.portfolio.api.service.integration.v1.model.AggregatePortfoliosPostRequest;
import com.backbase.portfolio.api.service.integration.v1.model.AggregatePortfoliosPutRequest;
import com.backbase.portfolio.api.service.integration.v1.model.AllocationClassifierType;
import com.backbase.portfolio.api.service.integration.v1.model.Money;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioAllocationsParentItem;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioAllocationsPutRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioBenchmarkPostRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioCumulativePerformancesItem;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioCumulativePerformancesPutRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioPositionTransactionsPostItem;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioPositionsHierarchyItem;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioPositionsHierarchyPutRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioTransactionsPostItem;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioTransactionsPostRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioValuationsItem;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioValuationsPutRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PortfoliosGetItem;
import com.backbase.portfolio.api.service.integration.v1.model.PortfoliosGetResponse;
import com.backbase.portfolio.api.service.integration.v1.model.PortfoliosPostRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PortfoliosPutRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PositionTransactionPutRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PositionsGetItem;
import com.backbase.portfolio.api.service.integration.v1.model.PositionsPostRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PositionsPutRequest;
import com.backbase.portfolio.api.service.integration.v1.model.SubPortfolioGetResponse;
import com.backbase.portfolio.api.service.integration.v1.model.SubPortfoliosPostRequest;
import com.backbase.portfolio.api.service.integration.v1.model.SubPortfoliosPutRequest;
import com.backbase.portfolio.api.service.integration.v1.model.TransactionCategoryPostRequest;
import com.backbase.portfolio.api.service.integration.v1.model.TransactionCategoryPutRequest;
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
import com.backbase.stream.portfolio.model.AllocationBundle;
import com.backbase.stream.portfolio.model.HierarchyBundle;
import com.backbase.stream.portfolio.model.Portfolio;
import com.backbase.stream.portfolio.model.PortfolioBenchmark;
import com.backbase.stream.portfolio.model.PortfolioBundle;
import com.backbase.stream.portfolio.model.PortfolioCumulativePerformances;
import com.backbase.stream.portfolio.model.PortfolioPositionsHierarchy;
import com.backbase.stream.portfolio.model.PortfolioValuation;
import com.backbase.stream.portfolio.model.Position;
import com.backbase.stream.portfolio.model.PositionBundle;
import com.backbase.stream.portfolio.model.PositionTransaction;
import com.backbase.stream.portfolio.model.PositionTransactionBundle;
import com.backbase.stream.portfolio.model.SubPortfolio;
import com.backbase.stream.portfolio.model.SubPortfolioBundle;
import com.backbase.stream.portfolio.model.TransactionBundle;
import com.backbase.stream.portfolio.model.TransactionCategory;
import com.backbase.stream.portfolio.model.ValuationsBundle;
import com.backbase.stream.portfolio.model.WealthPortfolioAllocationsBundle;
import com.backbase.stream.portfolio.model.WealthPortfolioBundle;
import com.backbase.stream.portfolio.model.WealthPortfolioPositionHierarchyBundle;
import com.backbase.stream.portfolio.model.WealthPortfolioTransactionBundle;
import com.backbase.stream.portfolio.model.WealthPortfolioValuationsBundle;
import com.backbase.stream.portfolio.model.WealthPositionsBundle;
import com.backbase.stream.portfolio.model.WealthSubPortfolioBundle;
import com.backbase.stream.portfolio.model.WealthTransactionCategoriesBundle;
import com.backbase.stream.portfolio.util.PortfolioTestUtil;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class PortfolioIntegrationServiceTest {

    @SuppressWarnings("unused")
    @Spy
    private PortfolioMapper portfolioMapper = Mappers.getMapper(PortfolioMapper.class);

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
    void shouldCreateAgreateAggregatePortfolio() {
        String arrangementId = "arrangementId";
        AggregatePortfolio aggregatePortfolios = new AggregatePortfolio().id(arrangementId);

        when(aggregatePortfolioManagementApi.postAggregatePortfolios(any(AggregatePortfoliosPostRequest.class)))
                .thenReturn(Mono.empty());

        portfolioIntegrationService.createAggregatePortfolio(aggregatePortfolios).block();

        verify(aggregatePortfolioManagementApi)
                .postAggregatePortfolios(new AggregatePortfoliosPostRequest().id(arrangementId));

        verify(aggregatePortfolioManagementApi, times(0)).putAggregatePortfolio(anyString(),
                any(AggregatePortfoliosPutRequest.class));
    }

    @Test
    void shouldUpdateAgreateAggregatePortfolio() {
        String arrangementId = "arrangementId";
        AggregatePortfolio aggregatePortfolios = new AggregatePortfolio().id(arrangementId);

        when(aggregatePortfolioManagementApi.postAggregatePortfolios(any(AggregatePortfoliosPostRequest.class)))
                .thenReturn(Mono.error(WebClientResponseException.create(HttpStatus.CONFLICT.value(), "Conflict",
                        HttpHeaders.EMPTY, new byte[] {}, null)));
        when(aggregatePortfolioManagementApi.putAggregatePortfolio(anyString(),
                any(AggregatePortfoliosPutRequest.class))).thenReturn(Mono.empty());

        portfolioIntegrationService.createAggregatePortfolio(aggregatePortfolios).block();

        verify(aggregatePortfolioManagementApi)
                .postAggregatePortfolios(new AggregatePortfoliosPostRequest().id(arrangementId));
        verify(aggregatePortfolioManagementApi).putAggregatePortfolio(arrangementId,
                portfolioMapper.mapPutAggregate(aggregatePortfolios));

    }

    @Test
    void shouldCreatePosition_PositionBundle() {
        String positionId = "positionId";
        String instrumentId = "instrumentId";
        String portfolioId = "portfolioId";
        String subPortfolioId = "subPortfolioId";
        String transactionCategoryKey = "transactionCategoryKey";
        String transactionId = "transactionId";
        String categoryAlias = "categoryAlias";
        String exchange = "exchange";

        PositionBundle positionBundle = new PositionBundle().portfolioId(portfolioId)
                .subPortfolioId(subPortfolioId)
                .position(new Position().instrumentId(instrumentId).externalId(positionId))
                .addTransactionCategoriesItem(
                        new TransactionCategory().key(transactionCategoryKey).alias(categoryAlias))
                .transactions(List.of(new PositionTransaction().transactionId(transactionId).exchange(exchange)));

        when(positionManagementApi.getPositionById(anyString())).thenReturn(Mono.empty());
        when(positionManagementApi.postPositions(any(PositionsPostRequest.class))).thenReturn(Mono.empty());
        when(transactionCategoryManagementApi.postTransactionCategory(any(TransactionCategoryPostRequest.class)))
                .thenReturn(Mono.empty());
        when(transactionManagementApi.putPositionTransactionById(anyString(), anyString(),
                any(PositionTransactionPutRequest.class)))
                        .thenReturn(Mono.error(WebClientResponseException.create(HttpStatus.BAD_REQUEST.value(),
                                "Bad request", HttpHeaders.EMPTY, new byte[] {}, null)));
        when(transactionManagementApi.postPortfolioTransactions(anyString(),
                any(PortfolioTransactionsPostRequest.class))).thenReturn(Mono.empty());
        when(transactionCategoryManagementApi.getTransactionCategories()).thenReturn(Flux.empty());

        portfolioIntegrationService.upsertPosition(positionBundle).block();

        verify(positionManagementApi).postPositions(new PositionsPostRequest().portfolioCode(portfolioId)
                .subPortfolioCode(subPortfolioId)
                .externalId(positionId)
                .instrumentId(instrumentId));
        verify(transactionCategoryManagementApi).postTransactionCategory(
                new TransactionCategoryPostRequest().key(transactionCategoryKey).alias(categoryAlias));
        verify(transactionManagementApi).postPortfolioTransactions(portfolioId, new PortfolioTransactionsPostRequest()
                .transactions(List.of(new PortfolioTransactionsPostItem().positionId(positionId)
                        .transactions(List.of(new PortfolioPositionTransactionsPostItem().transactionId(transactionId)
                                .exchange(exchange))))));
    }

    @Test
    void shouldCreatePosition_Position() throws Exception {
        String positionId = "ID543894783";
        String instrumentId = "ID78344628";
        String portfolioId = "IZ23452FD234";
        String subPortfolioId = "IX4389HJ49307";

        WealthPositionsBundle wealthPositionsBundle = PortfolioTestUtil.getWealthPositionsBundle();
        List<Position> positions = wealthPositionsBundle.getPositions();
        Position position0 = positions.get(0);

        when(positionManagementApi.getPositionById(anyString())).thenReturn(Mono.empty());
        when(positionManagementApi.postPositions(any(PositionsPostRequest.class))).thenReturn(Mono.empty());

        portfolioIntegrationService.upsertPosition(position0).block();

        verify(positionManagementApi).getPositionById(positionId);
        verify(positionManagementApi).postPositions(new PositionsPostRequest().portfolioCode(portfolioId)
                .subPortfolioCode(subPortfolioId)
                .externalId(positionId)
                .instrumentId(instrumentId)
                .absolutePerformance(new Money().amount(BigDecimal.valueOf(44.12)).currencyCode(EUR_CURRENCY_CODE))
                .relativePerformance(BigDecimal.valueOf(1.2))
                .purchasePrice(new Money().amount(BigDecimal.valueOf(124.18)).currencyCode(EUR_CURRENCY_CODE))
                .unrealizedPLPct(BigDecimal.valueOf(4.14))
                .unrealizedPL(new Money().amount(BigDecimal.valueOf(34.12)).currencyCode(EUR_CURRENCY_CODE))
                .todayPLPct(BigDecimal.valueOf(1.14))
                .todayPL(new Money().amount(BigDecimal.valueOf(14.12)).currencyCode(EUR_CURRENCY_CODE))
                .accruedInterest(new Money().amount(BigDecimal.valueOf(12.45)).currencyCode(EUR_CURRENCY_CODE))
                .quantity(BigDecimal.valueOf(187))
                .valuation(new Money().amount(BigDecimal.valueOf(132.11)).currencyCode(EUR_CURRENCY_CODE))
                .costPrice(new Money().amount(BigDecimal.valueOf(145.11)).currencyCode(EUR_CURRENCY_CODE))
                .costExchangeRate(new Money().amount(BigDecimal.valueOf(1.23)).currencyCode(EUR_CURRENCY_CODE))
                .percentAssetClass(BigDecimal.valueOf(187))
                .percentPortfolio(BigDecimal.valueOf(187))
                .percentParent(BigDecimal.valueOf(187))
                .positionType("Bond")
                .additions(Map.of("someKey", "someValue")));

        verify(positionManagementApi, times(0)).putPosition(anyString(), any(PositionsPutRequest.class));
    }

    @Test
    void shouldCreatePosition_Position_GetThrowsNotFound() throws Exception {
        String positionId = "ID543894783";
        String instrumentId = "ID78344628";
        String portfolioId = "IZ23452FD234";
        String subPortfolioId = "IX4389HJ49307";
        String EUR_CURRENCY_CODE = "EUR";

        WealthPositionsBundle wealthPositionsBundle = PortfolioTestUtil.getWealthPositionsBundle();
        List<Position> positions = wealthPositionsBundle.getPositions();
        Position position0 = positions.get(0);

        when(positionManagementApi.getPositionById(anyString())).thenReturn(Mono.error(WebClientResponseException
                .create(HttpStatus.NOT_FOUND.value(), "Not Found", HttpHeaders.EMPTY, new byte[] {}, null)));
        when(positionManagementApi.postPositions(any(PositionsPostRequest.class))).thenReturn(Mono.empty());

        portfolioIntegrationService.upsertPosition(position0).block();

        verify(positionManagementApi).getPositionById(positionId);
        verify(positionManagementApi).postPositions(new PositionsPostRequest().portfolioCode(portfolioId)
                .subPortfolioCode(subPortfolioId)
                .externalId(positionId)
                .instrumentId(instrumentId)
                .absolutePerformance(new Money().amount(BigDecimal.valueOf(44.12)).currencyCode(EUR_CURRENCY_CODE))
                .relativePerformance(BigDecimal.valueOf(1.2))
                .purchasePrice(new Money().amount(BigDecimal.valueOf(124.18)).currencyCode(EUR_CURRENCY_CODE))
                .unrealizedPLPct(BigDecimal.valueOf(4.14))
                .unrealizedPL(new Money().amount(BigDecimal.valueOf(34.12)).currencyCode(EUR_CURRENCY_CODE))
                .todayPLPct(BigDecimal.valueOf(1.14))
                .todayPL(new Money().amount(BigDecimal.valueOf(14.12)).currencyCode(EUR_CURRENCY_CODE))
                .accruedInterest(new Money().amount(BigDecimal.valueOf(12.45)).currencyCode(EUR_CURRENCY_CODE))
                .quantity(BigDecimal.valueOf(187))
                .valuation(new Money().amount(BigDecimal.valueOf(132.11)).currencyCode(EUR_CURRENCY_CODE))
                .costPrice(new Money().amount(BigDecimal.valueOf(145.11)).currencyCode(EUR_CURRENCY_CODE))
                .costExchangeRate(new Money().amount(BigDecimal.valueOf(1.23)).currencyCode(EUR_CURRENCY_CODE))
                .percentAssetClass(BigDecimal.valueOf(187))
                .percentPortfolio(BigDecimal.valueOf(187))
                .percentParent(BigDecimal.valueOf(187))
                .positionType("Bond")
                .additions(Map.of("someKey", "someValue")));

        verify(positionManagementApi, times(0)).putPosition(anyString(), any(PositionsPutRequest.class));
    }

    @Test
    void shouldUpdatePosition_Position() throws Exception {
        String positionId = "ID543894783";
        String EUR_CURRENCY_CODE = "EUR";

        WealthPositionsBundle wealthPositionsBundle = PortfolioTestUtil.getWealthPositionsBundle();
        List<Position> positions = wealthPositionsBundle.getPositions();
        Position position0 = positions.get(0);

        Mono<PositionsGetItem> positionGetResponse = Mono.just(new PositionsGetItem().externalId(positionId));

        when(positionManagementApi.getPositionById(positionId)).thenReturn(positionGetResponse);
        when(positionManagementApi.putPosition(anyString(), any(PositionsPutRequest.class))).thenReturn(Mono.empty());

        portfolioIntegrationService.upsertPosition(position0).block();

        verify(positionManagementApi, times(1)).getPositionById(positionId);
        verify(positionManagementApi, times(1)).putPosition(positionId, new PositionsPutRequest()
                .absolutePerformance(new Money().amount(BigDecimal.valueOf(44.12)).currencyCode(EUR_CURRENCY_CODE))
                .relativePerformance(BigDecimal.valueOf(1.2))
                .purchasePrice(new Money().amount(BigDecimal.valueOf(124.18)).currencyCode(EUR_CURRENCY_CODE))
                .unrealizedPLPct(BigDecimal.valueOf(4.14))
                .unrealizedPL(new Money().amount(BigDecimal.valueOf(34.12)).currencyCode(EUR_CURRENCY_CODE))
                .todayPLPct(BigDecimal.valueOf(1.14))
                .todayPL(new Money().amount(BigDecimal.valueOf(14.12)).currencyCode(EUR_CURRENCY_CODE))
                .accruedInterest(new Money().amount(BigDecimal.valueOf(12.45)).currencyCode(EUR_CURRENCY_CODE))
                .quantity(BigDecimal.valueOf(187))
                .valuation(new Money().amount(BigDecimal.valueOf(132.11)).currencyCode(EUR_CURRENCY_CODE))
                .costPrice(new Money().amount(BigDecimal.valueOf(145.11)).currencyCode(EUR_CURRENCY_CODE))
                .costExchangeRate(new Money().amount(BigDecimal.valueOf(1.23)).currencyCode(EUR_CURRENCY_CODE))
                .percentAssetClass(BigDecimal.valueOf(187))
                .percentPortfolio(BigDecimal.valueOf(187))
                .percentParent(BigDecimal.valueOf(187))
                .positionType("Bond")
                .additions(Map.of("someKey", "someValue")));
    }

    @Test
    void shouldUpdatePositionTransactions() throws Exception {
        WealthPortfolioTransactionBundle wealthPortfolioTransactionBundle =
                PortfolioTestUtil.getWealthPortfolioTransactionBundle();
        List<TransactionBundle> transactionBundles = wealthPortfolioTransactionBundle.getBatchPortfolioTransactions();
        TransactionBundle transactionBundle0 = transactionBundles.get(0);
        String portfolioCode = transactionBundle0.getPortfolioCode();
        List<PositionTransactionBundle> positionTransactionBundles = transactionBundle0.getTransactions();
        PositionTransactionBundle positionTransactionBundle0 = positionTransactionBundles.get(0);
        String positionId = positionTransactionBundle0.getPositionId();
        List<PositionTransaction> transactions = positionTransactionBundle0.getTransactions();
        PositionTransaction positionTransaction0 = transactions.get(0);

        when(transactionManagementApi.putPositionTransactionById(anyString(), anyString(),
                any(PositionTransactionPutRequest.class))).thenReturn(Mono.empty());

        portfolioIntegrationService.upsertTransactions(transactions, portfolioCode, positionId).blockLast();

        verify(transactionManagementApi, times(1)).putPositionTransactionById(positionId,
                positionTransaction0.getTransactionId(),
                portfolioMapper.mapPositionTransactionPutRequest(positionTransaction0));
        verify(transactionManagementApi, times(0)).postPortfolioTransactions(portfolioCode,
                new PortfolioTransactionsPostRequest()
                        .addTransactionsItem(new PortfolioTransactionsPostItem().positionId(positionId)
                                .addTransactionsItem(portfolioMapper.mapTransactionPostItem(positionTransaction0))));
    }

    @Test
    void shouldCreatePositionTransactions() throws Exception {
        WealthPortfolioTransactionBundle wealthPortfolioTransactionBundle =
                PortfolioTestUtil.getWealthPortfolioTransactionBundle();
        List<TransactionBundle> transactionBundles = wealthPortfolioTransactionBundle.getBatchPortfolioTransactions();
        TransactionBundle transactionBundle0 = transactionBundles.get(0);
        String portfolioCode = transactionBundle0.getPortfolioCode();
        List<PositionTransactionBundle> positionTransactionBundles = transactionBundle0.getTransactions();
        PositionTransactionBundle positionTransactionBundle0 = positionTransactionBundles.get(0);
        String positionId = positionTransactionBundle0.getPositionId();
        List<PositionTransaction> transactions = positionTransactionBundle0.getTransactions();
        PositionTransaction positionTransaction0 = transactions.get(0);

        when(transactionManagementApi.putPositionTransactionById(anyString(), anyString(),
                any(PositionTransactionPutRequest.class)))
                        .thenReturn(Mono.error(WebClientResponseException.create(HttpStatus.BAD_REQUEST.value(),
                                "Bad request", HttpHeaders.EMPTY, new byte[] {}, null)));
        when(transactionManagementApi.postPortfolioTransactions(anyString(),
                any(PortfolioTransactionsPostRequest.class))).thenReturn(Mono.empty());

        portfolioIntegrationService.upsertTransactions(transactions, portfolioCode, positionId).blockLast();

        verify(transactionManagementApi, times(1)).putPositionTransactionById(positionId,
                positionTransaction0.getTransactionId(),
                portfolioMapper.mapPositionTransactionPutRequest(positionTransaction0));
        verify(transactionManagementApi, times(1)).postPortfolioTransactions(portfolioCode,
                new PortfolioTransactionsPostRequest()
                        .addTransactionsItem(new PortfolioTransactionsPostItem().positionId(positionId)
                                .addTransactionsItem(portfolioMapper.mapTransactionPostItem(positionTransaction0))));
    }

    @Test
    void shouldCreatePortfolio_PortfolioBundle() {

        String benchmarkName = "benchmarkName";
        String portfolioId = "portfolioId";
        String iban = "iban";
        String subPortfolioCode = "subPortfolio";
        PortfolioBundle portfolioBundle = new PortfolioBundle().portfolio(new Portfolio().iban(iban).code(portfolioId))
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
        when(portfolioManagementApi.putPortfolioAllocations(anyString(), any(PortfolioAllocationsPutRequest.class)))
                .thenReturn(Mono.empty());
        when(portfolioPositionsHierarchyManagementApi.putPortfolioPositionsHierarchy(anyString(),
                any(PortfolioPositionsHierarchyPutRequest.class))).thenReturn(Mono.empty());
        when(portfolioCumulativePerformanceManagementApi.putPortfolioCumulativePerformance(anyString(),
                any(PortfolioCumulativePerformancesPutRequest.class))).thenReturn(Mono.empty());
        when(portfolioBenchmarksManagementApi.postPortfolioBenchmark(any(PortfolioBenchmarkPostRequest.class)))
                .thenReturn(Mono.empty());
        when(portfolioValuationManagementApi.putPortfolioValuations(anyString(),
                any(PortfolioValuationsPutRequest.class))).thenReturn(Mono.empty());
        when(subPortfolioManagementApi.getSubPortfolio(anyString(), anyString())).thenReturn(Mono.empty());
        when(portfolioManagementApi.getPortfolio(anyString())).thenReturn(Mono.empty());
        when(portfolioBenchmarksManagementApi.getPortfolioBenchmarks(0, Integer.MAX_VALUE)).thenReturn(Mono.empty());
        when(portfolioValuationManagementApi.deletePortfolioValuations(anyString(), anyString()))
                .thenReturn(Mono.empty());

        portfolioIntegrationService.upsertPortfolio(portfolioBundle).blockLast();

        verify(portfolioManagementApi).postPortfolios(new PortfoliosPostRequest().code(portfolioId).iban(iban));
        verify(subPortfolioManagementApi).postSubPortfolios(portfolioId,
                new SubPortfoliosPostRequest().code(subPortfolioCode));
        verify(portfolioManagementApi).putPortfolioAllocations(portfolioId,
                new PortfolioAllocationsPutRequest().addAllocationsItem(
                        new PortfolioAllocationsParentItem().classifierType(AllocationClassifierType.ASSET_CLASS)));
        verify(portfolioPositionsHierarchyManagementApi).putPortfolioPositionsHierarchy(portfolioId,
                new PortfolioPositionsHierarchyPutRequest().addItemsItem(new PortfolioPositionsHierarchyItem()
                        .itemType(PortfolioPositionsHierarchyItem.ItemTypeEnum.ASSET_CLASS)));
        verify(portfolioCumulativePerformanceManagementApi).putPortfolioCumulativePerformance(portfolioId,
                new PortfolioCumulativePerformancesPutRequest().addCumulativePerformanceItem(
                        new PortfolioCumulativePerformancesItem().valuePct(BigDecimal.TEN)));
        verify(portfolioBenchmarksManagementApi)
                .postPortfolioBenchmark(new PortfolioBenchmarkPostRequest().id(portfolioId).name(benchmarkName));
        verify(portfolioValuationManagementApi).putPortfolioValuations(portfolioId, new PortfolioValuationsPutRequest()
                .addValuationsItem(new PortfolioValuationsItem().valuePct(BigDecimal.ONE)));

    }

    @Test
    void shouldCreatePortfolio_Portfolio() throws Exception {
        WealthPortfolioBundle wealthPortfolioBundle = PortfolioTestUtil.getWealthPortfolioBundle();
        List<Portfolio> portfolios = wealthPortfolioBundle.getPortfolios();
        Portfolio portfolio0 = portfolios.get(0);

        String portfolioCode = "ARRANGEMENT_SARA";

        when(portfolioManagementApi.getPortfolio(anyString())).thenReturn(Mono.empty());
        when(portfolioManagementApi.postPortfolios(any(PortfoliosPostRequest.class))).thenReturn(Mono.empty());

        portfolioIntegrationService.upsertPortfolio(portfolio0).block();

        verify(portfolioManagementApi).getPortfolio(portfolioCode);
        verify(portfolioManagementApi).postPortfolios(portfolioMapper.mapPortfolio(portfolio0));
        verify(portfolioManagementApi, times(0)).putPortfolio(anyString(), any(PortfoliosPutRequest.class));
    }

    @Test
    void shouldCreatePortfolio_Portfolio_GetThrowsNotFound() throws Exception {
        WealthPortfolioBundle wealthPortfolioBundle = PortfolioTestUtil.getWealthPortfolioBundle();
        List<Portfolio> portfolios = wealthPortfolioBundle.getPortfolios();
        Portfolio portfolio0 = portfolios.get(0);

        String portfolioCode = "ARRANGEMENT_SARA";

        when(portfolioManagementApi.getPortfolio(anyString())).thenReturn(Mono.error(WebClientResponseException
                .create(HttpStatus.NOT_FOUND.value(), "Not Found", HttpHeaders.EMPTY, new byte[] {}, null)));
        when(portfolioManagementApi.postPortfolios(any(PortfoliosPostRequest.class))).thenReturn(Mono.empty());

        portfolioIntegrationService.upsertPortfolio(portfolio0).block();

        verify(portfolioManagementApi).getPortfolio(portfolioCode);
        verify(portfolioManagementApi).postPortfolios(portfolioMapper.mapPortfolio(portfolio0));
        verify(portfolioManagementApi, times(0)).putPortfolio(anyString(), any(PortfoliosPutRequest.class));
    }

    @Test
    void shouldUpdatePortfolio_Portfolio() throws Exception {
        WealthPortfolioBundle wealthPortfolioBundle = PortfolioTestUtil.getWealthPortfolioBundle();
        List<Portfolio> portfolios = wealthPortfolioBundle.getPortfolios();
        Portfolio portfolio0 = portfolios.get(0);

        String portfolioCode = "ARRANGEMENT_SARA";

        when(portfolioManagementApi.getPortfolio(anyString()))
            .thenReturn(Mono.just(
                new PortfoliosGetResponse().addPortfoliosItem(new PortfoliosGetItem().code(portfolioCode))
                    .getPortfolios().get(0)));
        when(portfolioManagementApi.putPortfolio(anyString(), any(PortfoliosPutRequest.class)))
                .thenReturn(Mono.empty());

        portfolioIntegrationService.upsertPortfolio(portfolio0).block();

        verify(portfolioManagementApi).getPortfolio(portfolioCode);

        verify(portfolioManagementApi).putPortfolio(portfolioCode, portfolioMapper.mapPutPortfolio(portfolio0));
        verify(portfolioManagementApi, times(0)).postPortfolios(any(PortfoliosPostRequest.class));
    }

    @Test
    void shouldCreateAllocation() throws Exception {
        WealthPortfolioAllocationsBundle wealthPortfolioAllocationsBundle =
                PortfolioTestUtil.getWealthPortfolioAllocationsBundle();
        List<AllocationBundle> batchPortfolioAllocations =
                wealthPortfolioAllocationsBundle.getBatchPortfolioAllocations();
        AllocationBundle allocationBundle0 = batchPortfolioAllocations.get(0);

        when(portfolioManagementApi.putPortfolioAllocations(anyString(), any(PortfolioAllocationsPutRequest.class)))
                .thenReturn(Mono.empty());

        String portfolioCode = allocationBundle0.getPortfolioCode();
        List<Allocation> allocations = allocationBundle0.getAllocations();

        portfolioIntegrationService.upsertAllocations(allocations, portfolioCode).block();

        verify(portfolioManagementApi).putPortfolioAllocations(portfolioCode,
                new PortfolioAllocationsPutRequest().allocations(portfolioMapper.mapAllocations(allocations)));
    }

    @Test
    void shouldCreateSubPortfolio() throws Exception {
        WealthSubPortfolioBundle wealthSubPortfolioBundle = PortfolioTestUtil.getWealthSubPortfolioBundle();
        List<SubPortfolioBundle> batchSubPortfolios = wealthSubPortfolioBundle.getBatchSubPortfolios();
        SubPortfolioBundle subPortfolioBundle0 = batchSubPortfolios.get(0);

        when(subPortfolioManagementApi.getSubPortfolio(anyString(), anyString())).thenReturn(Mono.empty());
        when(subPortfolioManagementApi.postSubPortfolios(anyString(), any(SubPortfoliosPostRequest.class)))
                .thenReturn(Mono.empty());

        String portfolioCode0 = subPortfolioBundle0.getPortfolioCode();
        List<SubPortfolio> subPortfolios = subPortfolioBundle0.getSubPortfolios();
        SubPortfolio subPortfolio0 = subPortfolios.get(0);
        SubPortfolio subPortfolio1 = subPortfolios.get(1);

        portfolioIntegrationService.upsertSubPortfolios(subPortfolios, portfolioCode0).block();

        verify(subPortfolioManagementApi).getSubPortfolio(portfolioCode0, subPortfolio0.getCode());
        verify(subPortfolioManagementApi).getSubPortfolio(portfolioCode0, subPortfolio1.getCode());

        verify(subPortfolioManagementApi).postSubPortfolios(portfolioCode0,
                portfolioMapper.mapSubPortfolio(subPortfolio0));
        verify(subPortfolioManagementApi).postSubPortfolios(portfolioCode0,
                portfolioMapper.mapSubPortfolio(subPortfolio1));

        verify(subPortfolioManagementApi, times(0)).putSubPortfolio(anyString(), anyString(),
                any(SubPortfoliosPutRequest.class));
    }

    @Test
    void shouldUpdateSubPortfolio() throws Exception {
        WealthSubPortfolioBundle wealthSubPortfolioBundle = PortfolioTestUtil.getWealthSubPortfolioBundle();
        List<SubPortfolioBundle> batchSubPortfolios = wealthSubPortfolioBundle.getBatchSubPortfolios();
        SubPortfolioBundle subPortfolioBundle0 = batchSubPortfolios.get(0);
        String portfolioCode0 = subPortfolioBundle0.getPortfolioCode();
        List<SubPortfolio> subPortfolios = subPortfolioBundle0.getSubPortfolios();
        SubPortfolio subPortfolio0 = subPortfolios.get(0);
        String subPortfolioCode0 = subPortfolio0.getCode();

        when(subPortfolioManagementApi.getSubPortfolio(portfolioCode0, subPortfolioCode0))
                .thenReturn(Mono.just(new SubPortfolioGetResponse().code(subPortfolioCode0)));
        when(subPortfolioManagementApi.putSubPortfolio(anyString(), anyString(), any(SubPortfoliosPutRequest.class)))
                .thenReturn(Mono.empty());

        portfolioIntegrationService.upsertSubPortfolios(List.of(subPortfolio0), portfolioCode0).block();

        verify(subPortfolioManagementApi).getSubPortfolio(portfolioCode0, subPortfolio0.getCode());

        verify(subPortfolioManagementApi).putSubPortfolio(portfolioCode0, subPortfolioCode0,
                portfolioMapper.mapPutSubPortfolio(subPortfolio0));

        verify(subPortfolioManagementApi, times(0)).postSubPortfolios(anyString(), any(SubPortfoliosPostRequest.class));
    }

    @Test
    void shouldCreateTransactionCategories() throws Exception {
        WealthTransactionCategoriesBundle wealthTransactionCategoriesBundle =
                PortfolioTestUtil.getWealthTransactionCategoriesBundle();
        List<TransactionCategory> transactionCategories = wealthTransactionCategoriesBundle.getTransactionCategories();

        when(transactionCategoryManagementApi.getTransactionCategories()).thenReturn(Flux.empty());
        when(transactionCategoryManagementApi.postTransactionCategory(any(TransactionCategoryPostRequest.class)))
                .thenReturn(Mono.empty());

        portfolioIntegrationService.upsertTransactionCategories(transactionCategories).block();

        verify(transactionCategoryManagementApi).getTransactionCategories();

        TransactionCategory transactionCategory0 = transactionCategories.get(0);
        TransactionCategory transactionCategory1 = transactionCategories.get(1);

        verify(transactionCategoryManagementApi)
                .postTransactionCategory(portfolioMapper.mapTransactionCategory(transactionCategory0));
        verify(transactionCategoryManagementApi)
                .postTransactionCategory(portfolioMapper.mapTransactionCategory(transactionCategory1));

        verify(transactionCategoryManagementApi, times(0)).putTransactionCategory(anyString(),
                any(TransactionCategoryPutRequest.class));
    }

    @Test
    void shouldUpdateTransactionCategories() throws Exception {
        WealthTransactionCategoriesBundle wealthTransactionCategoriesBundle =
                PortfolioTestUtil.getWealthTransactionCategoriesBundle();
        List<TransactionCategory> transactionCategories = wealthTransactionCategoriesBundle.getTransactionCategories();
        TransactionCategory transactionCategory0 = transactionCategories.get(0);
        TransactionCategory transactionCategory1 = transactionCategories.get(1);
        String key0 = transactionCategory0.getKey();
        String key1 = transactionCategory1.getKey();

        when(transactionCategoryManagementApi.getTransactionCategories()).thenReturn(Flux.fromIterable(
                List.of(new com.backbase.portfolio.api.service.integration.v1.model.TransactionCategory().key(key0),
                        new com.backbase.portfolio.api.service.integration.v1.model.TransactionCategory().key(key1))));
        when(transactionCategoryManagementApi.putTransactionCategory(anyString(),
                any(TransactionCategoryPutRequest.class))).thenReturn(Mono.empty());

        portfolioIntegrationService.upsertTransactionCategories(transactionCategories).block();

        verify(transactionCategoryManagementApi).getTransactionCategories();

        verify(transactionCategoryManagementApi).putTransactionCategory(key0,
                portfolioMapper.mapPutTransactionCategory(transactionCategory0));
        verify(transactionCategoryManagementApi).putTransactionCategory(key1,
                portfolioMapper.mapPutTransactionCategory(transactionCategory1));

        verify(transactionCategoryManagementApi, times(0))
                .postTransactionCategory(any(TransactionCategoryPostRequest.class));
    }

    @Test
    void shouldUpdateHierarchies() throws Exception {
        WealthPortfolioPositionHierarchyBundle wealthPortfolioPositionHierarchyBundle =
                PortfolioTestUtil.getWealthPortfolioPositionHierarchyBundle();
        List<HierarchyBundle> batchPortfolioPositionsHierarchies =
                wealthPortfolioPositionHierarchyBundle.getBatchPortfolioPositionsHierarchies();
        HierarchyBundle hierarchyBundle0 = batchPortfolioPositionsHierarchies.get(0);
        List<PortfolioPositionsHierarchy> hierarchies = hierarchyBundle0.getHierarchies();
        String portfolioCode0 = hierarchyBundle0.getPortfolioCode();

        when(portfolioPositionsHierarchyManagementApi.putPortfolioPositionsHierarchy(anyString(),
                any(PortfolioPositionsHierarchyPutRequest.class))).thenReturn(Mono.empty());

        portfolioIntegrationService.upsertHierarchies(hierarchies, portfolioCode0).block();

        verify(portfolioPositionsHierarchyManagementApi).putPortfolioPositionsHierarchy(portfolioCode0,
                new PortfolioPositionsHierarchyPutRequest().items(portfolioMapper.mapHierarchies(hierarchies)));
    }

    @Test
    void shouldUpdateValuations() throws Exception {
        WealthPortfolioValuationsBundle wealthPortfolioValuationsBundle =
                PortfolioTestUtil.getWealthPortfolioValuationsBundle();
        List<ValuationsBundle> batchPortfolioValuations = wealthPortfolioValuationsBundle.getBatchPortfolioValuations();
        ValuationsBundle valuationsBundle0 = batchPortfolioValuations.get(0);

        List<PortfolioValuation> valuations0 = valuationsBundle0.getValuations();
        String portfolioCode0 = valuationsBundle0.getPortfolioCode();

        when(portfolioValuationManagementApi.deletePortfolioValuations(anyString(), anyString()))
                .thenReturn(Mono.empty());
        when(portfolioValuationManagementApi.putPortfolioValuations(anyString(),
                any(PortfolioValuationsPutRequest.class))).thenReturn(Mono.empty());

        portfolioIntegrationService.upsertPortfolioValuations(valuations0, portfolioCode0).block();

        verify(portfolioValuationManagementApi).putPortfolioValuations(portfolioCode0,
                new PortfolioValuationsPutRequest().valuations(portfolioMapper.mapValuations(valuations0)));
    }
}
