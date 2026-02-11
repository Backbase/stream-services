package com.backbase.stream.investment.service.resttemplate;

import com.backbase.investment.api.service.sync.v1.model.EntryCreateUpdateRequest;
import com.backbase.investment.api.service.sync.v1.model.OASDocumentRequestDataRequest;
import com.backbase.stream.investment.AssetKey;
import com.backbase.stream.investment.ModelAsset;
import com.backbase.stream.investment.model.ContentDocumentEntry;
import com.backbase.stream.investment.model.MarketNewsEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper
public interface ContentMapper {

    @Mapping(target = "thumbnail", ignore = true)
    @Mapping(target = "status", constant = "PUBLISHED")
    EntryCreateUpdateRequest map(MarketNewsEntry entry);

    @Mapping(target = "assets", source = "assets", qualifiedByName = "mapRawAsserts")
    OASDocumentRequestDataRequest map(ContentDocumentEntry request);

    @Named("mapRawAsserts")
    default List<Map<String, Object>> mapRawAsserts(List<ModelAsset> assets) {
        return Objects.requireNonNullElse(assets, new ArrayList<ModelAsset>())
            .stream().map(AssetKey::getAssetMap).toList();
    }

}
