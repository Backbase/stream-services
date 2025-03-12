package com.backbase.stream.clients.config;

import com.backbase.accesscontrol.customeraccessgroup.api.service.v1.CustomerAccessGroupApi;
import com.backbase.dbs.accesscontrol.api.service.ApiClient;
import com.backbase.dbs.accesscontrol.api.service.v3.DataGroupsApi;
import com.backbase.dbs.accesscontrol.api.service.v3.FunctionGroupsApi;
import com.backbase.dbs.accesscontrol.api.service.v3.LegalEntitiesApi;
import com.backbase.dbs.accesscontrol.api.service.v3.ServiceAgreementsApi;
import com.backbase.dbs.accesscontrol.api.service.v3.UserContextApi;
import com.backbase.dbs.accesscontrol.api.service.v3.UsersApi;
import com.backbase.dbs.accesscontrol.api.service.v3.PermissionSetApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("backbase.communication.services.access-control")
public class AccessControlClientConfig extends CompositeApiClientConfig {

    public static final String ACCESS_CONTROL_SERVICE_ID = "access-control";

    public AccessControlClientConfig() {
        super(ACCESS_CONTROL_SERVICE_ID);
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiClient accessControlApiClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat)
            .setBasePath(createBasePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public UsersApi accessControlUsersApi(ApiClient accessControlApiClient) {
        return new UsersApi(accessControlApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public DataGroupsApi dataGroupsApi(ApiClient accessControlApiClient) {
        return new DataGroupsApi(accessControlApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public FunctionGroupsApi functionGroupsApi(ApiClient accessControlApiClient) {
        return new FunctionGroupsApi(accessControlApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public ServiceAgreementsApi serviceAgreementsApi(ApiClient accessControlApiClient) {
        return new ServiceAgreementsApi(accessControlApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public LegalEntitiesApi legalEntitiesApi(ApiClient accessControlApiClient) {
        return new LegalEntitiesApi(accessControlApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public UserContextApi userContextApi(ApiClient accessControlApiClient) {
        return new UserContextApi(accessControlApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public PermissionSetApi permissionSetApi(ApiClient accessControlApiClient) {
        return new PermissionSetApi(accessControlApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public CustomerAccessGroupApi customerAccessGroupApi(com.backbase.accesscontrol.customeraccessgroup.api.service.ApiClient accessControlApiClient) {
        return new CustomerAccessGroupApi(accessControlApiClient);
    }

}
