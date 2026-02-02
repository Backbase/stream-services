package com.backbase.stream.investment.service.resttemplate;

import com.backbase.investment.api.service.sync.v1.model.AssetCategory;
import com.backbase.investment.api.service.sync.v1.model.AssetCategoryRequest;
import com.backbase.investment.api.service.sync.v1.model.OASAssetRequestDataRequest;
import com.backbase.investment.api.service.sync.v1.model.PatchedAssetCategoryRequest;
import com.backbase.stream.investment.Asset;
import com.backbase.stream.investment.model.AssetCategoryEntry;
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
public interface RestTemplateAssetMapper {

    @Mapping(target = "categories", ignore = true)
    OASAssetRequestDataRequest mapAsset(Asset asset,
        Map<String, UUID> categoryIdByCode);

    @AfterMapping
    default void postMap(
        @MappingTarget OASAssetRequestDataRequest requestDataRequest,
        Asset asset,
        Map<String, UUID> categoryIdByCode) {
        if (requestDataRequest == null) {
            return;
        }
        requestDataRequest.setCategories(
            Objects.requireNonNullElse(asset.getCategories(), new ArrayList<AssetCategory>())
                .stream().filter(Objects::nonNull).map(categoryIdByCode::get)
                .filter(Objects::nonNull).toList());
    }

    @Mapping(target = "categories", source = "categories", qualifiedByName = "mapSyncCategories")
    @Mapping(target = "logo", ignore = true)
    com.backbase.stream.investment.Asset mapFromSyncAsset(
        com.backbase.investment.api.service.sync.v1.model.Asset asset);

    // Post request cannot insert file directly, so set to null for the initial creation call
    @Mapping(target = "image", ignore = true)
    AssetCategoryRequest mapAssetCategory(AssetCategoryEntry entry);

    // Post request cannot insert file directly, so set to null for the initial creation call
    @Mapping(target = "image", ignore = true)
    PatchedAssetCategoryRequest mapPatchAssetCategory(AssetCategoryEntry entry);

    @Named("mapSyncCategories")
    default List<String> mapSyncCategories(
        List<com.backbase.investment.api.service.sync.v1.model.AssetCategory> categories) {
        return Objects.requireNonNullElse(categories,
                new ArrayList<AssetCategory>())
            .stream().map(com.backbase.investment.api.service.sync.v1.model.AssetCategory::getCode).toList();
    }

}
