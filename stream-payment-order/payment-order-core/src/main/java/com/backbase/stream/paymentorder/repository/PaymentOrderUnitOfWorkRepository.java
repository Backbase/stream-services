package com.backbase.stream.paymentorder.repository;

import com.backbase.stream.paymentorder.PaymentOrderTask;
import com.backbase.stream.worker.repository.UnitOfWorkRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentOrderUnitOfWorkRepository
    extends UnitOfWorkRepository<PaymentOrderTask, String> {

}
