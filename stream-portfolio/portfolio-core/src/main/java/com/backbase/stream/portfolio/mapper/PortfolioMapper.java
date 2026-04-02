package com.backbase.stream.portfolio.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.backbase.portfolio.api.service.integration.v1.model.AggregatePortfoliosPostRequest;
import com.backbase.portfolio.api.service.integration.v1.model.AggregatePortfoliosPutRequest;
import com.backbase.portfolio.api.service.integration.v1.model.AllocationType;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioAllocationsParentItem;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioBenchmarkPostRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioBenchmarkPutRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioCumulativePerformancesItem;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioPositionTransactionsPostItem;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioPositionsHierarchyItem;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioTransactionsPostItem;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioValuationsItem;
import com.backbase.portfolio.api.service.integration.v1.model.PortfoliosPostRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PortfoliosPutRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PositionTransactionPutRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PositionsPostRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PositionsPutRequest;
import com.backbase.portfolio.api.service.integration.v1.model.SubPortfoliosPostRequest;
import com.backbase.portfolio.api.service.integration.v1.model.SubPortfoliosPutRequest;
import com.backbase.portfolio.api.service.integration.v1.model.TransactionCategoryPostRequest;
import com.backbase.portfolio.api.service.integration.v1.model.TransactionCategoryPutRequest;
import com.backbase.stream.portfolio.model.AggregatePortfolio;
import com.backbase.stream.portfolio.model.Allocation;
import com.backbase.stream.portfolio.model.Portfolio;
import com.backbase.stream.portfolio.model.PortfolioCumulativePerformances;
import com.backbase.stream.portfolio.model.PortfolioPositionsHierarchy;
import com.backbase.stream.portfolio.model.PortfolioValuation;
import com.backbase.stream.portfolio.model.Position;
import com.backbase.stream.portfolio.model.PositionTransaction;
import com.backbase.stream.portfolio.model.SubPortfolio;
import com.backbase.stream.portfolio.model.TransactionCategory;

@Mapper
public interface PortfolioMapper {

    PortfoliosPostRequest mapPortfolio(Portfolio portfolio);

    PortfoliosPutRequest mapPutPortfolio(Portfolio portfolio);

    SubPortfoliosPostRequest mapSubPortfolio(SubPortfolio subPortfolio);

    SubPortfoliosPutRequest mapPutSubPortfolio(SubPortfolio subPortfolio);

    List<PortfolioAllocationsParentItem> mapAllocations(List<Allocation> allocations);

    default AllocationType map(String allocationTypeEnum) {
        if (allocationTypeEnum == null) {
            return null;
        }
        return switch (allocationTypeEnum) {
            case "CURRENCY", "BY_CURRENCY" -> AllocationType.BY_CURRENCY;
            case "ASSET_CLASS", "BY_ASSET_CLASS" -> AllocationType.BY_ASSET_CLASS;
            case "REGION", "BY_REGION" -> AllocationType.BY_REGION;
            case "COUNTRY", "BY_COUNTRY" -> AllocationType.BY_COUNTRY;
            case "SUB_ASSET_CLASS", "BY_SUB_ASSET_CLASS" -> AllocationType.BY_SUB_ASSET_CLASS;
            case "SECTOR", "BY_SECTOR" -> AllocationType.BY_SECTOR;
            default -> null;
        };
    }

    List<PortfolioPositionsHierarchyItem> mapHierarchies(List<PortfolioPositionsHierarchy> hierarchies);

    List<PortfolioCumulativePerformancesItem>
            mapCumulativePerformances(List<PortfolioCumulativePerformances> cumulativePerformances);

    @Mapping(target = "name", source = "benchmarkName")
    PortfolioBenchmarkPutRequest mapBenchmark(String benchmarkName);

    @Mapping(target = "id", source = "portfolioId")
    @Mapping(target = "name", source = "benchmarkName")
    PortfolioBenchmarkPostRequest mapBenchmark(String portfolioId, String benchmarkName);

    List<PortfolioValuationsItem> mapValuations(List<PortfolioValuation> valuations);

    AggregatePortfoliosPostRequest mapAggregate(AggregatePortfolio aggregatePortfolios);

    AggregatePortfoliosPutRequest mapPutAggregate(AggregatePortfolio aggregatePortfolios);

    @Mapping(target = "portfolioCode", source = "portfolioId")
    @Mapping(target = "subPortfolioCode", source = "subPortfolioId")
    @Mapping(target = ".", source = "position")
    PositionsPostRequest mapPosition(String portfolioId, String subPortfolioId, Position position);

    PositionsPostRequest mapPostPosition(Position position);

    PositionsPutRequest mapPutPosition(Position position);

    @Mapping(target = "positionId", source = "positionId")
    @Mapping(target = "transactions", source = "transactions")
    PortfolioTransactionsPostItem mapPortfolioTransactionsPostItem(String positionId,
            List<PositionTransaction> transactions);
    
    PositionTransactionPutRequest mapPositionTransactionPutRequest(PositionTransaction transaction);

    PortfolioPositionTransactionsPostItem mapTransactionPostItem(PositionTransaction transaction);

    List<PortfolioPositionTransactionsPostItem> mapTransactionPostItems(List<PositionTransaction> transactions);

    TransactionCategoryPostRequest mapTransactionCategory(TransactionCategory tc);

    TransactionCategoryPutRequest mapPutTransactionCategory(TransactionCategory tc);

}
