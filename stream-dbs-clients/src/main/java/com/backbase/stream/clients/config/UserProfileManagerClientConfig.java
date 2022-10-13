package com.backbase.stream.clients.config;

import com.backbase.dbs.user.profile.api.service.ApiClient;
import com.backbase.dbs.user.profile.api.service.v2.UserProfileManagementApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("backbase.communication.services.user.profile")
public class UserProfileManagerClientConfig extends DbsApiClientConfig {

    public static final String USER_PROFILE_MANAGER_SERVICE_ID = "user-profile-manager";

    public UserProfileManagerClientConfig() {
        super(USER_PROFILE_MANAGER_SERVICE_ID);
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiClient userProfileManagerClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat)
            .setBasePath(createBasePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public UserProfileManagementApi userProfileManagementApi(ApiClient userProfileManagerClient) {
        return new UserProfileManagementApi(userProfileManagerClient);
    }

}
