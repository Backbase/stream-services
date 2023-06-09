package com.backbase.stream.configuration;

import static org.mockito.Mockito.when;

import com.backbase.stream.contact.ContactsSaga;
import com.backbase.stream.contact.ContactsUnitOfWorkExecutor;
import com.backbase.stream.contact.repository.ContactsUnitOfWorkRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;

@ExtendWith(MockitoExtension.class)
@Import(ContactsServiceConfiguration.class)
class ContactsServiceConfigurationTest {

    ContactsServiceConfiguration configuration;
    @Mock
    private ContactsUnitOfWorkRepository repository;
    @Mock
    private ContactsSaga contactsSaga;

    @BeforeEach
    void beforeEach() {
        configuration = Mockito.spy(new ContactsServiceConfiguration());
    }

    @Test
    void test_contactsUnitOfWorkExecutor() {
        ContactsWorkerConfigurationProperties contactsWorkerConfigurationProperties =
            Mockito.mock(ContactsWorkerConfigurationProperties.class);
        when(contactsWorkerConfigurationProperties.getTaskExecutors()).thenReturn(1);

        ContactsUnitOfWorkExecutor workExecutor =
            configuration.contactsUnitOfWorkExecutor(
                repository, contactsSaga, contactsWorkerConfigurationProperties);
        Assertions.assertNotNull(workExecutor);
    }

    @Test
    void test_contactsUnitOfWorkRepository() {
        ContactsWorkerConfigurationProperties contactsWorkerConfigurationProperties =
            Mockito.mock(ContactsWorkerConfigurationProperties.class);
        when(contactsWorkerConfigurationProperties.getTaskExecutors()).thenReturn(1);

        ContactsUnitOfWorkExecutor workExecutor =
            configuration.contactsUnitOfWorkExecutor(
                repository, contactsSaga, contactsWorkerConfigurationProperties);
        Assertions.assertNotNull(workExecutor);
    }

    @Test
    void ContactsUnitOfWorkRepository() {
        ContactsUnitOfWorkRepository repo = configuration.contactsUnitOfWorkRepository();
        Assertions.assertNotNull(repo);
    }

    @Test
    void ContactsUnitOfWorkRepositoryProperties() {
        ContactsWorkerConfigurationProperties propertiesLocal =
            new ContactsWorkerConfigurationProperties();
        propertiesLocal.setContinueOnError(true);

        ContactsUnitOfWorkExecutor workExecutor =
            configuration.contactsUnitOfWorkExecutor(repository, contactsSaga, propertiesLocal);
        Assertions.assertNotNull(workExecutor);
    }
}
