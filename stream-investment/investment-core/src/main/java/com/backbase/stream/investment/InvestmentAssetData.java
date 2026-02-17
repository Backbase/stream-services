package com.backbase.stream.investment;

import com.backbase.investment.api.service.v1.model.AssetCategoryType;
import com.backbase.investment.api.service.v1.model.Currency;
import com.backbase.investment.api.service.v1.model.GroupResult;
import com.backbase.investment.api.service.v1.model.Market;
import com.backbase.investment.api.service.v1.model.MarketSpecialDay;
import com.backbase.stream.investment.model.AssetCategoryEntry;
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

    private List<Currency> currencies;
    private List<Market> markets;
    private List<MarketSpecialDay> marketSpecialDays;
    private List<AssetCategoryType> assetCategoryTypes;
    private List<AssetCategoryEntry> assetCategories;
    private List<Asset> assets;
    private List<AssetPrice> assetPrices;
    private List<GroupResult> priceAsyncTasks;
    private List<GroupResult> intradayPriceAsyncTasks;

    public Map<String, AssetPrice> getPriceByAsset() {
        return Objects.requireNonNullElse(assetPrices, List.<AssetPrice>of()).stream()
            .collect(Collectors.toMap(AssetKey::getKeyString, Function.identity()));
    }

    public Map<UUID, Asset> getAssetByUuid() {
        return Objects.requireNonNullElse(assets, List.<Asset>of()).stream()
            .collect(Collectors.toMap(Asset::getUuid, Function.identity()));

    }

}
