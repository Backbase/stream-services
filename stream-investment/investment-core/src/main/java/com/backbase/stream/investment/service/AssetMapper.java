package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.v1.model.AssetCategory;
import com.backbase.stream.investment.Asset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper
public interface AssetMapper {

    @Mapping(target = "categories", source = "categories", qualifiedByName = "mapCategories")
    @Mapping(target = "logo", ignore = true)
    Asset map(com.backbase.investment.api.service.v1.model.Asset asset);

    @Named("mapCategories")
    default List<String> mapCategories(List<AssetCategory> categories) {
        return Objects.requireNonNullElse(categories, new ArrayList<AssetCategory>())
            .stream().map(AssetCategory::getCode).toList();
    }

}
