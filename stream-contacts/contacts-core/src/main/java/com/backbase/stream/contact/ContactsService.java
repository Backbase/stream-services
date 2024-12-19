package com.backbase.stream.contact;

import com.backbase.dbs.contact.api.service.v2.model.ContactsBulkPostRequestBody;
import com.backbase.dbs.contact.api.service.v2.model.ContactsBulkPostResponseBody;
import com.backbase.stream.worker.model.UnitOfWork;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
public class ContactsService {

  private final ContactsUnitOfWorkExecutor contactsUnitOfWorkExecutor;

  public ContactsService(ContactsUnitOfWorkExecutor contactsUnitOfWorkExecutor) {
    this.contactsUnitOfWorkExecutor = contactsUnitOfWorkExecutor;
  }

  public Flux<ContactsBulkPostResponseBody> createBulkContacts(
      Flux<ContactsBulkPostRequestBody> items) {
    Flux<ContactsBulkPostRequestBody> cleanItems =
        items.map(
            item -> {
              String userId = item.getAccessContext().getExternalUserId();
              log.info("User id {}", userId);
              return item;
            });
    Flux<UnitOfWork<ContactsTask>> unitOfWorkFlux =
        contactsUnitOfWorkExecutor.prepareUnitOfWork(cleanItems);

    return unitOfWorkFlux
        .flatMap(contactsUnitOfWorkExecutor::executeUnitOfWork)
        .flatMap(
            contactsTaskUnitOfWork -> {
              Stream<ContactsBulkPostResponseBody> stream =
                  contactsTaskUnitOfWork.getStreamTasks().stream().map(ContactsTask::getResponse);
              return Flux.fromStream(stream);
            });
  }
}
