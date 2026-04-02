package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.v1.model.AssetCategory;
import com.backbase.investment.api.service.v1.model.AssetCategoryTypeRequest;
import com.backbase.investment.api.service.v1.model.Market;
import com.backbase.investment.api.service.v1.model.MarketRequest;
import com.backbase.investment.api.service.v1.model.MarketSpecialDay;
import com.backbase.investment.api.service.v1.model.MarketSpecialDayRequest;
import com.backbase.stream.investment.Asset;
import com.backbase.stream.investment.model.AssetCategoryEntry;
import java.util.List;
import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper
public interface AssetMapper {

    /**
     * Maps a v1 API {@link com.backbase.investment.api.service.v1.model.Asset} response to the
     * stream {@link Asset} model. {@code logo} is explicitly ignored because the API model holds a
     * {@link java.net.URI} (a full signed URL) while the stream model holds a {@code String}
     * filename — the type mismatch prevents automatic mapping. Logo change detection is handled
     * separately in {@code isLogoUnchanged()} via a URI {@code contains} check.
     */
    @Mapping(target = "categories", source = "categories", qualifiedByName = "mapCategories")
    @Mapping(target = "logo", ignore = true)
    Asset map(com.backbase.investment.api.service.v1.model.Asset asset);

    /**
     * Maps an existing {@link Market} response to a {@link MarketRequest} so the two can be compared
     * field-by-field using the generated {@code equals()} method to detect whether a re-run carries
     * identical data and the update can be skipped.
     */
    MarketRequest toMarketRequest(Market market);

    /**
     * Maps an existing {@link MarketSpecialDay} response to a {@link MarketSpecialDayRequest} for
     * equality comparison before deciding whether to call the update API.
     *
     * <p>All data fields ({@code date}, {@code description}, {@code sessionStart},
     * {@code sessionEnd}, {@code market}) share the same name and type in both models so MapStruct
     * maps them automatically. The {@code uuid} field on {@link MarketSpecialDay} has no target in
     * {@link MarketSpecialDayRequest} and is silently ignored by MapStruct.
     */
    MarketSpecialDayRequest toMarketSpecialDayRequest(MarketSpecialDay marketSpecialDay);

    /**
     * Maps an existing {@link com.backbase.investment.api.service.v1.model.AssetCategoryType} response
     * to an {@link AssetCategoryTypeRequest} for equality comparison before deciding whether to call
     * the update API.
     *
     * <p>Both {@code code} and {@code name} share the same name and type ({@code String}) so MapStruct
     * maps them automatically. The {@code uuid} field on
     * {@link com.backbase.investment.api.service.v1.model.AssetCategoryType} has no target in
     * {@link AssetCategoryTypeRequest} and is silently ignored by MapStruct.
     */
    AssetCategoryTypeRequest toAssetCategoryTypeRequest(
        com.backbase.investment.api.service.v1.model.AssetCategoryType assetCategoryType);

    /**
     * Maps an existing {@link AssetCategory} (v1 response model) to an {@link AssetCategoryEntry}
     * for equality comparison before deciding whether to call the patch API.
     *
     * <p>The content fields ({@code name}, {@code code}, {@code order}, {@code type},
     * {@code excerpt}, {@code description}) share the same name and type in both models and are
     * mapped automatically. Two fields require explicit {@code ignore}:
     * <ul>
     *   <li>{@code image} — type mismatch: {@code URI} on the response vs {@code String} on the
     *       entry. Image change detection is handled separately in
     *       {@code isImageUnchanged()} via a filename {@code contains} check on the URI.</li>
     *   <li>{@code imageResource} — no equivalent field on the response model.</li>
     * </ul>
     * The {@code uuid} is mapped normally (both sides are {@link java.util.UUID}) so that the
     * returned entry can be used to set the uuid via {@code doOnSuccess} if needed.
     */
    @Mapping(target = "imageResource", ignore = true)
    @Mapping(target = "image", ignore = true)
    AssetCategoryEntry toAssetCategoryEntry(AssetCategory assetCategory);

    @Named("mapCategories")
    default List<String> mapCategories(List<AssetCategory> categories) {
        return Objects.requireNonNullElse(categories, List.<AssetCategory>of())
            .stream().map(AssetCategory::getCode).toList();
    }

}
