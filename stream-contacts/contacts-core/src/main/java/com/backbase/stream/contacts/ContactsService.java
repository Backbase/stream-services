package com.backbase.stream.contacts;

import com.backbase.dbs.contact.service.model.ContactsbulkingestionPostResponseBody;
import com.backbase.dbs.contact.service.model.ContactsbulkingestionRequest;
import com.backbase.stream.worker.model.UnitOfWork;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;

public class ContactsService {

    private final ContactsUnitOfWorkExecutor contactsUnitOfWorkExecutor;

    public ContactsService(ContactsUnitOfWorkExecutor contactsUnitOfWorkExecutor) {
        this.contactsUnitOfWorkExecutor = contactsUnitOfWorkExecutor;
    }

    /**
     * This is very very wrong.
     */
    public Flux<ContactsbulkingestionPostResponseBody> ingestContacts(Flux<ContactsbulkingestionRequest> items) {
        return contactsUnitOfWorkExecutor.prepareUnitOfWork(items)
            .flatMap(contactsUnitOfWorkExecutor::executeUnitOfWork)
            .flatMap(this::mapResultFromUnitOfWork);
    }

    @NotNull
    private Flux<ContactsbulkingestionPostResponseBody> mapResultFromUnitOfWork(UnitOfWork<ContactsTask> limitsTaskUnitOfWork) {
        return Flux.fromStream(limitsTaskUnitOfWork.getStreamTasks().stream().map(ContactsTask::getResponse));
    }

}
