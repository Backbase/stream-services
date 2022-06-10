package com.backbase.stream.configuration;

import com.backbase.dbs.contact.api.service.v2.ContactsApi;
import com.backbase.dbs.contact.api.service.ApiClient;
import com.backbase.dbs.user.api.service.v2.UserManagementApi;
import com.backbase.stream.config.BackbaseStreamConfigurationProperties;
import com.backbase.stream.contact.ContactsSaga;
import com.backbase.stream.contact.ContactsTask;
import com.backbase.stream.contact.ContactsUnitOfWorkExecutor;
import com.backbase.stream.contact.repository.ContactsUnitOfWorkRepository;
import com.backbase.stream.webclient.DbsWebClientConfiguration;
import com.backbase.stream.worker.repository.impl.InMemoryReactiveUnitOfWorkRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.client.WebClient;

import java.text.DateFormat;

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
        ApiClient apiClient = new ApiClient(dbsWebClient, objectMapper, dateFormat)
            .setBasePath(configurationProperties.getDbs().getContactManagerBaseUrl());
        return new ContactsApi(apiClient);
    }

    @Bean
    public UserManagementApi userManagementApi(
        ObjectMapper objectMapper,
        DateFormat dateFormat,
        WebClient dbsWebClient,
        BackbaseStreamConfigurationProperties configurationProperties
    ) {
        com.backbase.dbs.user.api.service.ApiClient apiClient = new com.backbase.dbs.user.api.service.ApiClient(
            dbsWebClient, objectMapper, dateFormat);
        apiClient.setBasePath(configurationProperties.getDbs().getUserManagerBaseUrl());
        return new UserManagementApi(apiClient);
    }

    @Bean
    public ContactsSaga contactsSaga(ContactsApi contactsApi) {
        return new ContactsSaga(contactsApi);
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
