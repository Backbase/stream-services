package com.backbase.stream.contacts.configuration;

import com.backbase.dbs.contact.service.ApiClient;
import com.backbase.dbs.contact.service.api.ContactsApi;
import com.backbase.stream.config.BackbaseStreamConfigurationProperties;
import com.backbase.stream.contacts.ContactsSaga;
import com.backbase.stream.contacts.ContactsTask;
import com.backbase.stream.contacts.ContactsUnitOfWorkExecutor;
import com.backbase.stream.contacts.repository.ContactsUnitOfWorkRepository;
import com.backbase.stream.webclient.DbsWebClientConfiguration;
import com.backbase.stream.worker.repository.impl.InMemoryReactiveUnitOfWorkRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.client.WebClient;

@EnableConfigurationProperties({
    BackbaseStreamConfigurationProperties.class,
    ContactsWorkerConfigurationProperties.class
})
@AllArgsConstructor
@Configuration
@Import({DbsWebClientConfiguration.class})
public class ContactsServiceConfiguration {

    @Bean
    public ContactsApi contactsApi(
        ObjectMapper objectMapper,
        DateFormat dateFormat,
        WebClient dbsWebClient,
        BackbaseStreamConfigurationProperties configurationProperties
    ) {
        ApiClient apiClient = new ApiClient(dbsWebClient, objectMapper, dateFormat).setBasePath(configurationProperties.getDbs().getLimitsManagerBaseUrl());
        return new ContactsApi(apiClient);
    }

    @Bean
    public ContactsSaga contactsSaga(ContactsApi contactsApi) {
        return new ContactsSaga(contactsApi);
    }

    public static class InMemoryContactsUnitOfWorkRepository extends InMemoryReactiveUnitOfWorkRepository<ContactsTask> implements ContactsUnitOfWorkRepository {
    }

    @Bean
    @ConditionalOnProperty(name = "backbase.stream.persistence", havingValue = "memory", matchIfMissing = true)
    public ContactsUnitOfWorkRepository limitsUnitOfWorkRepository() {
        return new InMemoryContactsUnitOfWorkRepository();
    }

    @Bean
    public ContactsUnitOfWorkExecutor limitsUnitOfWorkExecutor(
        ContactsUnitOfWorkRepository repository, ContactsSaga saga, ContactsWorkerConfigurationProperties configurationProperties
    ) {
        return new ContactsUnitOfWorkExecutor(repository, saga, configurationProperties);
    }

}
