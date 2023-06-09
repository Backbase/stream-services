package com.backbase.stream.clients.config;

import com.backbase.dbs.contact.api.service.ApiClient;
import com.backbase.dbs.contact.api.service.v2.ContactsApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("backbase.communication.services.contactmanager")
public class ContactManagerClientConfig extends CompositeApiClientConfig {

    public static final String CONTACT_MANAGER_SERVICE_ID = "contact-manager";

    public ContactManagerClientConfig() {
        super(CONTACT_MANAGER_SERVICE_ID);
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiClient contactManagerApiClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat).setBasePath(createBasePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public ContactsApi contactsApi(ApiClient contactManagerApiClient) {
        return new ContactsApi(contactManagerApiClient);
    }
}
