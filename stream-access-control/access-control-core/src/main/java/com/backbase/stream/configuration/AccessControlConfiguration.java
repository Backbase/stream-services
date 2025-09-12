package com.backbase.stream.configuration;

import com.backbase.accesscontrol.assignpermissions.api.service.v1.AssignPermissionsApi;
import com.backbase.accesscontrol.customeraccessgroup.api.service.v1.CustomerAccessGroupApi;
import com.backbase.accesscontrol.datagroup.api.service.v1.DataGroupApi;
import com.backbase.accesscontrol.functiongroup.api.service.v1.FunctionGroupApi;
import com.backbase.accesscontrol.permissioncheck.api.service.v1.PermissionCheckApi;
import com.backbase.accesscontrol.serviceagreement.api.service.v1.ServiceAgreementApi;
import com.backbase.accesscontrol.usercontext.api.service.v1.UserContextApi;
import com.backbase.dbs.user.api.service.v2.IdentityManagementApi;
import com.backbase.dbs.user.api.service.v2.UserManagementApi;
import com.backbase.dbs.user.profile.api.service.v2.UserProfileManagementApi;
import com.backbase.identity.integration.api.service.v1.IdentityIntegrationServiceApi;
import com.backbase.stream.product.configuration.ProductConfiguration;
import com.backbase.stream.product.service.ArrangementService;
import com.backbase.stream.service.AccessGroupService;
import com.backbase.stream.service.CustomerAccessGroupService;
import com.backbase.stream.service.EntitlementsService;
import com.backbase.stream.service.LegalEntityService;
import com.backbase.stream.service.UserProfileService;
import com.backbase.stream.service.UserService;
import com.backbase.stream.utils.BatchResponseUtils;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Access Control Configuration.
 */
@Configuration
@Import(ProductConfiguration.class)
@Slf4j
@EnableConfigurationProperties({DeletionProperties.class, UserManagementProperties.class,
    AccessControlConfigurationProperties.class, CustomerAccessGroupConfigurationProperties.class})
public class AccessControlConfiguration {

    @Bean
    public BatchResponseUtils batchResponseUtils() {
        return new BatchResponseUtils();
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
        com.backbase.accesscontrol.legalentity.api.service.v1.LegalEntityApi legalEntitiesServiceApi,
        com.backbase.accesscontrol.legalentity.api.integration.v3.LegalEntityApi legalEntitiesIntegrationApi,
        BatchResponseUtils batchResponseUtils) {
        return new LegalEntityService(legalEntitiesServiceApi, legalEntitiesIntegrationApi, batchResponseUtils);
    }

    @Bean
    public UserService userService(Optional<IdentityIntegrationServiceApi> identityApi,
        UserManagementApi usersApi,
        IdentityManagementApi identityManagementApi,
        com.backbase.dbs.user.api.service.v2.UserProfileManagementApi userProfileManagementApi,
        UserManagementProperties userManagementProperties) {
        return new UserService(usersApi, identityManagementApi, identityApi,
            userProfileManagementApi, userManagementProperties);
    }

    @Bean
    public UserProfileService userProfileService(UserProfileManagementApi userProfileManagementApi) {
        return new UserProfileService(userProfileManagementApi);
    }

    @Bean
    public CustomerAccessGroupService customerAccessGroupService(CustomerAccessGroupApi customerAccessGroupApi) {
        return new CustomerAccessGroupService(customerAccessGroupApi);
    }

    @Bean
    public AccessGroupService accessGroupService(
        UserManagementApi usersApi,
        DeletionProperties configurationProperties,
        BatchResponseUtils batchResponseUtils,
        AccessControlConfigurationProperties accessControlConfigurationProperties,
        PermissionCheckApi permissionCheckApi,
        DataGroupApi dataGroupServiceApi,
        com.backbase.accesscontrol.datagroup.api.integration.v1.DataGroupApi dataGroupIntegrationApi,
        FunctionGroupApi functionGroupServiceApi,
        com.backbase.accesscontrol.functiongroup.api.integration.v1.FunctionGroupApi functionGroupIntegrationApi,
        ServiceAgreementApi serviceAgreementServiceApi,
        com.backbase.accesscontrol.serviceagreement.api.integration.v1.ServiceAgreementApi serviceAgreementIntegrationApi,
        AssignPermissionsApi assignPermissionsApi,
        UserContextApi userContextApi) {
        return new AccessGroupService(usersApi, batchResponseUtils, configurationProperties,
            accessControlConfigurationProperties,
            permissionCheckApi, dataGroupServiceApi, dataGroupIntegrationApi, functionGroupServiceApi,
            functionGroupIntegrationApi,
            serviceAgreementServiceApi, serviceAgreementIntegrationApi, assignPermissionsApi, userContextApi);
    }

}
