package com.backbase.stream.paymentorder;

import java.util.ArrayList;
import java.util.List;

import com.backbase.stream.model.request.PaymentOrderIngestRequest;
import com.backbase.stream.model.response.PaymentOrderIngestDbsResponse;
import com.backbase.stream.worker.model.StreamTask;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PaymentOrderTask extends StreamTask {

    public PaymentOrderTask(String unitOfWorkId,
                            List<PaymentOrderIngestRequest> data) {
        super(unitOfWorkId);
        this.data = data;
    }

    private List<PaymentOrderIngestRequest> data;
    private List<PaymentOrderIngestDbsResponse> responses = new ArrayList<>();

    @Override
    public String getName() {
        return "paymentorder";
    }
}
