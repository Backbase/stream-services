package com.backbase.stream.investment;

import com.backbase.investment.api.service.v1.model.PortfolioList;
import com.backbase.stream.worker.model.StreamTask;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class InvestmentTask extends StreamTask {

    private final InvestmentData data;

    public InvestmentTask(String unitOfWorkId, InvestmentData data) {
        super(unitOfWorkId);
        this.data = data;
    }

    @Override
    public String getName() {
        return "investment-portfolios-clients";
    }

    public void data(List<ClientUser> clients) {
        data.setClientUsers(clients);
    }

    public void setPortfolios(List<PortfolioList> portfolios) {
        data.setPortfolios(portfolios);
    }

}
