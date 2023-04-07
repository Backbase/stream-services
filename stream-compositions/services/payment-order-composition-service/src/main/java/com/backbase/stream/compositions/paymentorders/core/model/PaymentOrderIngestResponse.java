package com.backbase.stream.compositions.paymentorders.core.model;

import com.backbase.stream.model.response.PaymentOrderIngestDbsResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class PaymentOrderIngestResponse {
    private final String memberNumber;
    private final List<PaymentOrderIngestDbsResponse> paymentOrderIngestDbsResponses;
}
