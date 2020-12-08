package com.backbase.stream.contacts;

import com.backbase.dbs.contact.service.api.ContactsApi;
import com.backbase.dbs.contact.service.model.ContactsbulkdeleteRequest;
import com.backbase.dbs.contact.service.model.ContactsbulkingestionRequest;
import com.backbase.dbs.contact.service.model.ExternalContact;
import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.exception.StreamTaskException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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

//        contactsApi.getContacts(null, item.getAccessContext().getExternalLegalEntityId(), null, 0, 100, null):
        ContactsbulkdeleteRequest contactsbulkdeleteRequest = new ContactsbulkdeleteRequest();
        contactsbulkdeleteRequest.setAccessContext(item.getAccessContext());
        contactsbulkdeleteRequest.setExternalContactIds(item.getContacts().stream().map(ExternalContact::getExternalId).collect(Collectors.toList()));

        return
            contactsApi.postContactsBulkDelete(contactsbulkdeleteRequest)
                .publishOn(Schedulers.single())
                .onErrorResume(WebClientResponseException.class, throwable -> {
                    log.error("Failed to delete contacts: \n{} \nError: {}", item, throwable.getResponseBodyAsString());
                    task.error(CONTACTS, CREATE, ERROR, item.getAccessContext().toString(), null, throwable, "Failed to delete contacts: " + throwable.getResponseBodyAsString(), FAILED_TO_INGEST_CONTACTS);
                    return Mono.error(new StreamTaskException(task, throwable, FAILED_TO_INGEST_CONTACTS));
                })
                .doOnNext(v ->  log.info("Deleted all contacts for: {}",task.getName()))
                .then(contactsApi.postContactsBulkIngestion(item)
                    .map(contactsbulkingestionPostResponseBody -> {
                        log.info("Finished ingesting {} for {}", contactsbulkingestionPostResponseBody.getSuccessCount(), task.getData().getAccessContext().getExternalUserId());
                        task.setResponse(contactsbulkingestionPostResponseBody);
                        assert contactsbulkingestionPostResponseBody.getSuccessCount() != null;
                        task.info(CONTACTS, CREATE, SUCCESS, item.getAccessContext().toString(), contactsbulkingestionPostResponseBody.getSuccessCount().toString(), CREATED_SUCCESSFULLY);
                        return task;
                    })
                    .onErrorResume(WebClientResponseException.class, throwable -> {
                        log.error("Failed to ingest contacts: \n{} \nError: {}", item, throwable.getResponseBodyAsString());
                        task.error(CONTACTS, CREATE, ERROR, item.getAccessContext().toString(), null, throwable, "Failed to ingest contacts: " + throwable.getResponseBodyAsString(), FAILED_TO_INGEST_CONTACTS);
                        return Mono.error(new StreamTaskException(task, throwable, FAILED_TO_INGEST_CONTACTS));
                    }));

    }

    @Override
    public Mono<ContactsTask> rollBack(ContactsTask limitsTask) {
        return null;
    }
}
