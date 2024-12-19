package com.backbase.stream.contact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.dbs.contact.api.service.v2.ContactsApi;
import com.backbase.dbs.contact.api.service.v2.model.AccessContextScope;
import com.backbase.dbs.contact.api.service.v2.model.ContactsBulkPostRequestBody;
import com.backbase.dbs.contact.api.service.v2.model.ContactsBulkPostResponseBody;
import com.backbase.dbs.contact.api.service.v2.model.ExternalAccessContext;
import com.backbase.dbs.contact.api.service.v2.model.ExternalAccountInformation;
import com.backbase.dbs.contact.api.service.v2.model.ExternalContact;
import com.backbase.dbs.contact.api.service.v2.model.IngestMode;
import com.backbase.stream.configuration.ContactsWorkerConfigurationProperties;
import com.backbase.stream.worker.exception.StreamTaskException;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ContactsSagaTest {

  @InjectMocks private ContactsSaga contactsSaga;

  @Mock private ContactsApi contactsApi;

  @Mock private ContactsWorkerConfigurationProperties contactsWorkerConfigurationProperties;

  @Test
  void test_executeTask() {
    // Given
    ContactsTask contactsTask = createTask();
    when(contactsApi.postContactsBulk(any())).thenReturn(Mono.empty());

    // When
    Mono<ContactsTask> result = contactsSaga.executeTask(contactsTask);
    result.block();

    // Then
    verify(contactsApi).postContactsBulk(any());
  }

  @Test
  void test_executeTaskContinueOnErrorTrue() {
    // Given
    ContactsTask contactsTask = createTask();
    when(contactsApi.postContactsBulk(any())).thenReturn(Mono.error(new Throwable()));

    // When
    ContactsWorkerConfigurationProperties props = new ContactsWorkerConfigurationProperties();
    props.setContinueOnError(true);
    when(contactsWorkerConfigurationProperties.isContinueOnError()).thenReturn(true);
    Mono<ContactsTask> result = contactsSaga.executeTask(contactsTask);
    result.block();

    // Then
    verify(contactsApi).postContactsBulk(any());
  }

  @Test
  void test_executeTaskContinueOnErrorFalse() {
    // Given
    ContactsTask contactsTask = createTask();
    when(contactsApi.postContactsBulk(any())).thenReturn(Mono.error(new Throwable()));

    // When
    ContactsWorkerConfigurationProperties props = new ContactsWorkerConfigurationProperties();
    props.setContinueOnError(true);
    when(contactsWorkerConfigurationProperties.isContinueOnError()).thenReturn(false);

    StreamTaskException exception =
        assertThrows(
            StreamTaskException.class,
            () -> {
              contactsSaga.executeTask(contactsTask).block();
            });
  }

  @Test
  void test_executeTaskReturnResponse() {
    // Given
    ContactsTask contactsTask = createTask();
    when(contactsApi.postContactsBulk(any())).thenReturn(Mono.just(getMockResponse()));

    // When
    ContactsTask result = contactsSaga.executeTask(contactsTask).block();

    // Then
    assertEquals(2, result.getResponse().getSuccessCount());
    assertEquals("contact", result.getName());
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

  private ContactsBulkPostResponseBody getMockResponse() {
    ContactsBulkPostResponseBody responseBody = new ContactsBulkPostResponseBody();
    responseBody.setSuccessCount(2);
    return responseBody;
  }
}
