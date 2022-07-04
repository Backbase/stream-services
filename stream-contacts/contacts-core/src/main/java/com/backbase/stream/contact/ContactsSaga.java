package com.backbase.stream.contact;

import com.backbase.dbs.contact.api.service.v2.ContactsApi;
import com.backbase.dbs.contact.api.service.v2.model.ContactsBulkPostRequestBody;
import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.exception.StreamTaskException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class ContactsSaga implements StreamTaskExecutor<ContactsTask> {

    public static final String ENTITY = "contact";
    public static final String CREATE = "create";
    public static final String SUCCESS = "success";
    public static final String ERROR = "error";
    public static final String CREATED_SUCCESSFULLY = "Contact created successfully";
    public static final String FAILED_TO_INGEST_CONTACTS = "Failed to ingest contacts";
    private final ContactsApi contactsApi;

    @Override
    public Mono<ContactsTask> executeTask(ContactsTask contactsTask) {
        ContactsBulkPostRequestBody item = contactsTask.getData();

        log.info("Started ingestion of contacts for Type {}", item.getAccessContext().getScope());
        return contactsApi.postContactsBulk(item)
                .map(contactsBulkPostResponse -> {
                    contactsTask.setResponse(contactsBulkPostResponse);
                    contactsTask.info(ENTITY, CREATE, SUCCESS, item.getAccessContext().getExternalUserId(), null, CREATED_SUCCESSFULLY);
                    return contactsTask;
                })
                .onErrorResume(throwable -> {
                    contactsTask.error(ENTITY, CREATE, ERROR, item.getAccessContext().getExternalUserId(), null, throwable, "Failed to ingest contact " + throwable.getMessage(), FAILED_TO_INGEST_CONTACTS);
                    return Mono.error(new StreamTaskException(contactsTask, throwable, FAILED_TO_INGEST_CONTACTS));
                });

    }

    @Override
    public Mono<ContactsTask> rollBack(ContactsTask contactsTask) {
        return null;
    }
}
