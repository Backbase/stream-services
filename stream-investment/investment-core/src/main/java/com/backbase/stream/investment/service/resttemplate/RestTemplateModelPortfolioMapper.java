package com.backbase.stream.investment.service.resttemplate;

import com.backbase.investment.api.service.v1.model.OASAssetModelPortfolioRequestRequest;
import com.backbase.investment.api.service.v1.model.OASModelPortfolioRequestDataRequest;
import com.backbase.stream.investment.Allocation;
import com.backbase.stream.investment.ModelPortfolio;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * MapStruct mapper that converts the stream {@link ModelPortfolio} model into the OAS
 * {@link OASModelPortfolioRequestDataRequest} used by the Investment service REST API.
 */
@Mapper
public interface RestTemplateModelPortfolioMapper {

    /**
     * Maps a {@link ModelPortfolio} stream model to an {@link OASModelPortfolioRequestDataRequest}.
     *
     * <p>The {@code allocations} list is mapped to the {@code allocation} field using a custom
     * converter that resolves the asset identifier map from each {@link Allocation}.
     *
     * @param modelPortfolio the stream model to convert (must not be {@code null})
     * @return the populated OAS request object
     */
    @Mapping(target = "allocation", source = "allocations", qualifiedByName = "mapAllocations")
    OASModelPortfolioRequestDataRequest toRequest(ModelPortfolio modelPortfolio);

    /**
     * Converts a list of {@link Allocation} records into the OAS allocation request list.
     *
     * <p>Each allocation's asset is resolved to a {@code Map<String, Object>} key via
     * {@link com.backbase.stream.investment.AssetKey#getAssetMap()}.
     *
     * @param allocations source allocation list; {@code null} is treated as empty
     * @return non-null list of OAS allocation request objects
     */
    @Named("mapAllocations")
    default List<OASAssetModelPortfolioRequestRequest> mapAllocations(List<Allocation> allocations) {
        if (allocations == null) {
            return List.of();
        }
        return allocations.stream()
            .map(m -> new OASAssetModelPortfolioRequestRequest()
                .asset(m.asset().getAssetMap())
                .weight(m.weight()))
            .toList();
    }
}

