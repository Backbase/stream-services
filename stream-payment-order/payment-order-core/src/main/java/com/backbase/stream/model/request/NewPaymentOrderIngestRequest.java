package com.backbase.stream.model.request;

import com.backbase.dbs.paymentorder.api.service.v3.model.PaymentOrderPostRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class NewPaymentOrderIngestRequest implements PaymentOrderIngestRequest {

  private final PaymentOrderPostRequest paymentOrderPostRequest;

  @Override
  public String getBankReferenceId() {
    return paymentOrderPostRequest.getBankReferenceId();
  }
}
