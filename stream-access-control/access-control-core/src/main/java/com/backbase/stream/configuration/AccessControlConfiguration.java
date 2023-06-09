package com.backbase.stream.configuration;

import com.backbase.dbs.accesscontrol.api.service.v3.DataGroupsApi;
import com.backbase.dbs.accesscontrol.api.service.v3.FunctionGroupsApi;
import com.backbase.dbs.accesscontrol.api.service.v3.LegalEntitiesApi;
import com.backbase.dbs.accesscontrol.api.service.v3.ServiceAgreementsApi;
import com.backbase.dbs.accesscontrol.api.service.v3.UsersApi;
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
import com.backbase.stream.utils.BatchResponseUtils;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** Access Control Configuration. */
@Configuration
@Import(ProductConfiguration.class)
@Slf4j
@EnableConfigurationProperties(DeletionProperties.class)
public class AccessControlConfiguration {

  @Bean
  public BatchResponseUtils batchResponseUtils() {
    return new BatchResponseUtils();
  }

  @Bean
  public EntitlementsService entitlementsService(
      ArrangementService arrangementService,
      AccessGroupService accessGroupService,
      LegalEntityService legalEntityService,
      UserService userService) {

    return new EntitlementsService(
        arrangementService, userService, accessGroupService, legalEntityService);
  }

  @Bean
  public LegalEntityService legalEntityService(
      LegalEntitiesApi legalEntitiesApi, BatchResponseUtils batchResponseUtils) {
    return new LegalEntityService(legalEntitiesApi, batchResponseUtils);
  }

  @Bean
  public UserService userService(
      Optional<IdentityIntegrationServiceApi> identityApi,
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
      UserManagementApi usersApi,
      DeletionProperties configurationProperties,
      UsersApi accessControlUsersApi,
      DataGroupsApi dataGroupsApi,
      ServiceAgreementsApi serviceAgreementsApi,
      FunctionGroupsApi functionGroupsApi,
      BatchResponseUtils batchResponseUtils) {
    return new AccessGroupService(
        usersApi,
        accessControlUsersApi,
        dataGroupsApi,
        functionGroupsApi,
        serviceAgreementsApi,
        configurationProperties,
        batchResponseUtils);
  }
}
