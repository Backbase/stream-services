package com.backbase.stream.investment;

public record ModelAsset(String isin, String market, String currency) implements AssetKey {

    @Override
    public String getIsin() {
        return isin;
    }

    @Override
    public String getMarket() {
        return market;
    }

    @Override
    public String getCurrency() {
        return currency;
    }

}
