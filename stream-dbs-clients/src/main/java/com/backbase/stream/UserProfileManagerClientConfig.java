package com.backbase.stream;

import com.backbase.buildingblocks.webclient.client.ApiClientConfig;
import com.backbase.dbs.user.profile.api.service.ApiClient;
import com.backbase.dbs.user.profile.api.service.v2.UserProfileManagementApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("backbase.communication.services.user.profile")
public class UserProfileManagerClientConfig extends ApiClientConfig {

    public static final String USER_PROFILE_MANAGER_SERVICE_ID = "user-profile-manager";

    public UserProfileManagerClientConfig() {
        super(USER_PROFILE_MANAGER_SERVICE_ID);
    }

    @Bean
    public ApiClient apiClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat)
            .setBasePath(createBasePath());
    }

    @Bean
    public UserProfileManagementApi userProfileManagementApi(ApiClient apiClient) {
        return new UserProfileManagementApi(apiClient);
    }

}
