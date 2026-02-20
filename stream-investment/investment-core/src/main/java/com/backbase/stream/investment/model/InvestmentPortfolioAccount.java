package com.backbase.stream.investment.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvestmentPortfolioAccount {
    private String portfolioExternalId;
    private List<Account> accounts;
}
