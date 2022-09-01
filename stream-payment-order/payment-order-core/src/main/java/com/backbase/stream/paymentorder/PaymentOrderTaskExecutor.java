package com.backbase.stream.paymentorder;

import com.backbase.dbs.paymentorder.api.service.v2.PaymentOrdersApi;
import com.backbase.dbs.paymentorder.api.service.v2.model.GetPaymentOrderResponse;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostFilterRequest;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostFilterResponse;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostRequest;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostResponse;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPutRequest;
import com.backbase.dbs.paymentorder.api.service.v2.model.Status;
import com.backbase.dbs.paymentorder.api.service.v2.model.UpdateStatusPut;
import com.backbase.stream.mappers.PaymentOrderTypeMapper;
import com.backbase.stream.model.PaymentOrderIngestContext;
import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.exception.StreamTaskException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.backbase.dbs.paymentorder.api.service.v2.model.Status.*;

@Slf4j
@RequiredArgsConstructor
public class PaymentOrderTaskExecutor implements StreamTaskExecutor<PaymentOrderTask> {

    private final PaymentOrdersApi paymentOrdersApi;
    private final PaymentOrderTypeMapper paymentOrderTypeMapper;

    //TODO
    @Override
    public Mono<PaymentOrderTask> executeTask(PaymentOrderTask streamTask) {

        PaymentOrderIngestContext paymentOrderIngestContext = new PaymentOrderIngestContext();
        List<PaymentOrderPostRequest> corePaymentOrderPostRequestList = streamTask.getData();
        paymentOrderIngestContext.corePaymentOrders(corePaymentOrderPostRequestList);
        paymentOrderIngestContext.accountNumber("tblade");

        //todo once lists are finalized - send them to the right DBS service (post, update, delete)
        var paymentsContext = buildPaymentOrderIngestContext(paymentOrderIngestContext)
                .flatMap(this::updateExistingScheduledTransfers)
                .flatMap(this::persistNewPaymentOrders);



//        var updatePayments = updateExistingScheduledTransfers(createFakeUpdateList());

        String externalIds = streamTask.getData().stream().map(PaymentOrderPostRequest::getBankReferenceId)
                .collect(Collectors.joining(","));

        //todo this is for payment context
//        var paymentsContext =
//                buildPaymentOrderIngestContext(paymentOrderIngestContext);
////                persistNewPaymentOrders(paymentOrderIngestContext);

        return paymentsContext
                .onErrorResume(WebClientResponseException.class, throwable -> {
                    streamTask.error("payments", "post", "failed", externalIds, null, throwable,
                            throwable.getResponseBodyAsString(), "Failed to ingest payment order");
                    return Mono.error(new StreamTaskException(streamTask, throwable,
                            "Failed to Ingest Payment Order: " + throwable.getMessage()));
                })
                .map(transactionIds -> {
                    streamTask.error("payments", "post", "success", externalIds, transactionIds.accountNumber(), "Ingested Payment Order");
//                            .map(PaymentOrderPostResponse::getId).collect(Collectors.joining(",")), "Ingested Payment Order");
                    streamTask.setResponse(transactionIds);
                    return streamTask;
                });


        //todo test with single list
//        var newPayments =
//                persistNewScheduledTransfers(paymentOrderPostRequestList);
//
//        return newPayments.
//                onErrorResume(WebClientResponseException.class, throwable -> {
//                    streamTask.error("payments", "post", "failed", externalIds, null, throwable,
//                            throwable.getResponseBodyAsString(), "Failed to ingest payment order");
//                    return Mono.error(new StreamTaskException(streamTask, throwable,
//                            "Failed to Ingest Payment Order: " + throwable.getMessage()));
//                })
//                .map(transactionIds -> {
//                    streamTask.error("payments", "post", "success", externalIds, transactionIds.stream().map(
//                            PaymentOrderPostResponse::getId).collect(Collectors.joining(",")), "Ingested Payment Order");
//                    streamTask.setResponse(transactionIds);
//                    return streamTask;
//                });
    }

    public Mono<List<PaymentOrderPostResponse>> persistNewScheduledTransfers(
            List<PaymentOrderPostRequest> paymentOrderPostRequestList) {

        PaymentOrderIngestContext paymentOrderIngestContext = new PaymentOrderIngestContext();

        return Flux.fromIterable(paymentOrderPostRequestList)
                .map(s -> s.bankReferenceId("B"))
                .limitRate(1)
                .delayElements(Duration.ofMillis(10))
                .flatMap(this::persistNewPaymentOrder)
                .doOnNext(response -> log.debug("Saved new Transfer: {}", response))
                .collectList()
                .doOnSuccess(transferResults ->
                        log.debug("Successfully persisted: {} new scheduled transfers.",
                                transferResults.size()));
    }

    public Mono<PaymentOrderIngestContext> buildPaymentOrderIngestContext(PaymentOrderIngestContext paymentOrderIngestContext) {

        return getPersistedScheduledTransfers(paymentOrderIngestContext)
                .flatMap(this::buildNewList)
                .map(response -> {
                    log.debug("Ingestion context successfully build and ready for add, update and delete");
                    return response;
                });
    }

    public Mono<PaymentOrderIngestContext> buildNewList(PaymentOrderIngestContext paymentOrderIngestContext) {

        List<PaymentOrderPutRequest> updatePaymentOrders = new ArrayList<>();
        List<PaymentOrderPostRequest> newPaymentOrders = new ArrayList<>();

        paymentOrderIngestContext.corePaymentOrders().forEach(corePayment -> {
                    if(paymentOrderIngestContext.existingPaymentOrders().contains(corePayment.getBankReferenceId())) {
                        updatePaymentOrders.add(paymentOrderTypeMapper.mapPaymentOrderPostRequest(corePayment));
                    } else {
                        newPaymentOrders.add(corePayment);
                    }
                });

        //todo build logic for delete

        paymentOrderIngestContext.updatePaymentOrders(updatePaymentOrders);
        paymentOrderIngestContext.newPaymentOrders(newPaymentOrders);
        return Mono.just(paymentOrderIngestContext);
    }

//    /**
//     * Gets products related to the payments and already persisted scheduled payment orders.
//     * Note: Gets all transfers related data to be able to map and ingest the incoming transfers.
//     * @param paymentOrderIngestContext Holds all the Ingestion details.
//     * @return The Payment Order ingestion context
//     */
//    public Mono<PaymentOrderIngestContext> getPersistedOrders(PaymentOrderIngestContext paymentOrderIngestContext) {
//
//        return getPersistedScheduledTransfers(paymentOrderIngestContext)
//                .map(paymentOrderIngestContext::existingPaymentOrders)
//                .doOnError(throwable -> log.error("Error getting all data to ingest transfers: {}", throwable.getMessage()))
//                .doOnSuccess(response -> log.info("Fetched scheduled payment data from dbs successfully"));
//    }

    /**
     * Gets the list of payments that are persisted in DBS for a specific user.
     * The transfers have been divided by destination product type.
     *
     * @param paymentOrderIngestContext Holds all the Ingestion details.
     * @return A Mono of List of GetPaymentOrderResponse.
     */
    public @NotNull @Valid Mono<PaymentOrderIngestContext> getPersistedScheduledTransfers(PaymentOrderIngestContext paymentOrderIngestContext) {

        List<GetPaymentOrderResponse> listOfPayments = new ArrayList<>();

        return getPayments(paymentOrderIngestContext.accountNumber())
                .expand(response -> {
                    var totalEle = response.getTotalElements().intValue();
                    if (totalEle==0) {
                        // stop the loop
                        return Mono.empty();
                    }
                    return getPayments(paymentOrderIngestContext.accountNumber());
                })
                .map(PaymentOrderPostFilterResponse::getPaymentOrders)
                .collectList()
                .map(response -> {
                    for (List<GetPaymentOrderResponse> responseList : response) {
                        listOfPayments.addAll(responseList);
                    }
                    return listOfPayments;
                })
                .map(getPaymentOrderResponses -> paymentOrderIngestContext.existingPaymentOrders(getPaymentOrderResponses))
                .doOnSuccess(result ->
                        log.debug("Successfully fetched dbs scheduled payment orders"));
    }


    /**
     * Submits a new payment request to the payment order service.
     *
     * @param paymentOrderIngestContext Holds details of current payment ingestion.
     * @return The list of all the response for each request. List<PaymentOrderPostResponse>
     */
    public Mono<PaymentOrderIngestContext> persistNewPaymentOrders(
            PaymentOrderIngestContext paymentOrderIngestContext) {

        List<PaymentOrderPostRequest> paymentOrderPostRequestList = paymentOrderIngestContext.newPaymentOrders();

        return Flux.fromIterable(paymentOrderPostRequestList)
                .limitRate(1)
                .delayElements(Duration.ofMillis(10))
                .flatMap(this::persistNewPaymentOrder)
                .doOnNext(response -> log.debug("Saved new Payment Order: {}", response))
                .collectList()
                .map(paymentOrderPostResponses -> paymentOrderIngestContext.newAddedPaymentOrders(paymentOrderPostResponses))
                .doOnSuccess(paymentOrderResult ->
                        log.debug("Successfully persisted: {} new scheduled transfers.",
                                paymentOrderResult.newAddedPaymentOrders().size()));
    }

    /**
     * Submit request to update a payment status.
     *
     * @param paymentOrderIngestContext Holds details of current payment ingestion.
     * @return The response from the api. Mono<List<UpdateStatusPut>>
     */
    public Mono<PaymentOrderIngestContext> updateExistingScheduledTransfers(
            PaymentOrderIngestContext paymentOrderIngestContext) {

        return Flux.fromIterable(paymentOrderIngestContext.updatePaymentOrders())
                .limitRate(1)
                .delayElements(Duration.ofMillis(100))
                .flatMap(request -> updatePaymentOrderStatus(
                        request.getBankReferenceId(),
                        request.getStatus(),
                        request.getBankStatus(),
                        request.getNextExecutionDate()
                ))
                .doOnNext(response -> log.debug("Updated Payment Order status: {}", response))
                .collectList()
                .map(paymentOrderIngestContext::updatedPaymentOrders)
                .onErrorContinue((t, o) -> log.error(String.format("Update status failed: %s", o), t))
                .doOnSuccess(paymentOrderResult ->
                        log.debug("Successfully persisted: {} Payment Order updates.",
                                paymentOrderResult.updatedPaymentOrders().size()));
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
    private Mono<PaymentOrderPostResponse> persistNewPaymentOrder(PaymentOrderPostRequest newPaymentOrderRequest) {
        newPaymentOrderRequest.setId(generateTransferUniqueId());
        return paymentOrdersApi.postPaymentOrder(
                    newPaymentOrderRequest);

    }

    /**
     * Calls the payment order service to update the status of an existing payment.
     *
     * @param bankReferenceId   The bank reference id.
     * @param status            the status of the transaction
     * @param bankStatus        The bank status.
     * @param nextExecutionDate The next execution date.
     * @return A Mono with the response from the service api.
     */
    private Mono<UpdateStatusPut> updatePaymentOrderStatus(String bankReferenceId,
                                                     Status status,
                                                     String bankStatus,
                                                     LocalDate nextExecutionDate) {
        var updatePaymentStatus = new UpdateStatusPut()
                .bankReferenceId(bankReferenceId)
                .status(status)
                .bankStatus(bankStatus)
                .nextExecutionDate(nextExecutionDate);
        return paymentOrdersApi.putUpdateStatus(updatePaymentStatus);
    }

    /**
     * Calls the payment order service to update the status of an existing payment.
     *
     * @param internalPaymentOrderId   The DBS internal Payment Order id.
     * @return A Mono with the response from the service api.
     */
    private Mono<Void> deletePaymentOrder(String internalPaymentOrderId) {
        return paymentOrdersApi.deletePaymentOrder(internalPaymentOrderId);
    }

    /**
     * Calls the payment order service to retrieve existing payments.
     *
     * @param accessNumber   The user's external id.
     * @return A Mono with the response from the service api.
     */
    private Mono<PaymentOrderPostFilterResponse> getPayments(String accessNumber) {
        var paymentOrderPostFilterRequest = new PaymentOrderPostFilterRequest();
        paymentOrderPostFilterRequest.setCreatedBy(accessNumber);
        paymentOrderPostFilterRequest.setStatuses(
                List.of(READY, ACCEPTED, PROCESSED, CANCELLED, REJECTED, CANCELLATION_PENDING));
        return paymentOrdersApi.postFilterPaymentOrders(
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, Integer.MAX_VALUE, null,
                null, paymentOrderPostFilterRequest);
    }

    /**
     * Generates a unique id to use for new transfers
     * Note: fail circuit added in case validation fails for some odd reason.
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
                    throw new IndexOutOfBoundsException("Could not generate a new unique id transfer after several tries.");
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
        return Pattern.compile(regexUuidPattern)
                .matcher(uuid)
                .find();
    }
}
