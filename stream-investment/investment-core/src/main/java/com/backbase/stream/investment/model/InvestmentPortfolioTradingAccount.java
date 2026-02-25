package com.backbase.stream.investment.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvestmentPortfolioTradingAccount {
    private String portfolioExternalId;
    private String accountId;
    private String accountExternalId;
    private Boolean isDefault;
    private Boolean isInternal;
}
