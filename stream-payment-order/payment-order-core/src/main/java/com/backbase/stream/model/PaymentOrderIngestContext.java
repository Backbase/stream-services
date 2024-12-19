package com.backbase.stream.model;

import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementSearchesListResponse;
import com.backbase.dbs.paymentorder.api.service.v3.model.GetPaymentOrderResponse;
import com.backbase.dbs.paymentorder.api.service.v3.model.PaymentOrderPostRequest;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class PaymentOrderIngestContext {

  private String internalUserId;
  private List<PaymentOrderPostRequest> corePaymentOrder = new ArrayList<>();
  private List<GetPaymentOrderResponse> existingPaymentOrder = new ArrayList<>();
  private List<ArrangementSearchesListResponse> arrangementIds = new ArrayList<>();
}
