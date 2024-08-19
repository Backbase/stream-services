package com.backbase.stream.configuration;

import com.backbase.dbs.contact.api.service.v2.ContactsApi;
import com.backbase.stream.contact.ContactsSaga;
import com.backbase.stream.contact.ContactsTask;
import com.backbase.stream.contact.ContactsUnitOfWorkExecutor;
import com.backbase.stream.contact.repository.ContactsUnitOfWorkRepository;
import com.backbase.stream.worker.repository.impl.InMemoryReactiveUnitOfWorkRepository;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties({
    ContactsWorkerConfigurationProperties.class
})
@AllArgsConstructor
@Configuration
public class ContactsServiceConfiguration {

    @Bean
    public ContactsSaga contactsSaga(ContactsApi contactsApi,
        ContactsWorkerConfigurationProperties contactsWorkerConfigurationProperties) {
        return new ContactsSaga(contactsApi, contactsWorkerConfigurationProperties);
    }

    public static class InMemoryContactsUnitOfWorkRepository extends
        InMemoryReactiveUnitOfWorkRepository<ContactsTask> implements ContactsUnitOfWorkRepository {

    }

    @Bean
    @ConditionalOnProperty(name = "backbase.stream.persistence", havingValue = "memory", matchIfMissing = true)
    public ContactsUnitOfWorkRepository contactsUnitOfWorkRepository() {
        return new InMemoryContactsUnitOfWorkRepository();
    }

    @Bean
    public ContactsUnitOfWorkExecutor contactsUnitOfWorkExecutor(
        ContactsUnitOfWorkRepository repository, ContactsSaga saga,
        ContactsWorkerConfigurationProperties configurationProperties
    ) {
        return new ContactsUnitOfWorkExecutor(repository, saga, configurationProperties);
    }

}
