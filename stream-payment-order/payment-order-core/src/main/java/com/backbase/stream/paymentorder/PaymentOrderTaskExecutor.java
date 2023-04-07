package com.backbase.stream.paymentorder;

import com.backbase.dbs.paymentorder.api.service.v2.PaymentOrdersApi;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostRequest;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostResponse;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPutRequest;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPutResponse;
import com.backbase.stream.model.request.DeletePaymentOrderIngestRequest;
import com.backbase.stream.model.request.NewPaymentOrderIngestRequest;
import com.backbase.stream.model.request.PaymentOrderIngestRequest;
import com.backbase.stream.model.request.UpdatePaymentOrderIngestRequest;
import com.backbase.stream.model.response.DeletePaymentOrderIngestDbsResponse;
import com.backbase.stream.model.response.NewPaymentOrderIngestDbsResponse;
import com.backbase.stream.model.response.PaymentOrderIngestDbsResponse;
import com.backbase.stream.model.response.UpdatePaymentOrderIngestDbsResponse;
import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.exception.StreamTaskException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class PaymentOrderTaskExecutor implements StreamTaskExecutor<PaymentOrderTask> {

    private final PaymentOrdersApi paymentOrdersApi;

    private final String BANK_REFERENCE_ID_FIELD_NAME = "BANKREFERENCEID";

    @Override
    public Mono<PaymentOrderTask> executeTask(PaymentOrderTask streamTask) {

        String externalIds =
                streamTask.getData().stream()
                        .map(PaymentOrderIngestRequest::getBankReferenceId)
                        .collect(Collectors.joining(","));

        return Flux.fromIterable(streamTask.getData())
                .flatMap(this::executePaymentOrderRequest)
                .onErrorResume(
                        WebClientResponseException.class,
                        throwable -> {
                            streamTask.error(
                                    "payments",
                                    "post",
                                    "failed",
                                    externalIds,
                                    null,
                                    throwable,
                                    throwable.getResponseBodyAsString(),
                                    "Failed to ingest payment order");
                            return Mono.error(
                                    new StreamTaskException(
                                            streamTask,
                                            throwable,
                                            "Failed to Ingest Payment Order: "
                                                    + throwable.getMessage()));
                        })
                .collectList()
                .map(
                        paymentOrderIngestResponses -> {
                            streamTask.setResponses(paymentOrderIngestResponses);
                            return streamTask;
                        });
    }

    public Mono<PaymentOrderIngestDbsResponse> executePaymentOrderRequest(
            PaymentOrderIngestRequest paymentOrderIngestRequest) {
        if (paymentOrderIngestRequest instanceof NewPaymentOrderIngestRequest) {
            return persistNewPaymentOrders(paymentOrderIngestRequest);
        } else if (paymentOrderIngestRequest instanceof UpdatePaymentOrderIngestRequest) {
            return updatePaymentOrder(paymentOrderIngestRequest);
        } else if (paymentOrderIngestRequest instanceof DeletePaymentOrderIngestRequest) {
            return deletePaymentOrder(paymentOrderIngestRequest);
        }
        return Mono.empty();
    }

    /**
     * Submits a new payment request to the payment order service.
     *
     * @param paymentOrderIngestRequest
     * @return Mono<PaymentOrderIngestDbsResponse>
     */
    private Mono<PaymentOrderIngestDbsResponse> persistNewPaymentOrders(
            PaymentOrderIngestRequest paymentOrderIngestRequest) {
        NewPaymentOrderIngestRequest newPaymentOrderIngestRequest =
                (NewPaymentOrderIngestRequest) paymentOrderIngestRequest;
        return persistNewPaymentOrder(newPaymentOrderIngestRequest.getPaymentOrderPostRequest())
                .doOnNext(response -> log.debug("Saved new Payment Order: {}", response))
                .map(
                        paymentOrderPostResponse ->
                                new NewPaymentOrderIngestDbsResponse(paymentOrderPostResponse));
    }

    /**
     * Submit request to update a payment status.
     *
     * @param paymentOrderIngestRequest
     * @return Mono<PaymentOrderIngestDbsResponse>
     */
    private Mono<PaymentOrderIngestDbsResponse> updatePaymentOrder(
            PaymentOrderIngestRequest paymentOrderIngestRequest) {
        UpdatePaymentOrderIngestRequest updatePaymentOrderIngestRequest =
                (UpdatePaymentOrderIngestRequest) paymentOrderIngestRequest;
        PaymentOrderPutRequest paymentOrderPutRequest =
                updatePaymentOrderIngestRequest.getPaymentOrderPutRequest();

        return updatePaymentOrderStatus(
                        paymentOrderPutRequest.getBankReferenceId(), paymentOrderPutRequest)
                .doOnNext(response -> log.debug("Updated Payment Order status: {}", response))
                .map(updateStatusPut -> new UpdatePaymentOrderIngestDbsResponse(updateStatusPut));
    }

    /**
     * Submit request to update a payment status.
     *
     * @param paymentOrderIngestRequest
     * @return Mono<PaymentOrderIngestDbsResponse>
     */
    private Mono<PaymentOrderIngestDbsResponse> deletePaymentOrder(
            PaymentOrderIngestRequest paymentOrderIngestRequest) {
        DeletePaymentOrderIngestRequest deletePaymentOrderIngestRequest =
                (DeletePaymentOrderIngestRequest) paymentOrderIngestRequest;
        return deletePaymentOrder(deletePaymentOrderIngestRequest.getPaymentOrderId())
                .doOnNext(response -> log.debug("Deleted Payment Order status: " + response))
                .map(paymentOrderId -> new DeletePaymentOrderIngestDbsResponse(paymentOrderId));
    }

    @Override
    public Mono<PaymentOrderTask> rollBack(PaymentOrderTask streamTask) {
        return Mono.just(streamTask);
    }

    /**
     * Makes a call to the Payment Order service to update an existing payment.
     *
     * @param newPaymentOrderRequest The new payment post order request.
     * @return A Mono with the service api response.
     */
    private Mono<PaymentOrderPostResponse> persistNewPaymentOrder(
            PaymentOrderPostRequest newPaymentOrderRequest) {
        newPaymentOrderRequest.setId(generateTransferUniqueId());
        return paymentOrdersApi.postPaymentOrder(newPaymentOrderRequest);
    }

    /**
     * Calls the payment order service to update the status of an existing payment.
     *
     * @param bankReferenceId The bank reference id.
     * @param paymentOrderPutRequest holds all the data that needs to be updated
     * @return A Mono with the response from the service api.
     */
    private Mono<PaymentOrderPutResponse> updatePaymentOrderStatus(
            String bankReferenceId, PaymentOrderPutRequest paymentOrderPutRequest) {

        return paymentOrdersApi.updatePaymentOrder(
                bankReferenceId, BANK_REFERENCE_ID_FIELD_NAME, paymentOrderPutRequest);
    }

    /**
     * Calls the payment order service to update the status of an existing payment.
     *
     * @param internalPaymentOrderId The DBS internal Payment Order id.
     * @return A Mono with the response from the service api.
     */
    private Mono<String> deletePaymentOrder(String internalPaymentOrderId) {
        return paymentOrdersApi
                .deletePaymentOrder(internalPaymentOrderId)
                .thenReturn(internalPaymentOrderId);
    }

    /**
     * Generates a unique id to use for new transfers Note: fail circuit added in case validation
     * fails for some odd reason.
     *
     * @return A UUID in string format.
     */
    public String generateTransferUniqueId() {
        String uuid;
        boolean isValid;
        final int maxTotalAttempts = 10;
        int failAttemptsCount = 0;
        do {
            uuid = UUID.randomUUID().toString();
            isValid = validateNewPaymentUuid(uuid);

            if (!isValid) {
                failAttemptsCount += 1;
                if (failAttemptsCount > maxTotalAttempts) {
                    throw new IndexOutOfBoundsException(
                            "Could not generate a new unique id transfer after several tries.");
                }
            }
        } while (!isValid);
        return uuid;
    }

    /**
     * Validates that a uuid conforms to the valid pattern.
     *
     * @param uuid The string UUID to check.
     * @return True if it is valid.
     */
    public boolean validateNewPaymentUuid(String uuid) {
        String regexUuidPattern =
                "^[\\da-fA-F]{8}-[\\da-fA-F]{4}-[1-5][\\da-fA-F]{3}-[89abAB][\\da-fA-F]{3}-[\\da-fA-F]{12}$";
        return Pattern.compile(regexUuidPattern).matcher(uuid).find();
    }
}
