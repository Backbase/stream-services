package com.backbase.stream.paymentorder;

import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostRequest;
import com.backbase.stream.worker.model.StreamTask;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class PaymentOrderTask extends StreamTask {

    public PaymentOrderTask(String unitOfWorkId, List<PaymentOrderPostRequest> data) {
        super(unitOfWorkId);
        this.data = data;
    }

    private List<PaymentOrderPostRequest> data;
    private List<PaymentOrderPostRequest> response;
    @Override
    public String getName() {
        return "paymentorder";
    }
}
