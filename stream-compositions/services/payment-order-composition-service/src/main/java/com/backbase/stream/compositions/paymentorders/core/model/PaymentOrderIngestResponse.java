package com.backbase.stream.compositions.paymentorders.core.model;

import com.backbase.stream.model.PaymentOrderIngestContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PaymentOrderIngestResponse {
    private final String memberNumber;
    private final PaymentOrderIngestContext paymentOrderIngestContext;
}
