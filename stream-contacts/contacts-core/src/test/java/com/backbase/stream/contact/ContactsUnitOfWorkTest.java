package com.backbase.stream.contact;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.dbs.contact.api.service.v2.model.AccessContextScope;
import com.backbase.dbs.contact.api.service.v2.model.ContactsBulkPostRequestBody;
import com.backbase.dbs.contact.api.service.v2.model.ExternalAccessContext;
import com.backbase.dbs.contact.api.service.v2.model.ExternalAccountInformation;
import com.backbase.dbs.contact.api.service.v2.model.ExternalContact;
import com.backbase.dbs.contact.api.service.v2.model.IngestMode;
import com.backbase.stream.configuration.ContactsServiceConfiguration;
import com.backbase.stream.configuration.ContactsWorkerConfigurationProperties;
import com.backbase.stream.contact.repository.ContactsUnitOfWorkRepository;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
@Import(ContactsServiceConfiguration.class)
class ContactsUnitOfWorkTest {

    @Mock
    ContactsWorkerConfigurationProperties streamWorkerConfiguration;
    private ContactsUnitOfWorkExecutor contactsUnitOfWorkExecutor;
    @Mock
    private ContactsUnitOfWorkRepository repository;
    @Mock
    private ContactsSaga streamTaskExecutor;

    @Test
    void test_executeTask() {
        when(streamWorkerConfiguration.getTaskExecutors()).thenReturn(1);
        this.contactsUnitOfWorkExecutor =
            new ContactsUnitOfWorkExecutor(repository, streamTaskExecutor, streamWorkerConfiguration);
        contactsUnitOfWorkExecutor.prepareUnitOfWork(
            Collections.singletonList(getContactsBulkPostRequestBody()));
        verify(streamWorkerConfiguration).getTaskExecutors();
    }

    @Test
    void test_executeTaskReturnResponse() {
        when(streamWorkerConfiguration.getTaskExecutors()).thenReturn(1);
        when(streamWorkerConfiguration.getBufferSize()).thenReturn(2);
        this.contactsUnitOfWorkExecutor =
            new ContactsUnitOfWorkExecutor(repository, streamTaskExecutor, streamWorkerConfiguration);

        Flux<ContactsBulkPostRequestBody> contactsBulkPostRequestBodyFlux =
            Flux.just(getContactsBulkPostRequestBody());
        contactsUnitOfWorkExecutor.prepareUnitOfWork(contactsBulkPostRequestBodyFlux);
        verify(streamWorkerConfiguration).getBufferSize();
    }

    private ContactsBulkPostRequestBody getContactsBulkPostRequestBody() {
        var request = new ContactsBulkPostRequestBody();
        request.setIngestMode(IngestMode.UPSERT);
        ExternalAccessContext accessContext = new ExternalAccessContext();
        accessContext.setScope(AccessContextScope.LE);
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
}
