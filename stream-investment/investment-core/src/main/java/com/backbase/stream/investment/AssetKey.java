package com.backbase.stream.investment;

import java.util.Map;

public interface AssetKey {

    String getIsin();

    String getMarket();

    String getCurrency();

    default Map<String, Object> getAssetMap() {
        return Map.of("isin", getIsin(), "market", getMarket(), "currency", getCurrency());
    }

    default String getKeyString() {
        return getIsin() + "_" + getMarket() + "_" + getCurrency();
    }

}
