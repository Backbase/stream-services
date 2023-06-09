package com.backbase.stream;

import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostRequest;
import com.backbase.stream.paymentorder.PaymentOrderTask;
import com.backbase.stream.worker.model.UnitOfWork;
import reactor.core.publisher.Flux;

public interface PaymentOrderService {

    Flux<UnitOfWork<PaymentOrderTask>> processPaymentOrder(
        Flux<PaymentOrderPostRequest> paymentOrderPostRequestFlux);
}
