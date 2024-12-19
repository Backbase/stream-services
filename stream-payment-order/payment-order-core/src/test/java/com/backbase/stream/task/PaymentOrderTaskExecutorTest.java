package com.backbase.stream.task;

import com.backbase.dbs.paymentorder.api.service.v3.PaymentOrdersApi;
import com.backbase.dbs.paymentorder.api.service.v3.model.PaymentOrderPostResponse;
import com.backbase.dbs.paymentorder.api.service.v3.model.PaymentOrderPutResponse;
import com.backbase.stream.common.PaymentOrderBaseTest;
import com.backbase.stream.model.request.DeletePaymentOrderIngestRequest;
import com.backbase.stream.model.request.NewPaymentOrderIngestRequest;
import com.backbase.stream.model.request.PaymentOrderIngestRequest;
import com.backbase.stream.model.request.UpdatePaymentOrderIngestRequest;
import com.backbase.stream.paymentorder.PaymentOrderTask;
import com.backbase.stream.paymentorder.PaymentOrderTaskExecutor;
import com.backbase.stream.worker.exception.StreamTaskException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class PaymentOrderTaskExecutorTest extends PaymentOrderBaseTest {

  @InjectMocks private PaymentOrderTaskExecutor paymentOrderTaskExecutor;

  @Mock private PaymentOrdersApi paymentOrdersApi;

  @Test
  void executeTask_NewPaymentOrderIngestRequest_Success() {
    List<PaymentOrderIngestRequest> paymentOrderIngestRequestList =
        List.of(
            new NewPaymentOrderIngestRequest(paymentOrderPostRequest.get(0)),
            new NewPaymentOrderIngestRequest(paymentOrderPostRequest.get(1)));

    PaymentOrderTask paymentOrderTask =
        new PaymentOrderTask("po_task_id", paymentOrderIngestRequestList);
    PaymentOrderPostResponse paymentOrderPostResponse =
        new PaymentOrderPostResponse().id("po_post_resp_id");

    Mockito.lenient()
        .when(paymentOrdersApi.postPaymentOrder(Mockito.any()))
        .thenReturn(Mono.just(paymentOrderPostResponse));

    StepVerifier.create(paymentOrderTaskExecutor.executeTask(paymentOrderTask))
        .assertNext(
            response -> {
              Assertions.assertNotNull(response.getResponses());
              Assertions.assertEquals(paymentOrderTask.getId(), response.getId());
              Assertions.assertEquals(
                  paymentOrderIngestRequestList.size(), response.getData().size());
              Assertions.assertEquals(
                  paymentOrderIngestRequestList.size(), response.getResponses().size());
            })
        .verifyComplete();
  }

  @Test
  void executeTask_NewPaymentOrderIngestRequest_Failure() {
    List<PaymentOrderIngestRequest> paymentOrderIngestRequestList =
        List.of(
            new NewPaymentOrderIngestRequest(paymentOrderPostRequest.get(0)),
            new NewPaymentOrderIngestRequest(paymentOrderPostRequest.get(1)));

    PaymentOrderTask paymentOrderTask =
        new PaymentOrderTask("po_task_id", paymentOrderIngestRequestList);
    WebClientResponseException webClientResponseException =
        buildWebClientResponseException(HttpStatus.BAD_REQUEST, "Bad Request");

    Mockito.lenient()
        .when(paymentOrdersApi.postPaymentOrder(Mockito.any()))
        .thenReturn(Mono.error(webClientResponseException));

    StepVerifier.create(paymentOrderTaskExecutor.executeTask(paymentOrderTask))
        .consumeErrorWith(
            e -> {
              Assertions.assertTrue(e instanceof StreamTaskException);
              Assertions.assertEquals(
                  "Failed to Ingest Payment Order: " + webClientResponseException.getMessage(),
                  e.getMessage());
              Assertions.assertEquals(
                  webClientResponseException.getMessage(), e.getCause().getMessage());
            })
        .verify();
  }

  @Test
  void executeTask_UpdatePaymentOrderIngestRequest_Success() {
    List<PaymentOrderIngestRequest> paymentOrderIngestRequestList =
        List.of(
            new UpdatePaymentOrderIngestRequest(
                buildPaymentOrderPutRequest(paymentOrderPostRequest.get(0))),
            new UpdatePaymentOrderIngestRequest(
                buildPaymentOrderPutRequest(paymentOrderPostRequest.get(1))));

    PaymentOrderTask paymentOrderTask =
        new PaymentOrderTask("po_task_id", paymentOrderIngestRequestList);
    PaymentOrderPutResponse paymentOrderPutResponse = new PaymentOrderPutResponse();
    paymentOrderPutResponse.id("po_put_resp_id");

    Mockito.lenient()
        .when(paymentOrdersApi.updatePaymentOrder(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Mono.just(paymentOrderPutResponse));

    StepVerifier.create(paymentOrderTaskExecutor.executeTask(paymentOrderTask))
        .assertNext(
            response -> {
              Assertions.assertNotNull(response.getResponses());
              Assertions.assertEquals(paymentOrderTask.getId(), response.getId());
              Assertions.assertEquals(
                  paymentOrderIngestRequestList.size(), response.getData().size());
              Assertions.assertEquals(
                  paymentOrderIngestRequestList.size(), response.getResponses().size());
            })
        .verifyComplete();
  }

  @Test
  void executeTask_UpdatePaymentOrderIngestRequest_Failure() {
    List<PaymentOrderIngestRequest> paymentOrderIngestRequestList =
        List.of(
            new UpdatePaymentOrderIngestRequest(
                buildPaymentOrderPutRequest(paymentOrderPostRequest.get(0))),
            new UpdatePaymentOrderIngestRequest(
                buildPaymentOrderPutRequest(paymentOrderPostRequest.get(1))));

    PaymentOrderTask paymentOrderTask =
        new PaymentOrderTask("po_task_id", paymentOrderIngestRequestList);
    WebClientResponseException webClientResponseException =
        buildWebClientResponseException(HttpStatus.BAD_REQUEST, "Bad Request for");

    Mockito.lenient()
        .when(paymentOrdersApi.updatePaymentOrder(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Mono.error(webClientResponseException));

    StepVerifier.create(paymentOrderTaskExecutor.executeTask(paymentOrderTask))
        .consumeErrorWith(
            e -> {
              Assertions.assertTrue(e instanceof StreamTaskException);
              Assertions.assertEquals(
                  "Failed to Ingest Payment Order: " + webClientResponseException.getMessage(),
                  e.getMessage());
              Assertions.assertEquals(
                  webClientResponseException.getMessage(), e.getCause().getMessage());
            })
        .verify();
  }

  @Test
  void executeTask_DeletePaymentOrderIngestRequest_Success() {
    List<PaymentOrderIngestRequest> paymentOrderIngestRequestList =
        List.of(
            new DeletePaymentOrderIngestRequest("po_id_1", "ref_id_1"),
            new DeletePaymentOrderIngestRequest("po_id_2", "ref_id_2"));

    PaymentOrderTask paymentOrderTask =
        new PaymentOrderTask("po_task_id", paymentOrderIngestRequestList);

    Mockito.lenient()
        .when(paymentOrdersApi.deletePaymentOrder(Mockito.any()))
        .thenReturn(Mono.empty());

    StepVerifier.create(paymentOrderTaskExecutor.executeTask(paymentOrderTask))
        .assertNext(
            response -> {
              Assertions.assertNotNull(response.getResponses());
              Assertions.assertEquals(paymentOrderTask.getId(), response.getId());
              Assertions.assertEquals(
                  paymentOrderIngestRequestList.size(), response.getData().size());
              Assertions.assertEquals(
                  paymentOrderIngestRequestList.size(), response.getResponses().size());
            })
        .verifyComplete();
  }

  @Test
  void executeTask_DeletePaymentOrderIngestRequest_Failure() {
    List<PaymentOrderIngestRequest> paymentOrderIngestRequestList =
        List.of(
            new DeletePaymentOrderIngestRequest("po_id_1", "ref_id_1"),
            new DeletePaymentOrderIngestRequest("po_id_2", "ref_id_2"));

    PaymentOrderTask paymentOrderTask =
        new PaymentOrderTask("po_task_id", paymentOrderIngestRequestList);
    WebClientResponseException webClientResponseException =
        buildWebClientResponseException(HttpStatus.BAD_REQUEST, "Bad Request");

    Mockito.lenient()
        .when(paymentOrdersApi.deletePaymentOrder(Mockito.any()))
        .thenReturn(Mono.error(webClientResponseException));

    StepVerifier.create(paymentOrderTaskExecutor.executeTask(paymentOrderTask))
        .consumeErrorWith(
            e -> {
              Assertions.assertTrue(e instanceof StreamTaskException);
              Assertions.assertEquals(
                  "Failed to Ingest Payment Order: " + webClientResponseException.getMessage(),
                  e.getMessage());
              Assertions.assertEquals(
                  webClientResponseException.getMessage(), e.getCause().getMessage());
            })
        .verify();
  }

  @Test
  public void test_rollback() {
    List<PaymentOrderIngestRequest> paymentOrderIngestRequestList =
        List.of(
            new DeletePaymentOrderIngestRequest("po_id_1", "ref_id_1"),
            new DeletePaymentOrderIngestRequest("po_id_2", "ref_id_2"));

    PaymentOrderTask paymentOrderTask =
        new PaymentOrderTask("po_task_id", paymentOrderIngestRequestList);

    StepVerifier.create(paymentOrderTaskExecutor.rollBack(paymentOrderTask))
        .assertNext(task -> Assertions.assertEquals(paymentOrderTask, task))
        .verifyComplete();
  }
}
