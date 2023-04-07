package com.backbase.stream.model;

import com.backbase.dbs.paymentorder.api.service.v2.model.GetPaymentOrderResponse;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostRequest;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(fluent = true)
public class PaymentOrderIngestContext {

    private String internalUserId;
    private List<PaymentOrderPostRequest> corePaymentOrder = new ArrayList<>();
    private List<GetPaymentOrderResponse> existingPaymentOrder = new ArrayList<>();
}
