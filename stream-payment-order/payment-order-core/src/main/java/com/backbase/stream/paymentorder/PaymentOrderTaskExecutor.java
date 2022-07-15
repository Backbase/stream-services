package com.backbase.stream.paymentorder;

import com.backbase.dbs.paymentorder.api.service.v2.PaymentOrdersApi;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostRequest;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostResponse;
import com.backbase.stream.worker.StreamTaskExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public class PaymentOrderTaskExecutor implements StreamTaskExecutor<PaymentOrderTask> {

    private final PaymentOrdersApi paymentOrdersApi;

    //TODO
    @Override
    public Mono<PaymentOrderTask> executeTask(PaymentOrderTask streamTask) {
        List<PaymentOrderPostRequest> paymentOrderPostRequestList = streamTask.getData();

        Flux.fromIterable(paymentOrderPostRequestList)
                .limitRate(1)
                .delayElements(Duration.ofMillis(10))
                .flatMap(this::persistNewPaymentOrder)
                .doOnNext(response -> log.debug("Saved new Transfer: {}", response))
                .collectList()
                .doOnSuccess(transferResults ->
                        log.debug("Successfully persisted: {} new scheduled transfers.",
                                transferResults.size()));
        return null;
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
        return paymentOrdersApi.postPaymentOrder(
                addRecordIdAndBankReferenceId(newPaymentOrderRequest));
    }


    /**
     * Adds the unique record id and bank reference id to the request
     *
     * @param paymentOrderPostRequest The request to add the id and bank reference id to.
     * @return The request with the ids set.
     */
    private PaymentOrderPostRequest addRecordIdAndBankReferenceId(
            @NotNull PaymentOrderPostRequest paymentOrderPostRequest) {
        String uniqueId = generateTransferUniqueId();
        String transferLocator = "";//todo Objects.requireNonNull(paymentOrderPostRequest.getAdditions())
                //.get(TransferAdditions.TRANSFER_LOCATOR);
        paymentOrderPostRequest.setId(uniqueId);
        paymentOrderPostRequest.setBankReferenceId(buildBankReferenceId(uniqueId, transferLocator));
        return paymentOrderPostRequest;
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

    /**
     * Builds the bank reference id from uuid and transfer locator
     *
     * @param uniqueId        The uuid.
     * @param transferLocator The transfer locator
     * @return A new bank reference id.
     */
    private String buildBankReferenceId(String uniqueId, String transferLocator) {
        return uniqueId.replace("-", "")
                .concat("-").concat(transferLocator);
    }
}
