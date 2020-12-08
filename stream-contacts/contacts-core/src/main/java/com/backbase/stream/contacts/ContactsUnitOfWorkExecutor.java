package com.backbase.stream.contacts;

import com.backbase.dbs.contact.service.model.ContactsbulkingestionRequest;
import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.UnitOfWorkExecutor;
import com.backbase.stream.worker.configuration.StreamWorkerConfiguration;
import com.backbase.stream.worker.model.UnitOfWork;
import com.backbase.stream.worker.repository.UnitOfWorkRepository;
import java.util.List;
import reactor.core.publisher.Flux;

public class ContactsUnitOfWorkExecutor extends UnitOfWorkExecutor<ContactsTask> {

    public ContactsUnitOfWorkExecutor(UnitOfWorkRepository<ContactsTask, String> repository, StreamTaskExecutor<ContactsTask> streamTaskExecutor, StreamWorkerConfiguration streamWorkerConfiguration) {
        super(repository, streamTaskExecutor, streamWorkerConfiguration);
    }

    public Flux<UnitOfWork<ContactsTask>> prepareUnitOfWork(List<ContactsbulkingestionRequest> items) {
        String unitOfWorkId = "contacts-" + System.currentTimeMillis();
        Flux<UnitOfWork<ContactsTask>> toWorkOn = Flux.empty();
        items.forEach(item -> {
            ContactsTask task = new ContactsTask(unitOfWorkId + "-" + item.getAccessContext().getExternalLegalEntityId(), item);
            Flux<UnitOfWork<ContactsTask>> just = Flux.just(UnitOfWork.from(unitOfWorkId, task));
            toWorkOn.mergeWith(just);
        });
        return toWorkOn;
    }

    public Flux<UnitOfWork<ContactsTask>> prepareUnitOfWork(Flux<ContactsbulkingestionRequest> items) {
        return items.buffer(streamWorkerConfiguration.getBufferSize()).flatMap(this::prepareUnitOfWork);
    }
}
