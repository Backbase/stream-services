package com.backbase.stream.model.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class DeletePaymentOrderIngestRequest implements PaymentOrderIngestRequest  {

    private final String paymentOrderId;
    private final String bankReferenceId;

    @Override
    public String getBankReferenceId() {
        return bankReferenceId;
    }

}
