package com.backbase.streams.tailoredvalue.plan;

import com.backbase.stream.worker.model.StreamTask;
import com.backbase.tailoredvalue.planmanager.service.api.v0.model.UserPlanUpdateRequestBody;
import com.backbase.tailoredvalue.planmanager.service.api.v0.model.UserPlanUpdateResponseBody;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PlansTask extends StreamTask {

    public PlansTask(String unitOfWorkId, String internalUserId, UserPlanUpdateRequestBody data, String planName) {
        super(unitOfWorkId);
        this.internalUserId = internalUserId;
        this.planName = planName;
        this.reqData = data;
    }

    private String internalUserId;
    private String planName;
    private UserPlanUpdateRequestBody reqData;
    private UserPlanUpdateResponseBody response;

    @Override
    public String getName() {
        return getId();
    }
}
