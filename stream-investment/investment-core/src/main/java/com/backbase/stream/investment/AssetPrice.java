package com.backbase.stream.investment;

public record AssetPrice(String isin, String market, String currency, double price) implements AssetKey {

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
