package com.backbase.stream.investment;

import com.backbase.investment.api.service.v1.model.AssetCategory;
import com.backbase.investment.api.service.v1.model.AssetCategoryType;
import com.backbase.investment.api.service.v1.model.Market;
import com.backbase.investment.api.service.v1.model.MarketSpecialDay;
import com.backbase.stream.worker.model.StreamTask;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class InvestmentAssetsTask extends StreamTask {

    private final InvestmentAssetData data;

    public InvestmentAssetsTask(String unitOfWorkId, InvestmentAssetData data) {
        super(unitOfWorkId);
        this.data = data;
    }

    @Override
    public String getName() {
        return "investment";
    }

    public void setMarkets(List<Market> markets) {
        data.setMarkets(markets);
    }

    public void setMarketSpecialDays(List<MarketSpecialDay> marketSpecialDays) {
        data.setMarketSpecialDays(marketSpecialDays);
    }

    public void setAssetCategoryTypes(List<AssetCategoryType> assetCategoryTypes) {
        data.setAssetCategoryTypes(assetCategoryTypes);
    }
    public void setAssetCategories(List<AssetCategory> assetCategories) {
        data.setAssetCategories(assetCategories);
    }
    public void setAssets(List<Asset> assets) {
        data.setAssets(assets);
    }

}
