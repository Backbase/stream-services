package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.v1.model.AssetCategory;
import com.backbase.investment.api.service.v1.model.OASAssetRequestDataRequest;
import com.backbase.stream.investment.Asset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper
public interface AssetMapper {

    @Mapping(target = "categories", ignore = true)
    OASAssetRequestDataRequest map(Asset asset, Map<String, UUID> categoryIdByCode);

    @Mapping(target = "categories", source = "categories", qualifiedByName = "mapCategories")
    @Mapping(target = "logo", ignore = true)
    Asset map(com.backbase.investment.api.service.v1.model.Asset asset);

    @AfterMapping
    default void postMap(@MappingTarget OASAssetRequestDataRequest requestDataRequest, Asset asset,
        Map<String, UUID> categoryIdByCode) {
        if (requestDataRequest == null) {
            return;
        }
        requestDataRequest.setCategories(Objects.requireNonNullElse(asset.getCategories(), new ArrayList<AssetCategory>())
            .stream().filter(Objects::nonNull).map(categoryIdByCode::get)
            .filter(Objects::nonNull).toList());
    }

    @Named("mapCategories")
    default List<String> mapCategories(List<AssetCategory> categories) {
        return Objects.requireNonNullElse(categories, new ArrayList<AssetCategory>())
            .stream().map(AssetCategory::getCode).toList();
    }

}
