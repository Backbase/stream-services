package com.backbase.stream.transaction;

import com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostRequestBody;
import com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostResponseBody;
import com.backbase.stream.worker.model.StreamTask;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class TransactionTask extends StreamTask {

    public TransactionTask(String unitOfWorkId, List<TransactionsPostRequestBody> data) {
        super(unitOfWorkId);
        this.data = data;
    }

    private List<TransactionsPostRequestBody> data;
    private List<TransactionsPostResponseBody> response;

    @Override
    public String getName() {
        return "transaction";
    }
}
