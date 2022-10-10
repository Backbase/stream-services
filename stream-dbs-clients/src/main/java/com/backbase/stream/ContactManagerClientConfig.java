package com.backbase.stream;

import com.backbase.buildingblocks.webclient.client.ApiClientConfig;
import com.backbase.dbs.contact.api.service.ApiClient;
import com.backbase.dbs.contact.api.service.v2.ContactsApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("backbase.communication.services.contactmanager")
public class ContactManagerClientConfig extends ApiClientConfig {

    public static final String CONTACT_MANAGER_SERVICE_ID = "contact-manager";

    public ContactManagerClientConfig() {
        super(CONTACT_MANAGER_SERVICE_ID);
    }

    @Bean
    public ApiClient apiClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat)
            .setBasePath(createBasePath());
    }

    @Bean
    public ContactsApi contactsApi(ApiClient apiClient) {
        return new ContactsApi(apiClient);
    }

}
