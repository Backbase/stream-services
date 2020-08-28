package com.backbase.stream.limit;

import com.backbase.dbs.limit.service.model.CreateLimitRequest;
import com.backbase.dbs.limit.service.model.CreateLimitResponse;
import com.backbase.stream.worker.model.StreamTask;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class LimitsTask extends StreamTask {

    public LimitsTask(String unitOfWorkId, CreateLimitRequest data) {
        super(unitOfWorkId);
        this.data = data;
    }

    private CreateLimitRequest data;
    private CreateLimitResponse response;

    @Override
    public String getName() {
        return "limit";
    }
}
