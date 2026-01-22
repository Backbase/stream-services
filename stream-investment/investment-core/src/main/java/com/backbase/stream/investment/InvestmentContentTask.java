package com.backbase.stream.investment;

import com.backbase.stream.worker.model.StreamTask;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class InvestmentContentTask extends StreamTask {

    private final InvestmentContentData data;

    public InvestmentContentTask(String unitOfWorkId, InvestmentContentData data) {
        super(unitOfWorkId);
        this.data = data;
    }

    @Override
    public String getName() {
        return "investment-content";
    }

}
