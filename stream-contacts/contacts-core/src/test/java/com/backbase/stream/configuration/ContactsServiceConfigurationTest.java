package com.backbase.stream.configuration;

import com.backbase.dbs.contact.api.service.v2.ContactsApi;
import com.backbase.stream.config.BackbaseStreamConfigurationProperties;
import com.backbase.stream.config.BackbaseStreamConfigurationProperties.DbsConnectionProperties;
import com.backbase.stream.contact.ContactsSaga;
import com.backbase.stream.contact.ContactsUnitOfWorkExecutor;
import com.backbase.stream.contact.repository.ContactsUnitOfWorkRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.client.WebClient;

import java.text.DateFormat;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Import(ContactsServiceConfiguration.class)
class ContactsServiceConfigurationTest {

    @Mock
    private ContactsUnitOfWorkRepository repository;

    @Mock
    private ContactsSaga contactsSaga;

    BackbaseStreamConfigurationProperties properties;
    ContactsServiceConfiguration configuration;

    @BeforeEach
    void beforeEach() {
        properties = Mockito.mock(BackbaseStreamConfigurationProperties.class);
        configuration = Mockito.spy(new ContactsServiceConfiguration(properties));
    }

    @Test
    void test_contactsApi() {
        String contactBaseUrl = "http://contact";
        WebClient dbsWebClient = Mockito.mock(WebClient.class);
        ObjectMapper objectMapper = Mockito.mock(ObjectMapper.class);
        DateFormat dateFormat = Mockito.mock(DateFormat.class);
        DbsConnectionProperties dbsConnectionProperties = Mockito.mock(DbsConnectionProperties.class);

        Mockito.when(properties.getDbs()).thenReturn(dbsConnectionProperties);
        Mockito.when(dbsConnectionProperties.getContactManagerBaseUrl()).thenReturn(contactBaseUrl);

        ContactsApi contactsApi = configuration.contactsApi(dbsWebClient, objectMapper, dateFormat);
        Assertions.assertNotNull(contactsApi);

        ContactsSaga contactsSaga2 = configuration.contactsSaga(contactsApi);
        Assertions.assertNotNull(contactsSaga2);
    }

    @Test
     void test_contactsUnitOfWorkExecutor() {
        ContactsWorkerConfigurationProperties contactsWorkerConfigurationProperties = Mockito.mock(ContactsWorkerConfigurationProperties.class);
        when(contactsWorkerConfigurationProperties.getTaskExecutors()).thenReturn(1);

        ContactsUnitOfWorkExecutor workExecutor = configuration.contactsUnitOfWorkExecutor(repository, contactsSaga, contactsWorkerConfigurationProperties);
        Assertions.assertNotNull(workExecutor);

    }

    @Test
    void test_contactsUnitOfWorkRepository() {
        ContactsWorkerConfigurationProperties contactsWorkerConfigurationProperties = Mockito.mock(ContactsWorkerConfigurationProperties.class);
        when(contactsWorkerConfigurationProperties.getTaskExecutors()).thenReturn(1);

        ContactsUnitOfWorkExecutor workExecutor = configuration.contactsUnitOfWorkExecutor(repository, contactsSaga, contactsWorkerConfigurationProperties);
        Assertions.assertNotNull(workExecutor);
    }

    @Test
    void ContactsUnitOfWorkRepository () {
        ContactsUnitOfWorkRepository repo = configuration.contactsUnitOfWorkRepository();
        Assertions.assertNotNull(repo);
    }

    @Test
    void ContactsUnitOfWorkRepositoryProperties () {
        ContactsWorkerConfigurationProperties propertiesLocal = new ContactsWorkerConfigurationProperties();
        propertiesLocal.setContinueOnError(true);

        ContactsUnitOfWorkExecutor workExecutor = configuration.contactsUnitOfWorkExecutor(repository, contactsSaga, propertiesLocal);
        Assertions.assertNotNull(workExecutor);


    }

}