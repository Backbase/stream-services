package com.backbase.stream.contact;

import com.backbase.dbs.contact.api.service.v2.model.AccessContextScope;
import com.backbase.dbs.contact.api.service.v2.model.ContactsBulkPostRequestBody;
import com.backbase.dbs.contact.api.service.v2.model.ContactsBulkPostResponseBody;
import com.backbase.dbs.contact.api.service.v2.model.ExternalAccessContext;
import com.backbase.dbs.contact.api.service.v2.model.ExternalAccountInformation;
import com.backbase.dbs.contact.api.service.v2.model.ExternalContact;
import com.backbase.dbs.contact.api.service.v2.model.IngestMode;
import com.backbase.stream.worker.model.UnitOfWork;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactsServiceTest {

    @InjectMocks
    private ContactsService contactsService;

    @Mock
    private ContactsUnitOfWorkExecutor contactsUnitOfWorkExecutor;

    @Test
    void test_createBulkContacts() {
        List<ContactsTask> streamTasks = new ArrayList<ContactsTask>();
        ContactsTask task = new ContactsTask("1", getMockContactsBulkRequest());
        task.setResponse(getMockResponse());
        streamTasks.add(task);
        UnitOfWork<ContactsTask> unitOfWork = new UnitOfWork<>();
        unitOfWork.setStreamTasks(streamTasks);

        when(contactsUnitOfWorkExecutor.prepareUnitOfWork(any(Flux.class))).thenReturn(Flux.just(unitOfWork));

        Flux<ContactsBulkPostResponseBody> response = contactsService.createBulkContacts(Flux.just(getMockContactsBulkRequest()));
        verify(contactsUnitOfWorkExecutor).prepareUnitOfWork(any(Flux.class));
    }

    private ContactsBulkPostRequestBody getMockContactsBulkRequest() {
        var request = new ContactsBulkPostRequestBody();
        request.setIngestMode(IngestMode.UPSERT);

        ExternalAccessContext accessContext = new ExternalAccessContext();
        accessContext.setScope(AccessContextScope.LE);
        accessContext.setExternalUserId("USER1");
        request.setAccessContext(accessContext);

        ExternalContact contact = new ExternalContact();
        contact.setName("TEST1");
        contact.setExternalId("TEST101");

        ExternalAccountInformation account = new ExternalAccountInformation();
        account.setName("TESTACC1");
        account.setExternalId("TESTACC101");
        contact.setAccounts(Collections.singletonList(account));
        request.setContacts(Collections.singletonList(contact));

        return request;
    }

    private ContactsBulkPostResponseBody getMockResponse() {
        ContactsBulkPostResponseBody responseBody = new ContactsBulkPostResponseBody();
        responseBody.setSuccessCount(2);
        return responseBody;

    }

}
