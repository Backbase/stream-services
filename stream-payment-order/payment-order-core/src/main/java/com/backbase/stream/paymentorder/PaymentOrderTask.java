package com.backbase.stream.paymentorder;

import com.backbase.stream.model.request.PaymentOrderIngestRequest;
import com.backbase.stream.model.response.PaymentOrderIngestDbsResponse;
import com.backbase.stream.worker.model.StreamTask;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PaymentOrderTask extends StreamTask {

  private List<PaymentOrderIngestRequest> data;
  private List<PaymentOrderIngestDbsResponse> responses = new ArrayList<>();

  public PaymentOrderTask(String unitOfWorkId, List<PaymentOrderIngestRequest> data) {
    super(unitOfWorkId);
    this.data = data;
  }

  @Override
  public String getName() {
    return "paymentorder";
  }
}
