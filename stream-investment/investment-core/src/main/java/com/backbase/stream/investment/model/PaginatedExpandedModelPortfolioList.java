package com.backbase.stream.investment.model;

import com.backbase.investment.api.service.v1.model.InvestorModelPortfolio;
import com.backbase.stream.investment.ModelPortfolio;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@Data
@Builder
public class PaginatedExpandedModelPortfolioList {

    private Integer count;
    private URI next;
    private URI previous;
    private List<InvestorModelPortfolio> results = new ArrayList<>();

}
