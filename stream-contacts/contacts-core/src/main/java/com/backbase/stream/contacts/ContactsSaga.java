package com.backbase.stream.contacts;

import com.backbase.dbs.contact.service.api.ContactsApi;
import com.backbase.dbs.contact.service.model.ContactsbulkingestionRequest;
import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.exception.StreamTaskException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class ContactsSaga implements StreamTaskExecutor<ContactsTask> {

    public static final String CREATE = "create";
    public static final String SUCCESS = "success";
    public static final String ERROR = "error";
    public static final String CREATED_SUCCESSFULLY = "Contacts created successfully";
    public static final String FAILED_TO_INGEST_CONTACTS = "Failed to ingest contacts";
    public static final String CONTACTS = "CONTACTS";
    private final ContactsApi contactsApi;

    public ContactsSaga(ContactsApi contactsApi) {
        this.contactsApi = contactsApi;
    }

    @Override
    public Mono<ContactsTask> executeTask(ContactsTask task) {
        ContactsbulkingestionRequest item = task.getData();

        log.info("Started ingestion of contacts for user {}", item.getAccessContext());

        return contactsApi.postContactsBulkIngestion(item)
            .map(contactsbulkingestionPostResponseBody -> {
                task.setResponse(contactsbulkingestionPostResponseBody);
                task.info(CONTACTS, CREATE, SUCCESS, item.getAccessContext().toString(), contactsbulkingestionPostResponseBody.getSuccessCount().toString(), CREATED_SUCCESSFULLY);
                return task;
            })
            .onErrorResume(throwable -> {
                task.error(CONTACTS, CREATE, ERROR, item.getAccessContext().toString(), null, throwable, "Failed to ingest contacts: " + throwable.getMessage(), FAILED_TO_INGEST_CONTACTS);
                return Mono.error(new StreamTaskException(task, throwable, FAILED_TO_INGEST_CONTACTS));
            });

    }

    @Override
    public Mono<ContactsTask> rollBack(ContactsTask limitsTask) {
        return null;
    }
}
