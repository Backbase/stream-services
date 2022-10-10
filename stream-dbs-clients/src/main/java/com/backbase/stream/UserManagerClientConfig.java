package com.backbase.stream;

import com.backbase.buildingblocks.webclient.client.ApiClientConfig;
import com.backbase.dbs.user.api.service.ApiClient;
import com.backbase.dbs.user.api.service.v2.IdentityManagementApi;
import com.backbase.dbs.user.api.service.v2.UserManagementApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("backbase.communication.services.usermanager")
public class UserManagerClientConfig extends ApiClientConfig {

    public static final String USER_MANAGER_SERVICE_ID = "user-manager";

    public UserManagerClientConfig() {
        super(USER_MANAGER_SERVICE_ID);
    }

    @Bean
    public ApiClient apiClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat)
            .setBasePath(createBasePath());
    }

    @Bean
    public UserManagementApi usersApi(ApiClient apiClient) {
        return new UserManagementApi(apiClient);
    }

    @Bean
    public IdentityManagementApi identityManagementApi(ApiClient apiClient) {
        return new IdentityManagementApi(apiClient);
    }

}
