package com.backbase.stream.task;

import com.backbase.stream.common.PaymentOrderBaseTest;
import com.backbase.stream.model.request.NewPaymentOrderIngestRequest;
import com.backbase.stream.model.request.PaymentOrderIngestRequest;
import com.backbase.stream.paymentorder.PaymentOrderTask;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class PaymentOrderTaskTest extends PaymentOrderBaseTest {

    private final List<PaymentOrderIngestRequest> paymentOrderIngestRequestList =
            List.of(
                    new NewPaymentOrderIngestRequest(paymentOrderPostRequest.get(0)),
                    new NewPaymentOrderIngestRequest(paymentOrderPostRequest.get(1)));

    @Test
    void create_PaymentOrderIngestRequest() {
        PaymentOrderTask paymentOrderTask =
                new PaymentOrderTask("po_task_id", paymentOrderIngestRequestList);
        Assertions.assertEquals("paymentorder", paymentOrderTask.getName());
        Assertions.assertEquals(
                paymentOrderIngestRequestList.size(), paymentOrderTask.getData().size());
        for (int idx = 0; idx < paymentOrderTask.getData().size(); idx++) {
            Assertions.assertEquals(
                    paymentOrderIngestRequestList.get(idx).getBankReferenceId(),
                    paymentOrderTask.getData().get(idx).getBankReferenceId());
        }
    }

    @Test
    void set_PaymentOrderIngestRequest_Data() {
        PaymentOrderTask paymentOrderTask = new PaymentOrderTask("po_task_id", null);
        paymentOrderTask.setData(paymentOrderIngestRequestList);
        Assertions.assertEquals("paymentorder", paymentOrderTask.getName());
        Assertions.assertEquals(
                paymentOrderIngestRequestList.size(), paymentOrderTask.getData().size());
        for (int idx = 0; idx < paymentOrderTask.getData().size(); idx++) {
            Assertions.assertEquals(
                    paymentOrderIngestRequestList.get(idx).getBankReferenceId(),
                    paymentOrderTask.getData().get(idx).getBankReferenceId());
        }
    }
}
