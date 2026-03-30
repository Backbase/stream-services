package com.backbase.stream.contact;

import com.backbase.dbs.contact.api.service.v2.ContactsApi;
import com.backbase.dbs.contact.api.service.v2.model.ContactsBulkPostRequestBody;
import com.backbase.dbs.contact.api.service.v2.model.ContactsBulkPostResponseBody;
import com.backbase.stream.configuration.ContactsWorkerConfigurationProperties;
import com.backbase.stream.worker.StreamTaskExecutor;
import java.util.concurrent.Semaphore;
import javax.annotation.Nonnull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@RequiredArgsConstructor
@Getter
public class ContactsSaga implements StreamTaskExecutor<ContactsTask> {

    public static final String ENTITY = "contact";
    public static final String CREATE = "create";
    public static final String SUCCESS = "success";
    public static final String ERROR = "error";
    public static final String CREATED_SUCCESSFULLY = "Contact created successfully";
    public static final String FAILED_TO_INGEST_CONTACTS = "Failed to ingest contacts";
    private final ContactsApi contactsApi;
    private final ContactsWorkerConfigurationProperties contactsWorkerConfigurationProperties;

    @Value("${backbase.stream.contact.worker.semaphorePermitsCount:1}")
    private int semaphorePermitsCount = 1;
    @Value("${backbase.stream.contact.worker.sequential:true}")
    private boolean sequential = true;

    /**
     * Ensures postContactsBulk calls are executed sequentially (one at a time), even when executeTask is invoked
     * concurrently.
     */
    private final Semaphore semaphore = new Semaphore(semaphorePermitsCount);

    @Override
    public Mono<ContactsTask> executeTask(ContactsTask contactsTask) {
        ContactsBulkPostRequestBody item = contactsTask.getData();

        log.info("Started ingestion of contacts for Type {}", item.getAccessContext().getScope());
        // Acquire the permit on boundedElastic to avoid blocking the event-loop thread,
        // then execute the API call. doFinally releases the permit on complete/error/cancel.
        Mono<ContactsBulkPostResponseBody> insert = sequential ? sequentialContactsInsert(item) : contactsInsert(item);
        return insert
            .map(contactsBulkPostResponse -> {
                contactsTask.setResponse(contactsBulkPostResponse);
                contactsTask.info(ENTITY, CREATE, SUCCESS, item.getAccessContext().getExternalUserId(), null,
                    CREATED_SUCCESSFULLY);
                return contactsTask;
            })
            .doOnError(throwable -> {
                if (throwable instanceof WebClientResponseException ex) {
                    log.warn("Execute contacts POST client failed (falling back to PATCH client): status={}, body={}",
                        ex.getStatusCode(), ex.getResponseBodyAsString());
                } else {
                    log.warn("Execute contacts POST client failed (falling back to PATCH client): {}",
                        throwable.getMessage());
                }
            })
            .onErrorResume(throwable -> {
                if (contactsWorkerConfigurationProperties.isContinueOnError()) {
                    contactsTask.warn(ENTITY, CREATE, ERROR, item.getAccessContext().getExternalUserId(), null,
                        "Failed to ingest contact but continueOnError is enabled " + throwable.getMessage(),
                        FAILED_TO_INGEST_CONTACTS);
                    return Mono.just(contactsTask);
                } else {
                    contactsTask.error(ENTITY, CREATE, ERROR, item.getAccessContext().getExternalUserId(), null,
                        throwable, "Failed to ingest contact " + throwable.getMessage(), FAILED_TO_INGEST_CONTACTS);
                    return Mono.empty();
                }
            });
    }

    @Nonnull
    private Mono<ContactsBulkPostResponseBody> sequentialContactsInsert(ContactsBulkPostRequestBody item) {
        return Mono.fromCallable(() -> {
                log.info("Acquiring semaphore for contacts ingestion of Type {} and externalUserId {}",
                    item.getAccessContext().getScope(), item.getAccessContext().getExternalUserId());
                semaphore.acquire();   // blocking – runs on boundedElastic
                return item;
            })
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(i -> contactsApi.postContactsBulk(i)
                .doFinally(signal -> semaphore.release()));
    }

    @Nonnull
    private Mono<ContactsBulkPostResponseBody> contactsInsert(ContactsBulkPostRequestBody item) {
        return contactsApi.postContactsBulk(item);
    }

    @Override
    public Mono<ContactsTask> rollBack(ContactsTask contactsTask) {
        return null;
    }
}
