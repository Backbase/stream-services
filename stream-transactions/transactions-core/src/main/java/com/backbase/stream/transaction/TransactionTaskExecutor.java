package com.backbase.stream.transaction;

import com.backbase.dbs.transaction.api.service.v4.TransactionPresentationServiceApi;
import com.backbase.dbs.transaction.api.service.v4.model.TransactionsPostRequestBody;
import com.backbase.dbs.transaction.api.service.v4.model.TransactionsPostResponseBody;
import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.exception.StreamTaskException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class TransactionTaskExecutor implements StreamTaskExecutor<TransactionTask> {

    /** Aligns with service-api v4: `(^[1-9][0-9]*)|0$` as a full-string match, maxLength 20. */
    private static final Pattern SEQUENCE_NUMBER_PATTERN = Pattern.compile("^(?:0|[1-9]\\d{0,19})$");
    private static final int SEQUENCE_NUMBER_MAX_LENGTH = 20;

    private final TransactionPresentationServiceApi transactionPresentationServiceApi;

    public TransactionTaskExecutor(TransactionPresentationServiceApi transactionPresentationServiceApi) {
        this.transactionPresentationServiceApi = transactionPresentationServiceApi;
    }

    @Override
    public Mono<TransactionTask> executeTask(TransactionTask streamTask) {
        List<TransactionsPostRequestBody> data = streamTask.getData();
        data.forEach(this::ensureSequenceNumber);
        String externalIds = streamTask.getData().stream().map(TransactionsPostRequestBody::getExternalId)
            .collect(Collectors.joining(","));
        log.info("Post {} transactions: ", data.size());
        return transactionPresentationServiceApi.postTransactions(data, null, null, null)
            .onErrorResume(WebClientResponseException.class, throwable -> {
                streamTask.error("transactions", "post", "failed", externalIds, null, throwable,
                    throwable.getResponseBodyAsString(), "Failed to ingest transactions");
                return Mono.error(new StreamTaskException(streamTask, throwable,
                    "Failed to Ingest Transactions: " + throwable.getResponseBodyAsString()));
            })
            .collectList()
            .map(transactionIds -> {
                streamTask.info("transactions", "post", "success", externalIds, transactionIds.stream().map(
                    TransactionsPostResponseBody::getId).collect(Collectors.joining(",")), "Ingested Transactions");
                streamTask.setResponse(transactionIds);
                return streamTask;
            });
    }

    @Override
    public Mono<TransactionTask> rollBack(TransactionTask streamTask) {
        return Mono.just(streamTask);
    }

    /**
     * service-api v4 requires sequenceNumber. Prefer existing value; else numeric externalId; else "0".
     */
    void ensureSequenceNumber(TransactionsPostRequestBody body) {
        if (body == null) {
            return;
        }
        if (isValidSequenceNumber(body.getSequenceNumber())) {
            return;
        }
        String externalId = body.getExternalId();
        if (isValidSequenceNumber(externalId)) {
            body.setSequenceNumber(externalId);
            return;
        }
        body.setSequenceNumber("0");
    }

    private static boolean isValidSequenceNumber(String value) {
        return value != null
            && !value.isBlank()
            && value.length() <= SEQUENCE_NUMBER_MAX_LENGTH
            && SEQUENCE_NUMBER_PATTERN.matcher(value).matches();
    }

}
