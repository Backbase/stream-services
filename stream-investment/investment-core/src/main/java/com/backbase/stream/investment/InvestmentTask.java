package com.backbase.stream.investment;

import com.backbase.stream.worker.model.StreamTask;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import reactor.core.publisher.Mono;

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
}
