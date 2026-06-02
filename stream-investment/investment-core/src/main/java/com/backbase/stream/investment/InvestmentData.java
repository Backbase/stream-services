package com.backbase.stream.investment;

import com.backbase.investment.api.service.v1.model.GroupResult;
import com.backbase.investment.api.service.v1.model.PortfolioProduct;
import com.backbase.stream.investment.model.InvestmentPortfolio;
import com.backbase.stream.investment.model.InvestmentPortfolioTradingAccount;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode
@Data
@Builder
@Slf4j
public class InvestmentData implements InvestmentDataValue {

    private List<ClientUser> clientUsers;
    private List<InvestmentArrangement> investmentArrangements;
    private List<ModelPortfolio> modelPortfolios;
    private List<ProductPortfolio> portfolioProducts;
    private List<PortfolioProduct> ingestedPortfolioProducts;
    private InvestmentAssetData investmentAssetData;
    private List<InvestmentPortfolio> portfolios;
    private List<InvestmentPortfolioTradingAccount> investmentPortfolioTradingAccounts;
    private List<PortfolioRiskAssessment> portfolioRiskAssessments;

    public Map<String, List<UUID>> getClientsByLeExternalId() {
        Map<String, List<UUID>> clientsByLeExternalId = new HashMap<>();
        clientUsers.forEach(
            c -> clientsByLeExternalId.computeIfAbsent(c.getLegalEntityId(), l -> new ArrayList<>())
                .add(c.getInvestmentClientId()));
        return clientsByLeExternalId;
    }

    public void addPortfoliosProducts(List<PortfolioProduct> products) {
        products.forEach(this::addPortfolioProducts);
    }

    public void addPortfolioProducts(PortfolioProduct ingestedPortfolioProduct) {
        if (ingestedPortfolioProducts == null) {
            ingestedPortfolioProducts = new ArrayList<>();
        }
        if (ingestedPortfolioProducts.stream().noneMatch(p -> p.getUuid().equals(ingestedPortfolioProduct.getUuid()))) {
            ingestedPortfolioProducts.add(ingestedPortfolioProduct);
        }
    }

    public List<GroupResult> getPriceAsyncTasks() {
        return Optional.ofNullable(investmentAssetData).map(InvestmentAssetData::getPriceAsyncTasks).orElse(List.of());
    }

    public long getTotalProcessedValues() {
        log.debug(
            "Calculating total processed values: portfolios={}, portfolioProducts={}, modelPortfolios={}, clientUsers={}, investmentPortfolioTradingAccounts={}",
            getSize(portfolios), getSize(portfolioProducts), getSize(modelPortfolios), getSize(clientUsers),
            getSize(investmentPortfolioTradingAccounts));
        return getSize(portfolios) + getSize(portfolioProducts) + getSize(modelPortfolios) + getSize(clientUsers)
            + getSize(investmentPortfolioTradingAccounts);
    }

}
