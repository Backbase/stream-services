package com.backbase.stream.configuration;

import com.backbase.dbs.accesscontrol.query.service.api.AccesscontrolApi;
import com.backbase.dbs.accessgroup.presentation.service.api.AccessgroupsApi;
import com.backbase.dbs.legalentity.presentation.service.api.LegalentitiesApi;
import com.backbase.dbs.user.presentation.service.api.UsersApi;
import com.backbase.dbs.userprofile.api.UserProfileApi;
import com.backbase.stream.config.BackbaseStreamConfigurationProperties;
import com.backbase.stream.product.configuration.ProductConfiguration;
import com.backbase.stream.product.service.ArrangementService;
import com.backbase.stream.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.client.WebClient;

import java.text.DateFormat;

/**
 * Access Control Configuration.
 */
@Configuration
@EnableConfigurationProperties(BackbaseStreamConfigurationProperties.class)
@Import(ProductConfiguration.class)
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
        com.backbase.dbs.legalentity.presentation.service.ApiClient legalEntityApiClient,
        com.backbase.dbs.accesscontrol.query.service.ApiClient accessControlQueryApiClient) {
        LegalentitiesApi legalEntityApi = new LegalentitiesApi(legalEntityApiClient);
        AccesscontrolApi accessControlQueryApi = new AccesscontrolApi(accessControlQueryApiClient);
        return new LegalEntityService(legalEntityApi, accessControlQueryApi);
    }

    @Bean
    public UserService userService(com.backbase.dbs.user.presentation.service.ApiClient usersApiClient) {
        return new UserService(new UsersApi(usersApiClient));
    }

    @Bean
    public UserProfileService userProfileService(com.backbase.dbs.userprofile.ApiClient usersApiClient) {
        return new UserProfileService(new UserProfileApi(usersApiClient));
    }

    @Bean
    public AccessGroupService accessGroupService(
        com.backbase.dbs.accesscontrol.query.service.ApiClient accessControlQueryApiClient,
        com.backbase.dbs.accessgroup.presentation.service.ApiClient accessGroupsApiClient,
        com.backbase.dbs.user.presentation.service.ApiClient usersApiClient) {
        AccesscontrolApi accessControlQueryApi = new AccesscontrolApi(accessControlQueryApiClient);
        AccessgroupsApi accessGroupServiceApi = new AccessgroupsApi(accessGroupsApiClient);
        UsersApi usersApi = new UsersApi(usersApiClient);
        return new AccessGroupService(accessControlQueryApi, accessGroupServiceApi, usersApi);
    }

    @Bean
    protected com.backbase.dbs.user.presentation.service.ApiClient usersApiClient(
        WebClient dbsWebClient,
        ObjectMapper objectMapper,
        DateFormat dateFormat) {
        com.backbase.dbs.user.presentation.service.ApiClient apiClient =
            new com.backbase.dbs.user.presentation.service.ApiClient(
                dbsWebClient, objectMapper, dateFormat);
        apiClient.setBasePath(backbaseStreamConfigurationProperties.getDbs().getUserManagerBaseUrl());
        return apiClient;
    }

    @Bean
    protected com.backbase.dbs.userprofile.ApiClient userProfileApiClient(
        WebClient dbsWebClient,
        ObjectMapper objectMapper,
        DateFormat dateFormat) {
        com.backbase.dbs.userprofile.ApiClient apiClient =
            new com.backbase.dbs.userprofile.ApiClient(
                dbsWebClient, objectMapper, dateFormat);
        apiClient.setBasePath(backbaseStreamConfigurationProperties.getDbs().getUserProfileManagerBaseUrl());
        return apiClient;
    }

    @Bean
    protected com.backbase.dbs.accesscontrol.query.service.ApiClient accessControlQueryApiClient(
        WebClient dbsWebClient,
        ObjectMapper objectMapper,
        DateFormat dateFormat) {
        com.backbase.dbs.accesscontrol.query.service.ApiClient apiClient =
            new com.backbase.dbs.accesscontrol.query.service.ApiClient(
                dbsWebClient, objectMapper, dateFormat);
        apiClient.setBasePath(backbaseStreamConfigurationProperties.getDbs().getAccessControlBaseUrl());
        return apiClient;
    }

    @Bean
    protected com.backbase.dbs.accessgroup.presentation.service.ApiClient accessGroupApiClient(
        WebClient dbsWebClient,
        ObjectMapper objectMapper,
        DateFormat dateFormat) {
        com.backbase.dbs.accessgroup.presentation.service.ApiClient apiClient =
            new com.backbase.dbs.accessgroup.presentation.service.ApiClient(
                dbsWebClient, objectMapper, dateFormat);
        apiClient.setBasePath(backbaseStreamConfigurationProperties.getDbs().getAccessControlBaseUrl());
        return apiClient;
    }

    @Bean
    public com.backbase.dbs.legalentity.presentation.service.ApiClient legalEntityClientApi(
        WebClient dbsWebClient,
        ObjectMapper objectMapper,
        DateFormat dateFormat) {
        com.backbase.dbs.legalentity.presentation.service.ApiClient apiClient =
            new com.backbase.dbs.legalentity.presentation.service.ApiClient(
                dbsWebClient, objectMapper, dateFormat);
        apiClient.setBasePath(backbaseStreamConfigurationProperties.getDbs().getAccessControlBaseUrl());
        return apiClient;
    }

}
