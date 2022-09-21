package com.backbase.stream.portfolio.mapper;

import com.backbase.portfolio.api.service.integration.v1.model.AggregatePortfoliosPostRequest;
import com.backbase.portfolio.api.service.integration.v1.model.AllocationType;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioAllocationsParentItem;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioBenchmarkPostRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioCumulativePerformancesItem;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioPositionTransactionsPostItem;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioPositionsHierarchyItem;
import com.backbase.portfolio.api.service.integration.v1.model.PortfolioValuationsItem;
import com.backbase.portfolio.api.service.integration.v1.model.PortfoliosPostRequest;
import com.backbase.portfolio.api.service.integration.v1.model.PositionsPostRequest;
import com.backbase.portfolio.api.service.integration.v1.model.SubPortfoliosPostRequest;
import com.backbase.portfolio.api.service.integration.v1.model.TransactionCategoryPostRequest;
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
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ValueMapping;

@Mapper
public interface PortfolioMapper {

    PortfoliosPostRequest mapPortfolio(Portfolio region);

    SubPortfoliosPostRequest mapSubPortfolio(SubPortfolio subPortfolio);

    List<PortfolioAllocationsParentItem> mapAllocations(List<Allocation> allocations);

    @ValueMapping(source = "BY_CURRENCY", target = "CURRENCY")
    @ValueMapping(source = "BY_ASSET_CLASS", target = "ASSET_CLASS")
    @ValueMapping(source = "BY_REGION", target = "REGION")
    @ValueMapping(source = "BY_COUNTRY", target = "COUNTRY")
    @ValueMapping(source = MappingConstants.ANY_UNMAPPED, target = MappingConstants.NULL)
    AllocationType map(String allocationTypeEnum);

    List<PortfolioPositionsHierarchyItem> mapHierarchies(List<PortfolioPositionsHierarchy> allocations);

    List<PortfolioCumulativePerformancesItem> mapCumulativePerformances(
        List<PortfolioCumulativePerformances> cumulativePerformances);

    @Mapping(target = "id", source = "portfolioId")
    @Mapping(target = "name", source = "benchmarkName")
    PortfolioBenchmarkPostRequest mapBenchmark(String portfolioId, String benchmarkName);

    List<PortfolioValuationsItem> mapValuations(List<PortfolioValuation> valuations);

    AggregatePortfoliosPostRequest mapAggregate(AggregatePortfolio aggregatePortfolios);

    @Mapping(target = "portfolioCode", source = "portfolioId")
    @Mapping(target = "subPortfolioCode", source = "subPortfolioId")
    @Mapping(target = ".", source = "position")
    PositionsPostRequest mapPosition(String portfolioId, String subPortfolioId, Position position);

    List<PortfolioPositionTransactionsPostItem> mapTransaction(List<PositionTransaction> trs);

    TransactionCategoryPostRequest mapTransactionCategory(TransactionCategory tc);

}
