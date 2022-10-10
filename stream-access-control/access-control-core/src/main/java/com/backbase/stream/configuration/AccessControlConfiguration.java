package com.backbase.stream.configuration;

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
import com.backbase.dbs.user.api.service.ApiClient;
import com.backbase.dbs.user.api.service.v2.IdentityManagementApi;
import com.backbase.dbs.user.api.service.v2.UserManagementApi;
import com.backbase.dbs.user.profile.api.service.v2.UserProfileManagementApi;
import com.backbase.identity.integration.api.service.v1.IdentityIntegrationServiceApi;
import com.backbase.stream.product.configuration.ProductConfiguration;
import com.backbase.stream.product.service.ArrangementService;
import com.backbase.stream.service.AccessGroupService;
import com.backbase.stream.service.EntitlementsService;
import com.backbase.stream.service.LegalEntityService;
import com.backbase.stream.service.UserProfileService;
import com.backbase.stream.service.UserService;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.lang.NonNull;

/**
 * Access Control Configuration.
 */
@Configuration
@Import(ProductConfiguration.class)
@Slf4j
@EnableConfigurationProperties(DeletionProperties.class)
public class AccessControlConfiguration {

    @Bean
    public EntitlementsService entitlementsService(ArrangementService arrangementService,
        AccessGroupService accessGroupService,
        LegalEntityService legalEntityService,
        UserService userService) {

        return new EntitlementsService(arrangementService, userService, accessGroupService, legalEntityService);
    }

    @Bean
    public LegalEntityService legalEntityService(@NonNull LegalEntitiesApi legalEntitiesApi,
        @NonNull LegalEntityApi legalEntityApi) {
        return new LegalEntityService(legalEntitiesApi, legalEntityApi);
    }

    @Bean
    public UserService userService(Optional<IdentityIntegrationServiceApi> identityApi,
        UserManagementApi usersApi,
        IdentityManagementApi identityManagementApi) {
        return new UserService(usersApi, identityManagementApi, identityApi);
    }

    @Bean
    public UserProfileService userProfileService(UserProfileManagementApi userProfileManagementApi) {
        return new UserProfileService(userProfileManagementApi);
    }

    @Bean
    public AccessGroupService accessGroupService(
        ApiClient usersApiClient,
        DeletionProperties configurationProperties, @NonNull UserQueryApi userQueryApi,
        @NonNull UsersApi accessControlUsersApi, @NonNull DataGroupApi dataGroupApi,
        @NonNull DataGroupsApi dataGroupsApi, @NonNull ServiceAgreementsApi serviceAgreementsApi,
        @NonNull ServiceAgreementApi serviceAgreementApi, @NonNull ServiceAgreementQueryApi serviceAgreementQueryApi,
        @NonNull FunctionGroupsApi functionGroupsApi, @NonNull FunctionGroupApi functionGroupApi) {

        UserManagementApi usersApi = new UserManagementApi(usersApiClient);
        return new AccessGroupService(usersApi, userQueryApi, accessControlUsersApi, dataGroupApi, dataGroupsApi,
            functionGroupApi, functionGroupsApi, serviceAgreementQueryApi, serviceAgreementApi, serviceAgreementsApi,
            configurationProperties);
    }

}
