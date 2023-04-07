package com.backbase.stream.task;

import com.backbase.dbs.paymentorder.api.service.v2.PaymentOrdersApi;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostRequest;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostResponse;
import com.backbase.stream.common.PaymentOrderBaseTest;
import com.backbase.stream.config.PaymentOrderWorkerConfigurationProperties;
import com.backbase.stream.model.request.NewPaymentOrderIngestRequest;
import com.backbase.stream.model.request.PaymentOrderIngestRequest;
import com.backbase.stream.paymentorder.PaymentOrderTask;
import com.backbase.stream.paymentorder.PaymentOrderTaskExecutor;
import com.backbase.stream.paymentorder.PaymentOrderUnitOfWorkExecutor;
import com.backbase.stream.worker.model.UnitOfWork;
import com.backbase.stream.worker.repository.UnitOfWorkRepository;
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
public class PaymentOrderUnitOfWorkExecutorTest extends PaymentOrderBaseTest {

  private final PaymentOrderWorkerConfigurationProperties streamWorkerConfiguration =
      new PaymentOrderWorkerConfigurationProperties();
  @Mock private PaymentOrdersApi paymentOrdersApi;
  private final PaymentOrderTaskExecutor streamTaskExecutor =
      new PaymentOrderTaskExecutor(paymentOrdersApi);

  @InjectMocks
  private PaymentOrderUnitOfWorkExecutor paymentOrderUnitOfWorkExecutor =
      new PaymentOrderUnitOfWorkExecutor(
          repository,
          streamTaskExecutor,
          streamWorkerConfiguration,
          paymentOrdersApi,
          paymentOrderTypeMapper);

  @Mock private UnitOfWorkRepository<PaymentOrderTask, String> repository;

  ;

  @Test
  void test_prepareUnitOfWork_paymentOrderIngestRequestList() {
    List<PaymentOrderIngestRequest> paymentOrderIngestRequestList =
        List.of(
            new NewPaymentOrderIngestRequest(paymentOrderPostRequest.get(0)),
            new NewPaymentOrderIngestRequest(paymentOrderPostRequest.get(1)));

    PaymentOrderPostResponse paymentOrderPostResponse =
        new PaymentOrderPostResponse().id("po_post_resp_id").putAdditionsItem("key", "val");

    Mockito.lenient()
        .when(paymentOrdersApi.postPaymentOrder(Mockito.any()))
        .thenReturn(Mono.just(paymentOrderPostResponse));

    StepVerifier.create(
            paymentOrderUnitOfWorkExecutor.prepareUnitOfWork(paymentOrderIngestRequestList))
        .assertNext(
            unitOfWork -> {
              Assertions.assertTrue(
                  unitOfWork.getUnitOfOWorkId().startsWith("payment-orders-mixed-"));
              Assertions.assertEquals(UnitOfWork.State.NEW, unitOfWork.getState());
              Assertions.assertEquals(1, unitOfWork.getStreamTasks().size());
              Assertions.assertEquals(
                  paymentOrderIngestRequestList.size(),
                  unitOfWork.getStreamTasks().get(0).getData().size());
            });
  }

  @Test
  void test_prepareUnitOfWork_paymentOrderPostRequestFlux() {
    Flux<PaymentOrderPostRequest> paymentOrderPostRequestFlux =
        Flux.fromIterable(paymentOrderPostRequest);

    PaymentOrderPostResponse paymentOrderPostResponse =
        new PaymentOrderPostResponse().id("po_post_resp_id").putAdditionsItem("key", "val");

    Mockito.lenient()
        .when(paymentOrdersApi.postPaymentOrder(Mockito.any()))
        .thenReturn(Mono.just(paymentOrderPostResponse));

    StepVerifier.create(
            paymentOrderUnitOfWorkExecutor.prepareUnitOfWork(paymentOrderPostRequestFlux))
        .assertNext(
            unitOfWork -> {
              Assertions.assertTrue(
                  unitOfWork.getUnitOfOWorkId().startsWith("payment-orders-mixed-"));
              Assertions.assertEquals(UnitOfWork.State.NEW, unitOfWork.getState());
              Assertions.assertEquals(1, unitOfWork.getStreamTasks().size());
              Assertions.assertEquals(
                  paymentOrderPostRequest.size(),
                  unitOfWork.getStreamTasks().get(0).getData().size());
            });
  }
}
