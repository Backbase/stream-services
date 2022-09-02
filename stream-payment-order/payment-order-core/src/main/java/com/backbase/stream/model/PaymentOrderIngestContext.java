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
    private List<PaymentOrderPostRequest> corePaymentOrder = new ArrayList<>();
    private List<GetPaymentOrderResponse> existingPaymentOrder = new ArrayList<>();


    private List<PaymentOrderPostRequest> newPaymentOrder = new ArrayList<>();
    private List<PaymentOrderPutRequest> updatePaymentOrder = new ArrayList<>();
    // we only need the internal Ids of the payment orders we need to delete
    private List<String> deletePaymentOrder = new ArrayList<>();

    private List<PaymentOrderPostResponse> newPaymentOrderResponse = new ArrayList<>();
    private List<UpdateStatusPut> updatedPaymentOrderResponse = new ArrayList<>();
    private List<String> deletePaymentOrderResponse = new ArrayList<>();

}
