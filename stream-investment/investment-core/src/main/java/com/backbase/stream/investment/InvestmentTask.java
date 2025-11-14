package com.backbase.stream.investment;

import com.backbase.investment.api.service.v1.model.Asset;
import com.backbase.investment.api.service.v1.model.Market;
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
        return "investment";
    }

    public InvestmentTask data(ClientUser clientUser) {

        return null;
    }

    public void data(List<ClientUser> clients) {
        data.setClientUsers(clients);
    }

    public void setMarkets(List<Market> markets) { data.setMarkets(markets); }

    public void setAssets(List<Asset> assets) { data.setAssets(assets); }
}
