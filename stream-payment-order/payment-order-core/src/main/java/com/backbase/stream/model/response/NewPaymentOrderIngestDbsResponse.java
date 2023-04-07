package com.backbase.stream.model.response;

import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostResponse;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public class NewPaymentOrderIngestDbsResponse implements PaymentOrderIngestDbsResponse {

    private final PaymentOrderPostResponse paymentOrderPostResponse;
}
