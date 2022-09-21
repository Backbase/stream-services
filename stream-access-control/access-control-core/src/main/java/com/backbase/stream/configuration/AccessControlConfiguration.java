package com.backbase.stream.configuration;

import com.backbase.buildingblocks.webclient.WebClientConstants;
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
import com.backbase.dbs.user.api.service.v2.IdentityManagementApi;
import com.backbase.dbs.user.api.service.v2.UserManagementApi;
import com.backbase.dbs.user.profile.api.service.v2.UserProfileManagementApi;
import com.backbase.identity.integration.api.service.v1.IdentityIntegrationServiceApi;
import com.backbase.stream.config.BackbaseStreamConfigurationProperties;
import com.backbase.stream.product.configuration.ProductConfiguration;
import com.backbase.stream.product.service.ArrangementService;
import com.backbase.stream.service.AccessGroupService;
import com.backbase.stream.service.EntitlementsService;
import com.backbase.stream.service.LegalEntityService;
import com.backbase.stream.service.UserProfileService;
import com.backbase.stream.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Access Control Configuration.
 */
@Configuration
@EnableConfigurationProperties(BackbaseStreamConfigurationProperties.class)
@Import(ProductConfiguration.class)
@Slf4j
public class AccessControlConfiguration {

    private final BackbaseStreamConfigurationProperties backbaseStreamConfigurationProperties;

    public AccessControlConfiguration(BackbaseStreamConfigurationProperties backbaseStreamConfigurationProperties) {
        this.backbaseStreamConfigurationProperties = backbaseStreamConfigurationProperties;
    }

    @Bean
    public EntitlementsService entitlementsService(ArrangementService arrangementService,
        AccessGroupService accessGroupService,
        LegalEntityService legalEntityService,
        UserService userService) {

        return new EntitlementsService(arrangementService, userService, accessGroupService, legalEntityService);
    }

    @Bean
    public LegalEntityService legalEntityService(
        com.backbase.dbs.accesscontrol.api.service.ApiClient legalEntityApiClient) {
        LegalEntitiesApi legalEntitiesApi = new LegalEntitiesApi(legalEntityApiClient);
        LegalEntityApi legalEntityApi = new LegalEntityApi(legalEntityApiClient);
        return new LegalEntityService(legalEntitiesApi, legalEntityApi);
    }

    @Bean
    public UserService userService(com.backbase.dbs.user.api.service.ApiClient usersApiClient,
                                   Optional<com.backbase.identity.integration.api.service.ApiClient>
                                       identityApiClient) {
        UserManagementApi usersApi = new UserManagementApi(usersApiClient);
        IdentityManagementApi identityManagementApi = new IdentityManagementApi(usersApiClient);
        return new UserService(usersApi, identityManagementApi,
            identityApiClient.map(IdentityIntegrationServiceApi::new));
    }

    @Bean
    public UserProfileService userProfileService(com.backbase.dbs.user.profile.api.service.ApiClient usersApiClient) {
        return new UserProfileService(new UserProfileManagementApi(usersApiClient));
    }

    @Bean
    public AccessGroupService accessGroupService(
        com.backbase.dbs.accesscontrol.api.service.ApiClient accessControlApiClient,
        com.backbase.dbs.user.api.service.ApiClient usersApiClient,
        BackbaseStreamConfigurationProperties configurationProperties) {

        UserManagementApi usersApi = new UserManagementApi(usersApiClient);
        UserQueryApi userQueryApi = new UserQueryApi(accessControlApiClient);
        UsersApi accessControlUsersApi = new UsersApi(accessControlApiClient);
        DataGroupApi dataGroupApi = new DataGroupApi(accessControlApiClient);
        DataGroupsApi dataGroupsApi = new DataGroupsApi(accessControlApiClient);
        FunctionGroupApi functionGroupApi = new FunctionGroupApi(accessControlApiClient);
        FunctionGroupsApi functionGroupsApi = new FunctionGroupsApi(accessControlApiClient);
        ServiceAgreementQueryApi serviceAgreementQueryApi = new ServiceAgreementQueryApi(accessControlApiClient);
        ServiceAgreementApi serviceAgreementApi = new ServiceAgreementApi(accessControlApiClient);
        ServiceAgreementsApi serviceAgreementsApi = new ServiceAgreementsApi(accessControlApiClient);
        return new AccessGroupService(usersApi, userQueryApi, accessControlUsersApi, dataGroupApi, dataGroupsApi,
            functionGroupApi, functionGroupsApi, serviceAgreementQueryApi, serviceAgreementApi, serviceAgreementsApi,
            configurationProperties);
    }

    @Bean
    public com.backbase.dbs.user.api.service.ApiClient usersApiClient(
        @Qualifier(WebClientConstants.INTER_SERVICE_WEB_CLIENT_NAME) WebClient dbsWebClient,
        ObjectMapper objectMapper,
        DateFormat dateFormat) {
        com.backbase.dbs.user.api.service.ApiClient apiClient =
            new com.backbase.dbs.user.api.service.ApiClient(
                dbsWebClient, objectMapper, dateFormat);
        apiClient.setBasePath(backbaseStreamConfigurationProperties.getDbs().getUserManagerBaseUrl());
        return apiClient;
    }

    @Bean
    public com.backbase.dbs.user.profile.api.service.ApiClient userProfileApiClient(
        @Qualifier(WebClientConstants.INTER_SERVICE_WEB_CLIENT_NAME) WebClient dbsWebClient,
        ObjectMapper objectMapper,
        DateFormat dateFormat) {
        com.backbase.dbs.user.profile.api.service.ApiClient apiClient =
            new com.backbase.dbs.user.profile.api.service.ApiClient(
                dbsWebClient, objectMapper, dateFormat);
        apiClient.setBasePath(backbaseStreamConfigurationProperties.getDbs().getUserProfileManagerBaseUrl());
        return apiClient;
    }

    @Bean
    public com.backbase.dbs.accesscontrol.api.service.ApiClient accessControlApiClient(
        @Qualifier(WebClientConstants.INTER_SERVICE_WEB_CLIENT_NAME) WebClient dbsWebClient,
        ObjectMapper objectMapper,
        DateFormat dateFormat) {
        com.backbase.dbs.accesscontrol.api.service.ApiClient apiClient =
            new com.backbase.dbs.accesscontrol.api.service.ApiClient(
                dbsWebClient, objectMapper, dateFormat);
        apiClient.setBasePath(backbaseStreamConfigurationProperties.getDbs().getAccessControlBaseUrl());
        return apiClient;
    }

    @Bean
    @ConditionalOnProperty(value = "backbase.stream.legalentity.sink.use-identity-integration")
    public com.backbase.identity.integration.api.service.ApiClient identityApiClient(
        @Qualifier(WebClientConstants.INTER_SERVICE_WEB_CLIENT_NAME) WebClient dbsWebClient,
        ObjectMapper objectMapper,
        DateFormat dateFormat) {
        if (backbaseStreamConfigurationProperties.getIdentity() == null
            || backbaseStreamConfigurationProperties.getIdentity().getIdentityIntegrationBaseUrl() == null) {
            log.error("missing identity url configuration");
            return null;
        }
        com.backbase.identity.integration.api.service.ApiClient apiClient =
            new com.backbase.identity.integration.api.service.ApiClient(
                dbsWebClient, objectMapper, dateFormat);
        apiClient.setBasePath(backbaseStreamConfigurationProperties.getIdentity().getIdentityIntegrationBaseUrl());
        return apiClient;
    }

}
