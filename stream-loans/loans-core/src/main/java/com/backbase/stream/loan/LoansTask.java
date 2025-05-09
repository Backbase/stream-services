package com.backbase.stream.loan;

import com.backbase.loan.inbound.api.service.v2.model.BatchResponseItemExtended;
import com.backbase.stream.legalentity.model.Loan;
import com.backbase.stream.worker.model.StreamTask;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class LoansTask extends StreamTask {

    public LoansTask(String unitOfWorkId, List<Loan> data) {
        super(unitOfWorkId);
        this.data = data;
    }

    private List<Loan> data;

    public List<Loan> getData() {
        return data;
    }

    public void setData(List<Loan> data) {
        this.data = data;
    }

    public BatchResponseItemExtended getResponse() {
        return response;
    }

    public void setResponse(BatchResponseItemExtended response) {
        this.response = response;
    }

    private BatchResponseItemExtended response;

    @Override
    public String getName() {
        return "loan";
    }
}
