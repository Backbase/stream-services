package com.backbase.stream.investment;

import java.util.Map;

public record ModelAsset(String isin, String market, String currency) {

    public Map<String, Object> getMap() {
        return Map.of("isin", isin, "market", market, "currency", currency);
    }

}
