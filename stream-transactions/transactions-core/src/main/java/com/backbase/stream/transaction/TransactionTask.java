package com.backbase.stream.transaction;

import com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostRequestBody;
import com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostResponseBody;
import com.backbase.stream.worker.model.StreamTask;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class TransactionTask extends StreamTask {

    private List<TransactionsPostRequestBody> data;
    private List<TransactionsPostResponseBody> response;

    public TransactionTask(String unitOfWorkId, List<TransactionsPostRequestBody> data) {
        super(unitOfWorkId);
        this.data = data;
    }

    @Override
    public String getName() {
        return "transaction";
    }
}
