package com.backbase.stream;

import com.backbase.dbs.paymentorder.api.service.v2.PaymentOrdersApi;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostRequest;
import com.backbase.stream.paymentorder.PaymentOrderTask;
import com.backbase.stream.paymentorder.PaymentOrderUnitOfWorkExecutor;
import com.backbase.stream.worker.model.UnitOfWork;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * Main Payment Order Ingestion Service. Supports Retry and back pressure and controller number of payment order to ingest
 * per second.
 */
@Slf4j
public class PaymentOrderServiceImpl implements PaymentOrderService{

    private final PaymentOrdersApi paymentOrdersApi;
    private final PaymentOrderUnitOfWorkExecutor paymentOrderUnitOfWorkExecutor;

    public PaymentOrderServiceImpl(PaymentOrdersApi paymentOrdersApi,
                                   PaymentOrderUnitOfWorkExecutor paymentOrderUnitOfWorkExecutor) {
        this.paymentOrderUnitOfWorkExecutor = paymentOrderUnitOfWorkExecutor;
        this.paymentOrdersApi = paymentOrdersApi;
    }

    @Override
    public Flux<UnitOfWork<PaymentOrderTask>> processPaymentOrder(Flux<PaymentOrderPostRequest> paymentOrderPostRequestFlux) {
        return null;
    }

}
