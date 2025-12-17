package com.backbase.stream.investment;

import com.backbase.investment.api.service.v1.model.AssetCategory;
import com.backbase.investment.api.service.v1.model.AssetTypeEnum;
import com.backbase.investment.api.service.v1.model.StatusA10Enum;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lightweight projection of {@link com.backbase.investment.api.service.v1.model.Asset} that keeps the DTO immutable
 * while providing helpers to translate to/from the generated model.
 */
public record Asset(
    UUID uuid,
    String name,
    String isin,
    String ticker,
    StatusA10Enum status,
    String market,
    String currency,
    @JsonProperty("extra_data")
    Map<String, Object> extraData,
    @JsonProperty("asset_type")
    AssetTypeEnum assetType,
    List<String> categories,
    URI logo,
    String externalId,
    String description
) {

    /**
     * Creates a record from the generated API model.
     */
    public static Asset fromModel(com.backbase.investment.api.service.v1.model.Asset asset) {
        if (asset == null) {
            return null;
        }
        List<AssetCategory> categories = asset.getCategories();
        Map<String, Object> extraData = asset.getExtraData();
        return new Asset(
            asset.getUuid(),
            asset.getName(),
            asset.getIsin(),
            asset.getTicker(),
            asset.getStatus(),
            asset.getMarket(),
            asset.getCurrency(),
            extraData == null ? Map.of() : Map.copyOf(extraData),
            asset.getAssetType(),
            categories == null ? List.of() : categories.stream().map(AssetCategory::getCode).toList(),
            asset.getLogo(),
            asset.getExternalId(),
            asset.getDescription()
        );
    }

    /**
     * Ensures the record keeps defensive copies of mutable collections.
     */
    public Asset {
        Map<String, Object> safeExtraData = extraData == null ? Map.of() : Map.copyOf(extraData);
        List<String> safeCategories = categories == null ? List.of() : List.copyOf(categories);
        extraData = safeExtraData;
        categories = safeCategories;
    }
}
