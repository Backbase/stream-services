package com.backbase.stream.transaction;

import com.backbase.dbs.transaction.presentation.service.model.TransactionIds;
import com.backbase.dbs.transaction.presentation.service.model.TransactionItemPost;
import com.backbase.stream.worker.model.StreamTask;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class TransactionTask extends StreamTask {

    public TransactionTask(String unitOfWorkId, List<TransactionItemPost> data) {
        super(unitOfWorkId);
        this.data = data;
    }

    private List<TransactionItemPost> data;
    private List<TransactionIds> response;

    @Override
    public String getName() {
        return "transaction";
    }
}
