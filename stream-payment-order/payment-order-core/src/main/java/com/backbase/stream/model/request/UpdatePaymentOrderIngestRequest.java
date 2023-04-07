package com.backbase.stream.model.request;

import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPutRequest;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class UpdatePaymentOrderIngestRequest implements PaymentOrderIngestRequest {

    private final PaymentOrderPutRequest paymentOrderPutRequest;

    @Override
    public String getBankReferenceId() {
        return paymentOrderPutRequest.getBankReferenceId();
    }
}
