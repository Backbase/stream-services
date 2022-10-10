package com.backbase.stream;

import com.backbase.buildingblocks.webclient.client.ApiClientConfig;
import com.backbase.dbs.accesscontrol.api.service.ApiClient;
import com.backbase.dbs.accesscontrol.api.service.v2.DataGroupApi;
import com.backbase.dbs.accesscontrol.api.service.v2.DataGroupsApi;
import com.backbase.dbs.accesscontrol.api.service.v2.FunctionGroupApi;
import com.backbase.dbs.accesscontrol.api.service.v2.FunctionGroupsApi;
import com.backbase.dbs.accesscontrol.api.service.v2.LegalEntitiesApi;
import com.backbase.dbs.accesscontrol.api.service.v2.LegalEntityApi;
import com.backbase.dbs.accesscontrol.api.service.v2.ServiceAgreementApi;
import com.backbase.dbs.accesscontrol.api.service.v2.ServiceAgreementQueryApi;
import com.backbase.dbs.accesscontrol.api.service.v2.ServiceAgreementsApi;
import com.backbase.dbs.accesscontrol.api.service.v2.UserQueryApi;
import com.backbase.dbs.accesscontrol.api.service.v2.UsersApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("backbase.communication.services.access-control")
public class AccessControlClientConfig extends ApiClientConfig {

    public static final String ACCESS_CONTROL_SERVICE_ID = "access-control";

    public AccessControlClientConfig() {
        super(ACCESS_CONTROL_SERVICE_ID);
    }

    @Bean
    public ApiClient apiClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat)
            .setBasePath(createBasePath());
    }

    @Bean
    public UserQueryApi userQueryApi(ApiClient apiClient) {
        return new UserQueryApi(apiClient);
    }

    @Bean
    public UsersApi accessControlUsersApi(ApiClient apiClient) {
        return new UsersApi(apiClient);
    }

    @Bean
    public DataGroupApi dataGroupApi(ApiClient apiClient) {
        return new DataGroupApi(apiClient);
    }

    @Bean
    public DataGroupsApi dataGroupsApi(ApiClient apiClient) {
        return new DataGroupsApi(apiClient);
    }

    @Bean
    public FunctionGroupApi functionGroupApi(ApiClient apiClient) {
        return new FunctionGroupApi(apiClient);
    }

    @Bean
    public FunctionGroupsApi functionGroupsApi(ApiClient apiClient) {
        return new FunctionGroupsApi(apiClient);
    }

    @Bean
    public ServiceAgreementQueryApi serviceAgreementQueryApi(ApiClient apiClient) {
        return new ServiceAgreementQueryApi(apiClient);
    }

    @Bean
    public ServiceAgreementApi serviceAgreementApi(ApiClient apiClient) {
        return new ServiceAgreementApi(apiClient);
    }

    @Bean
    public ServiceAgreementsApi serviceAgreementsApi(ApiClient apiClient) {
        return new ServiceAgreementsApi(apiClient);
    }

    @Bean
    public LegalEntitiesApi legalEntitiesApi(ApiClient apiClient) {
        return new LegalEntitiesApi(apiClient);
    }

    @Bean
    public LegalEntityApi legalEntityApi(ApiClient apiClient) {
        return new LegalEntityApi(apiClient);
    }

}
