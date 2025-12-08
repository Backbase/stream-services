package com.backbase.stream.investment;

import com.backbase.investment.api.service.v1.model.InvestorModelPortfolio;
import com.backbase.investment.api.service.v1.model.Market;
import com.backbase.investment.api.service.v1.model.MarketSpecialDay;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@Data
@Builder
public class InvestmentData {

    private String saName;
    private String saExternalId;
    private List<ClientUser> clientUsers;
    private List<InvestmentArrangement> investmentArrangements;
    private List<InvestorModelPortfolio> portfolioModels;
    private List<ModelPortfolio> modelPortfolios;
    private List<Market> markets;
    private List<MarketSpecialDay> marketSpecialDays;
    private List<Asset> assets;
    private List<AssetPrice> assetPrices;

    public Map<String, List<UUID>> getClientsByLeExternalId() {
        Map<String, List<UUID>> clientsByLeExternalId = new HashMap<>();
        clientUsers.forEach(
            c -> clientsByLeExternalId.computeIfAbsent(c.getLegalEntityExternalId(), l -> new ArrayList<>())
                .add(c.getInvestmentClientId()));
        return clientsByLeExternalId;
    }

    public Map<String, Double> getPriceByAsset() {
        return Objects.requireNonNullElse(assetPrices, List.<AssetPrice>of()).stream()
            .collect(Collectors.toMap(AssetKey::getKeyString, AssetPrice::price));
    }

}
