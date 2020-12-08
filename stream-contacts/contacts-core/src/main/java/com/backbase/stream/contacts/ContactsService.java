package com.backbase.stream.contacts;

import com.backbase.dbs.contact.service.model.ContactsbulkingestionPostResponseBody;
import com.backbase.dbs.contact.service.model.ContactsbulkingestionRequest;
import com.backbase.stream.worker.model.UnitOfWork;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class ContactsService {

    private final ContactsUnitOfWorkExecutor contactsUnitOfWorkExecutor;
    private final ContactsSaga contactsSaga;

    public Mono<ContactsTask> ingestContacts(ContactsbulkingestionRequest request) {
        return contactsSaga.executeTask(new ContactsTask("adhoc", request));
    }

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
