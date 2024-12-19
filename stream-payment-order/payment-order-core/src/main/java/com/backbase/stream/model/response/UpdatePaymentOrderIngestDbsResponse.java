package com.backbase.stream.model.response;

import com.backbase.dbs.paymentorder.api.service.v3.model.PaymentOrderPutResponse;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public class UpdatePaymentOrderIngestDbsResponse implements PaymentOrderIngestDbsResponse {

    private final PaymentOrderPutResponse paymentOrderPutResponse;

}
