package com.backbase.stream.investment;

import com.backbase.investment.api.service.v1.model.Market;
import com.backbase.investment.api.service.v1.model.MarketSpecialDay;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private List<Asset> assets;
    private List<AssetPrice> assetPrices;

    public Map<String, AssetPrice> getPriceByAsset() {
        return Objects.requireNonNullElse(assetPrices, List.<AssetPrice>of()).stream()
            .collect(Collectors.toMap(AssetKey::getKeyString, Function.identity()));
    }

}
