package com.backbase.stream.compositions.paymentorders.core.model;

import com.backbase.stream.model.response.PaymentOrderIngestDbsResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PaymentOrderIngestResponse {

  private final String memberNumber;
  private final List<PaymentOrderIngestDbsResponse> paymentOrderIngestDbsResponses;
}
