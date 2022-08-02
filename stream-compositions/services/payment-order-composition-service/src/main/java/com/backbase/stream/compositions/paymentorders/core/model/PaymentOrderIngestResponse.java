package com.backbase.stream.compositions.paymentorders.core.model;

import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class PaymentOrderIngestResponse {
    private final String arrangementId;
    private final List<PaymentOrderPostResponse> paymentOrderPostResponses;
}
