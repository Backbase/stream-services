package com.backbase.stream.clients.config;

import com.backbase.dbs.user.api.service.ApiClient;
import com.backbase.dbs.user.api.service.v2.IdentityManagementApi;
import com.backbase.dbs.user.api.service.v2.UserManagementApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("backbase.communication.services.usermanager")
public class UserManagerClientConfig extends CompositeApiClientConfig {

  public static final String USER_MANAGER_SERVICE_ID = "user-manager";

  public UserManagerClientConfig() {
    super(USER_MANAGER_SERVICE_ID);
  }

  @Bean
  @ConditionalOnMissingBean
  public ApiClient userManagerClient(ObjectMapper objectMapper, DateFormat dateFormat) {
    return new ApiClient(getWebClient(), objectMapper, dateFormat).setBasePath(createBasePath());
  }

  @Bean
  @ConditionalOnMissingBean
  public UserManagementApi usersApi(ApiClient userManagerClient) {
    return new UserManagementApi(userManagerClient);
  }

  @Bean
  @ConditionalOnMissingBean
  public IdentityManagementApi identityManagementApi(ApiClient userManagerClient) {
    return new IdentityManagementApi(userManagerClient);
  }
}
