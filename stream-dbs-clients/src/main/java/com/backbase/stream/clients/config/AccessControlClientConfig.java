package com.backbase.stream.clients.config;

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
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("backbase.communication.services.access-control")
public class AccessControlClientConfig extends DbsApiClientConfig {

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
    public UserQueryApi userQueryApi(ApiClient accessControlApiClient) {
        return new UserQueryApi(accessControlApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public UsersApi accessControlUsersApi(ApiClient accessControlApiClient) {
        return new UsersApi(accessControlApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public DataGroupApi dataGroupApi(ApiClient accessControlApiClient) {
        return new DataGroupApi(accessControlApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public DataGroupsApi dataGroupsApi(ApiClient accessControlApiClient) {
        return new DataGroupsApi(accessControlApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public FunctionGroupApi functionGroupApi(ApiClient accessControlApiClient) {
        return new FunctionGroupApi(accessControlApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public FunctionGroupsApi functionGroupsApi(ApiClient accessControlApiClient) {
        return new FunctionGroupsApi(accessControlApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public ServiceAgreementQueryApi serviceAgreementQueryApi(ApiClient accessControlApiClient) {
        return new ServiceAgreementQueryApi(accessControlApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public ServiceAgreementApi serviceAgreementApi(ApiClient accessControlApiClient) {
        return new ServiceAgreementApi(accessControlApiClient);
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
    public LegalEntityApi legalEntityApi(ApiClient accessControlApiClient) {
        return new LegalEntityApi(accessControlApiClient);
    }

}
