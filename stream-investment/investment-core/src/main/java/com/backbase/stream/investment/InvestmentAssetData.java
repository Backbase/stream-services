package com.backbase.stream.investment;

import com.backbase.investment.api.service.v1.model.AssetCategory;
import com.backbase.investment.api.service.v1.model.AssetCategoryRequest;
import com.backbase.investment.api.service.v1.model.AssetCategoryType;
import com.backbase.investment.api.service.v1.model.GroupResult;
import com.backbase.investment.api.service.v1.model.Market;
import com.backbase.investment.api.service.v1.model.MarketSpecialDay;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@Data
@Builder
public class InvestmentAssetData {

    private List<Market> markets;
    private List<MarketSpecialDay> marketSpecialDays;
    private List<AssetCategoryType> assetCategoryTypes;
    private List<AssetCategoryRequest> assetCategories;
    private List<Asset> assets;
    private List<AssetPrice> assetPrices;
    private List<AssetCategory> insertedAssetCategories;
    private List<GroupResult> priceAsyncTasks;

    public Map<String, AssetPrice> getPriceByAsset() {
        return Objects.requireNonNullElse(assetPrices, List.<AssetPrice>of()).stream()
            .collect(Collectors.toMap(AssetKey::getKeyString, Function.identity()));
    }

    public Map<UUID, Asset> getAssetByUuid() {
        return Objects.requireNonNullElse(assets, List.<Asset>of()).stream()
            .collect(Collectors.toMap(Asset::getUuid, Function.identity()));

    }

}
