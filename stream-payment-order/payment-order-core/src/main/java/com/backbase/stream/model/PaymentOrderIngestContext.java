package com.backbase.stream.model;

import com.backbase.dbs.paymentorder.api.service.v2.model.GetPaymentOrderResponse;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostRequest;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostResponse;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPutRequest;
import com.backbase.dbs.paymentorder.api.service.v2.model.UpdateStatusPut;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(fluent = true)
public class PaymentOrderIngestContext {

    private String accountNumber;
    private List<PaymentOrderPostRequest> corePaymentOrders = new ArrayList<>();
    private List<GetPaymentOrderResponse> existingPaymentOrders = new ArrayList<>();


    private List<PaymentOrderPostRequest> newPaymentOrders = new ArrayList<>();
    private List<PaymentOrderPutRequest> updatePaymentOrders = new ArrayList<>();
    // deletePaymentOrders = new ArrayList<>();

    //todo should we carry responses in the context?
    private List<UpdateStatusPut> updatedPaymentOrders = new ArrayList<>();
    private List<PaymentOrderPostResponse> newAddedPaymentOrders = new ArrayList<>();
}
