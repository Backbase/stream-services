package com.backbase.stream;


import com.backbase.dbs.contact.api.service.v2.model.ContactsBulkPostRequestBody;
import com.backbase.dbs.contact.api.service.v2.model.ContactsBulkPostResponseBody;
import com.backbase.stream.contact.ContactsTask;
import com.backbase.stream.contact.ContactsUnitOfWorkExecutor;
import com.backbase.stream.worker.model.UnitOfWork;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.stream.Stream;

@Slf4j
public class ContactsService {

    private ContactsUnitOfWorkExecutor contactsUnitOfWorkExecutor;

    public ContactsService(ContactsUnitOfWorkExecutor contactsUnitOfWorkExecutor) {
        this.contactsUnitOfWorkExecutor = contactsUnitOfWorkExecutor;
    }

    public Flux<ContactsBulkPostResponseBody> createBulkContacts(Flux<ContactsBulkPostRequestBody> items) {
        Flux<ContactsBulkPostRequestBody> cleanItems = items.map(item -> {
            String userId = item.getAccessContext().getExternalUserId();
            log.info("User id {}", userId);
            return item;
        });
        Flux<UnitOfWork<ContactsTask>> unitOfWorkFlux = contactsUnitOfWorkExecutor.prepareUnitOfWork(cleanItems);

        return unitOfWorkFlux.flatMap(contactsUnitOfWorkExecutor::executeUnitOfWork)
                .flatMap(contactsTaskUnitOfWork -> {
            Stream<ContactsBulkPostResponseBody> stream = contactsTaskUnitOfWork.getStreamTasks().stream()
                .map(ContactsTask::getResponse);
            return Flux.fromStream(stream);
        });
    }

}
