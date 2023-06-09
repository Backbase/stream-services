package com.backbase.stream.portfolio;

import com.backbase.stream.portfolio.model.WealthBundle;
import com.backbase.stream.worker.model.StreamTask;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class PortfolioTask extends StreamTask {

    private WealthBundle wealthBundle;

    public PortfolioTask(WealthBundle wealthBundle) {
        super(UUID.randomUUID().toString());
        this.wealthBundle = wealthBundle;
    }

    public WealthBundle getData() {
        return wealthBundle;
    }

    @Override
    public String getName() {
        return getId();
    }
}
