package com.backbase.stream.service;

import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostRequest;
import com.backbase.stream.PaymentOrderServiceImpl;
import com.backbase.stream.common.PaymentOrderBaseTest;
import com.backbase.stream.model.request.NewPaymentOrderIngestRequest;
import com.backbase.stream.model.request.PaymentOrderIngestRequest;
import com.backbase.stream.paymentorder.PaymentOrderTask;
import com.backbase.stream.paymentorder.PaymentOrderUnitOfWorkExecutor;
import com.backbase.stream.worker.model.UnitOfWork;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class PaymentOrderServiceTest extends PaymentOrderBaseTest {

  private final Flux<PaymentOrderPostRequest> paymentOrderPostRequestFlux =
      Flux.fromIterable(paymentOrderPostRequest);
  @InjectMocks private PaymentOrderServiceImpl paymentOrderService;
  @Mock private PaymentOrderUnitOfWorkExecutor paymentOrderUnitOfWorkExecutor;

  @Test
  public void test_processPaymentOrder() {

    List<PaymentOrderIngestRequest> paymentOrderIngestRequestList =
        List.of(
            new NewPaymentOrderIngestRequest(paymentOrderPostRequest.get(0)),
            new NewPaymentOrderIngestRequest(paymentOrderPostRequest.get(1)));

    PaymentOrderTask paymentOrderTask =
        new PaymentOrderTask("po_task_id", paymentOrderIngestRequestList);
    UnitOfWork<PaymentOrderTask> unitOfWork = UnitOfWork.from("uow_id", paymentOrderTask);
    Flux<UnitOfWork<PaymentOrderTask>> unitOfWorkFlux = Flux.just(unitOfWork);

    Mockito.lenient()
        .when(paymentOrderUnitOfWorkExecutor.prepareUnitOfWork(paymentOrderPostRequestFlux))
        .thenReturn(unitOfWorkFlux);

    Mockito.lenient()
        .when(paymentOrderUnitOfWorkExecutor.executeUnitOfWork(unitOfWork))
        .thenReturn(Mono.just(unitOfWork));

    StepVerifier.create(paymentOrderService.processPaymentOrder(paymentOrderPostRequestFlux))
        .assertNext(
            uow -> {
              Assertions.assertEquals(UnitOfWork.State.NEW, uow.getState());
              Assertions.assertEquals(1, uow.getStreamTasks().size());
            })
        .verifyComplete();
  }
}
