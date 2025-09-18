package com.backbase.stream.clients.config;

import com.backbase.accesscontrol.assignpermissions.api.service.v1.AssignPermissionsApi;
import com.backbase.accesscontrol.customeraccessgroup.api.service.v1.CustomerAccessGroupApi;
import com.backbase.accesscontrol.permissionset.api.service.v1.PermissionSetApi;
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
    public com.backbase.accesscontrol.assignpermissions.api.service.ApiClient assignPermissionsServiceApiClient(
        ObjectMapper objectMapper, DateFormat dateFormat) {
        return new com.backbase.accesscontrol.assignpermissions.api.service.ApiClient(getWebClient(), objectMapper,
            dateFormat).setBasePath(createBasePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public com.backbase.accesscontrol.customeraccessgroup.api.service.ApiClient customerAccessGroupServiceApiClient(
        ObjectMapper objectMapper, DateFormat dateFormat) {
        return new com.backbase.accesscontrol.customeraccessgroup.api.service.ApiClient(getWebClient(), objectMapper,
            dateFormat).setBasePath(createBasePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public com.backbase.accesscontrol.datagroup.api.service.ApiClient dataGroupServiceApiClient(
        ObjectMapper objectMapper, DateFormat dateFormat) {
        return new com.backbase.accesscontrol.datagroup.api.service.ApiClient(getWebClient(), objectMapper, dateFormat)
            .setBasePath(createBasePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public com.backbase.accesscontrol.datagroup.api.integration.ApiClient dataGroupIntegrationApiClient(
        ObjectMapper objectMapper, DateFormat dateFormat) {
        return new com.backbase.accesscontrol.datagroup.api.integration.ApiClient(getWebClient(), objectMapper,
            dateFormat).setBasePath(createBasePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public com.backbase.accesscontrol.functiongroup.api.service.ApiClient functionGroupServiceApiClient(
        ObjectMapper objectMapper, DateFormat dateFormat) {
        return new com.backbase.accesscontrol.functiongroup.api.service.ApiClient(getWebClient(), objectMapper,
            dateFormat).setBasePath(createBasePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public com.backbase.accesscontrol.functiongroup.api.integration.ApiClient functionGroupIntegrationApiClient(
        ObjectMapper objectMapper, DateFormat dateFormat) {
        return new com.backbase.accesscontrol.functiongroup.api.integration.ApiClient(getWebClient(), objectMapper,
            dateFormat).setBasePath(createBasePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public com.backbase.accesscontrol.legalentity.api.service.ApiClient legalEntityServiceApiClient(
        ObjectMapper objectMapper, DateFormat dateFormat) {
        return new com.backbase.accesscontrol.legalentity.api.service.ApiClient(getWebClient(), objectMapper,
            dateFormat).setBasePath(createBasePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public com.backbase.accesscontrol.legalentity.api.integration.ApiClient legalEntityIntegrationApiClient(
        ObjectMapper objectMapper, DateFormat dateFormat) {
        return new com.backbase.accesscontrol.legalentity.api.integration.ApiClient(getWebClient(), objectMapper,
            dateFormat).setBasePath(createBasePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public com.backbase.accesscontrol.permissioncheck.api.service.ApiClient permissionCheckServiceApiClient(
        ObjectMapper objectMapper, DateFormat dateFormat) {
        return new com.backbase.accesscontrol.permissioncheck.api.service.ApiClient(getWebClient(), objectMapper,
            dateFormat).setBasePath(createBasePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public com.backbase.accesscontrol.serviceagreement.api.service.ApiClient serviceAgreementServiceApiClient(
        ObjectMapper objectMapper, DateFormat dateFormat) {
        return new com.backbase.accesscontrol.serviceagreement.api.service.ApiClient(getWebClient(), objectMapper,
            dateFormat).setBasePath(createBasePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public com.backbase.accesscontrol.serviceagreement.api.integration.ApiClient serviceAgreementIntegrationApiClient(
        ObjectMapper objectMapper, DateFormat dateFormat) {
        return new com.backbase.accesscontrol.serviceagreement.api.integration.ApiClient(getWebClient(), objectMapper,
            dateFormat).setBasePath(createBasePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public com.backbase.accesscontrol.usercontext.api.service.ApiClient userContextServiceApiClient(
        ObjectMapper objectMapper, DateFormat dateFormat) {
        return new com.backbase.accesscontrol.usercontext.api.service.ApiClient(getWebClient(), objectMapper,
            dateFormat).setBasePath(createBasePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public AssignPermissionsApi assignPermissionsApi(
        com.backbase.accesscontrol.assignpermissions.api.service.ApiClient apiClient) {
        return new AssignPermissionsApi(apiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public CustomerAccessGroupApi customerAccessGroupApi(
        com.backbase.accesscontrol.customeraccessgroup.api.service.ApiClient apiClient) {
        return new CustomerAccessGroupApi(apiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public com.backbase.accesscontrol.datagroup.api.service.v1.DataGroupApi dataGroupServiceApi(
        com.backbase.accesscontrol.datagroup.api.service.ApiClient apiClient) {
        return new com.backbase.accesscontrol.datagroup.api.service.v1.DataGroupApi(apiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public com.backbase.accesscontrol.datagroup.api.integration.v1.DataGroupApi dataGroupIntegrationApi(
        com.backbase.accesscontrol.datagroup.api.integration.ApiClient apiClient) {
        return new com.backbase.accesscontrol.datagroup.api.integration.v1.DataGroupApi(apiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public com.backbase.accesscontrol.functiongroup.api.service.v1.FunctionGroupApi functionGroupServiceApi(
        com.backbase.accesscontrol.functiongroup.api.service.ApiClient apiClient) {
        return new com.backbase.accesscontrol.functiongroup.api.service.v1.FunctionGroupApi(apiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public com.backbase.accesscontrol.functiongroup.api.integration.v1.FunctionGroupApi functionGroupIntegrationApi(
        com.backbase.accesscontrol.functiongroup.api.integration.ApiClient apiClient) {
        return new com.backbase.accesscontrol.functiongroup.api.integration.v1.FunctionGroupApi(apiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public com.backbase.accesscontrol.legalentity.api.service.v1.LegalEntityApi legalEntityServiceApi(
        com.backbase.accesscontrol.legalentity.api.service.ApiClient apiClient) {
        return new com.backbase.accesscontrol.legalentity.api.service.v1.LegalEntityApi(apiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public com.backbase.accesscontrol.legalentity.api.integration.v3.LegalEntityApi legalEntityIntegrationApi(
        com.backbase.accesscontrol.legalentity.api.integration.ApiClient apiClient) {
        return new com.backbase.accesscontrol.legalentity.api.integration.v3.LegalEntityApi(apiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public com.backbase.accesscontrol.permissioncheck.api.service.v1.PermissionCheckApi permissionCheckServiceApi(
        com.backbase.accesscontrol.permissioncheck.api.service.ApiClient apiClient) {
        return new com.backbase.accesscontrol.permissioncheck.api.service.v1.PermissionCheckApi(apiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public com.backbase.accesscontrol.serviceagreement.api.service.v1.ServiceAgreementApi serviceAgreementServiceApi(
        com.backbase.accesscontrol.serviceagreement.api.service.ApiClient apiClient) {
        return new com.backbase.accesscontrol.serviceagreement.api.service.v1.ServiceAgreementApi(apiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public com.backbase.accesscontrol.serviceagreement.api.integration.v1.ServiceAgreementApi serviceAgreementIntegrationApi(
        com.backbase.accesscontrol.serviceagreement.api.integration.ApiClient apiClient) {
        return new com.backbase.accesscontrol.serviceagreement.api.integration.v1.ServiceAgreementApi(apiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public com.backbase.accesscontrol.usercontext.api.service.v1.UserContextApi userContextServiceApi(
        com.backbase.accesscontrol.usercontext.api.service.ApiClient apiClient) {
        return new com.backbase.accesscontrol.usercontext.api.service.v1.UserContextApi(apiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public com.backbase.accesscontrol.permissionset.api.service.ApiClient permissionSetServiceApiClient(
        ObjectMapper objectMapper, DateFormat dateFormat) {
        return new com.backbase.accesscontrol.permissionset.api.service.ApiClient(getWebClient(), objectMapper,
            dateFormat).setBasePath(createBasePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public PermissionSetApi permissionSetPermissionsApi(
        com.backbase.accesscontrol.permissionset.api.service.ApiClient apiClient) {
        return new PermissionSetApi(apiClient);
    }
}
