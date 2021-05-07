package com.backbase.stream;

import com.backbase.stream.approval.model.Approval;
import com.backbase.stream.worker.model.StreamTask;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
public class ApprovalTask extends StreamTask {

    private Approval approval;

    public ApprovalTask(Approval data) {
        super(data.getName());
        this.approval = data;
    }

    public ApprovalTask(String taskId, Approval data) {
        super(taskId);
        this.approval = data;
    }

    public Approval getData() {
        return approval;
    }

    public ApprovalTask data(Approval approval) {
        this.approval = approval;
        return this;
    }

    @Override
    public String getName() {
        return getId();
    }
}
