package com.backbase.stream.investment;

import com.backbase.investment.api.service.v1.model.InvestorModelPortfolio;
import com.backbase.investment.api.service.v1.model.PortfolioList;
import com.backbase.investment.api.service.v1.model.PortfolioProduct;
import com.backbase.investment.api.service.v1.model.ProductTypeEnum;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
    private List<ModelPortfolio> modelPortfolios;
    private List<PortfolioProduct> portfolioProducts;
    private InvestmentAssetData investmentAssetData;
    private List<PortfolioList> portfolios;

    public Map<String, List<UUID>> getClientsByLeExternalId() {
        Map<String, List<UUID>> clientsByLeExternalId = new HashMap<>();
        clientUsers.forEach(
            c -> clientsByLeExternalId.computeIfAbsent(c.getLegalEntityExternalId(), l -> new ArrayList<>())
                .add(c.getInvestmentClientId()));
        return clientsByLeExternalId;
    }

    public void setPortfoliosProducts(List<PortfolioProduct> products) {
        this.portfolioProducts = products;
    }

    public void addPortfolioProducts(PortfolioProduct portfolioProduct) {
        if (portfolioProducts == null) {
            portfolioProducts = new ArrayList<>();
        }
        if (portfolioProducts.stream().noneMatch(p -> p.getUuid().equals(portfolioProduct.getUuid()))) {
            portfolioProducts.add(portfolioProduct);
        }
    }

    public Optional<PortfolioProduct> findPortfolioProduct(ProductTypeEnum productType, Integer riskLevel) {
        return Optional.ofNullable(portfolioProducts)
            .flatMap(ps -> ps.stream()
                .filter(p -> p.getProductType().equals(productType)
                    && Optional.ofNullable(p.getModelPortfolio()).map(InvestorModelPortfolio::getRiskLevel)
                    .map(risk -> risk <= riskLevel).orElse(false))
                .findAny());
    }

}
