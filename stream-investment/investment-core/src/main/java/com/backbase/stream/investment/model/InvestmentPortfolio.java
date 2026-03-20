package com.backbase.stream.investment.model;

import com.backbase.investment.api.service.v1.model.PortfolioList;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Builder
@Getter
@Setter
public class InvestmentPortfolio {

    private PortfolioList portfolio;
    private BigDecimal initialCash;

    public double getInitialCashOrDefault(double defaultAmount) {
        return Optional.ofNullable(initialCash).map(BigDecimal::doubleValue)
            .orElse(defaultAmount);
    }

}
