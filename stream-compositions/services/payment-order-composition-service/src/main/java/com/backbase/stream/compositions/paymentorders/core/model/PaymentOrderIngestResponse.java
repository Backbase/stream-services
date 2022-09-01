package com.backbase.stream.compositions.paymentorders.core.model;

import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostResponse;
import com.backbase.stream.model.PaymentOrderIngestContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class PaymentOrderIngestResponse {
    private final String memberNumber;
//    private final List<PaymentOrderPostResponse> paymentOrderPostResponses;

    //todo with payment context
    private final PaymentOrderIngestContext paymentOrderIngestContext;
}
