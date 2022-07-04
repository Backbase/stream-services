package com.backbase.stream.contact;

import com.backbase.dbs.contact.api.service.v2.ContactsApi;
import com.backbase.dbs.contact.api.service.v2.model.AccessContextScope;
import com.backbase.dbs.contact.api.service.v2.model.ContactsBulkPostRequestBody;
import com.backbase.dbs.contact.api.service.v2.model.ExternalAccessContext;
import com.backbase.dbs.contact.api.service.v2.model.ExternalAccountInformation;
import com.backbase.dbs.contact.api.service.v2.model.ExternalContact;
import com.backbase.dbs.contact.api.service.v2.model.IngestMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactsSagaTest {

    @InjectMocks
    private ContactsSaga contactsSaga;

    @Mock
    private ContactsApi contactsApi;

    @Test
    void test_executeTask() {

        // Given
        ContactsTask contactsTask = createTask();
        when(contactsApi.postContactsBulk(any())).thenReturn(Mono.empty());

        // When
        Mono<ContactsTask> result = contactsSaga.executeTask(contactsTask);
        result.block();

        // Then
        verify(contactsApi).postContacts(any());
    }

    private ContactsTask createTask() {
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
        return new ContactsTask("1", request);
    }

}