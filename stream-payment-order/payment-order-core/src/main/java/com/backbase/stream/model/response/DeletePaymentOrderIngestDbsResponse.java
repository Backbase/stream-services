package com.backbase.stream.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public class DeletePaymentOrderIngestDbsResponse implements PaymentOrderIngestDbsResponse {

    private final String paymentOrderId;
}
