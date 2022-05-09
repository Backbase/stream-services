package com.backbase.stream;

import static com.backbase.stream.service.UserService.REMOVED_PREFIX;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.dbs.user.api.service.v2.model.GetUser;
import com.backbase.dbs.user.api.service.v2.model.GetUsersList;
import com.backbase.dbs.user.api.service.v2.model.Realm;
import com.backbase.stream.configuration.LegalEntitySagaConfigurationProperties;
import com.backbase.stream.legalentity.model.BaseProductGroup;
import com.backbase.stream.legalentity.model.BusinessFunctionGroup;
import com.backbase.stream.legalentity.model.CurrentAccount;
import com.backbase.stream.legalentity.model.EmailAddress;
import com.backbase.stream.legalentity.model.IdentityUserLinkStrategy;
import com.backbase.stream.legalentity.model.JobProfileUser;
import com.backbase.stream.legalentity.model.JobRole;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.LegalEntityParticipant;
import com.backbase.stream.legalentity.model.LegalEntityStatus;
import com.backbase.stream.legalentity.model.LegalEntityType;
import com.backbase.stream.legalentity.model.PhoneNumber;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.legalentity.model.SavingsAccount;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.legalentity.model.User;
import com.backbase.stream.product.BatchProductIngestionSaga;
import com.backbase.stream.product.task.BatchProductGroupTask;
import com.backbase.stream.product.task.ProductGroupTask;
import com.backbase.stream.service.AccessGroupService;
import com.backbase.stream.service.LegalEntityService;
import com.backbase.stream.service.UserProfileService;
import com.backbase.stream.service.UserService;
import java.util.ArrayList;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class LegalEntitySagaTest {

    @InjectMocks
    private LegalEntitySaga legalEntitySaga;

    @Mock
    private LegalEntityService legalEntityService;

    @Mock
    private UserService userService;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private AccessGroupService accessGroupService;

    @Mock
    private BatchProductIngestionSaga batchProductIngestionSaga;

    @Spy
    private final LegalEntitySagaConfigurationProperties legalEntitySagaConfigurationProperties =
        getLegalEntitySagaConfigurationProperties();

    @Test
    void customServiceAgreementCreation() {
        String leExternalId = "someLeExternalId";
        String leParentExternalId = "someParentLeExternalId";
        String leInternalId = "someLeInternalId";
        String adminExId = "someAdminExId";
        String regularUserExId = "someRegularUserExId";
        String customSaExId = "someCustomSaExId";

        SavingsAccount account = new SavingsAccount();
        account.externalId("someAccountExId").productTypeExternalId("Account").currency("GBP");
        ProductGroup productGroup = new ProductGroup();
        productGroup.productGroupType(BaseProductGroup.ProductGroupTypeEnum.ARRANGEMENTS).name("somePgName")
            .description("somePgDescription").savingAccounts(Collections.singletonList(account));

        ProductGroupTask productGroupTask = new ProductGroupTask(productGroup);
        Mono<ProductGroupTask> productGroupTaskMono = Mono.just(productGroupTask);

        JobProfileUser regularUser = new JobProfileUser().user(new User().internalId("someRegularUserInId")
            .externalId(regularUserExId));
        BusinessFunctionGroup functionGroup = new BusinessFunctionGroup().name("someFunctionGroup");
        JobRole jobRole = new JobRole().functionGroups(Collections.singletonList(functionGroup)).name("someJobRole");
        ServiceAgreement customSa = new ServiceAgreement().externalId(customSaExId).addJobRolesItem(jobRole).creatorLegalEntity(leExternalId);
        User adminUser = new User().internalId("someAdminInId").externalId(adminExId);
        LegalEntity legalEntity = new LegalEntity().internalId(leInternalId).externalId(leExternalId).addAdministratorsItem(adminUser)
            .parentExternalId(leParentExternalId).customServiceAgreement(customSa).users(Collections.singletonList(regularUser))
            .productGroups(Collections.singletonList(productGroup)).subsidiaries(Collections.singletonList(
                    new LegalEntity().externalId(leExternalId).customServiceAgreement(customSa)
            ));

        LegalEntityTask task = mockLegalEntityTask(legalEntity);

        when(legalEntityService.getLegalEntityByExternalId(eq(leExternalId))).thenReturn(Mono.empty());
        when(legalEntityService.getLegalEntityByInternalId(eq(leInternalId))).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.createLegalEntity(any())).thenReturn(Mono.just(legalEntity));
        when(accessGroupService.getServiceAgreementByExternalId(eq(customSaExId))).thenReturn(Mono.empty());
        when(accessGroupService.createServiceAgreement(any(), eq(customSa))).thenReturn(Mono.just(customSa));
        when(accessGroupService.setupJobRole(any(), any(), any())).thenReturn(Mono.just(jobRole));
        when(accessGroupService.updateServiceAgreementRegularUsers(any(), eq(customSa), any())).thenReturn(Mono.just(customSa));
        when(userService.getUserByExternalId(eq(regularUserExId))).thenReturn(Mono.just(regularUser.getUser()));
        when(userService.getUserByExternalId(eq(adminExId))).thenReturn(Mono.just(adminUser));
        when(userService.createUser(any(), any(), any())).thenReturn(Mono.empty());
        when(batchProductIngestionSaga.process(any(ProductGroupTask.class))).thenReturn(productGroupTaskMono);
        when(userService.setupRealm(any())).thenReturn(Mono.just(new Realm()));
        when(userService.linkLegalEntityToRealm(any())).thenReturn(Mono.just(new LegalEntity()));

        LegalEntityTask result = legalEntitySaga.executeTask(task)
            .block();

        Assertions.assertNotNull(result);
        // To verify that processSubsidiaries was invoked
        Assertions.assertEquals(leExternalId, result.getData().getSubsidiaries().get(0).getParentExternalId());

        verify(accessGroupService).createServiceAgreement(eq(task), eq(customSa));
        verify(accessGroupService).updateServiceAgreementRegularUsers(eq(task), eq(customSa), any());
        verify(accessGroupService).setupJobRole(eq(task), eq(customSa), eq(jobRole));
    }

    @Test
    void customServiceAgreementCreationNoUsersOrAdministrators() {
        String leExternalId = "someLeExternalId";
        String leInternalId = "someLeInternalId";
        String customSaExId = "someCustomSaExId";

        SavingsAccount account = new SavingsAccount();
        account.externalId("someAccountExId").productTypeExternalId("Account").currency("GBP");
        ProductGroup productGroup = new ProductGroup();
        productGroup.productGroupType(BaseProductGroup.ProductGroupTypeEnum.ARRANGEMENTS).name("somePgName")
            .description("somePgDescription").savingAccounts(Collections.singletonList(account));

        ProductGroupTask productGroupTask = new ProductGroupTask(productGroup);
        Mono<ProductGroupTask> productGroupTaskMono = Mono.just(productGroupTask);

        BusinessFunctionGroup functionGroup = new BusinessFunctionGroup().name("someFunctionGroup");
        JobRole jobRole = new JobRole().functionGroups(Collections.singletonList(functionGroup)).name("someJobRole");
        ServiceAgreement customSa = new ServiceAgreement().externalId(customSaExId).addJobRolesItem(jobRole).creatorLegalEntity(leExternalId);
        LegalEntity legalEntity = new LegalEntity().internalId(leInternalId).externalId(leExternalId).customServiceAgreement(customSa)
            .parentExternalId(leExternalId).productGroups(Collections.singletonList(productGroup));

        LegalEntityTask task = mockLegalEntityTask(legalEntity);

        when(legalEntityService.getLegalEntityByExternalId(eq(leExternalId))).thenReturn(Mono.empty());
        when(legalEntityService.getLegalEntityByInternalId(eq(leInternalId))).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.createLegalEntity(any())).thenReturn(Mono.just(legalEntity));
        when(accessGroupService.getServiceAgreementByExternalId(eq(customSaExId))).thenReturn(Mono.empty());
        when(accessGroupService.createServiceAgreement(any(), eq(customSa))).thenReturn(Mono.just(customSa));
        when(accessGroupService.setupJobRole(any(), any(), any())).thenReturn(Mono.just(jobRole));
        when(accessGroupService.updateServiceAgreementRegularUsers(eq(task), eq(customSa), any()))
            .thenReturn(Mono.just(customSa));
        when(batchProductIngestionSaga.process(any(ProductGroupTask.class)))
            .thenReturn(productGroupTaskMono);
        when(userService.setupRealm(task.getLegalEntity()))
            .thenReturn(Mono.just(new Realm()));
        when(userService.linkLegalEntityToRealm(task.getLegalEntity()))
            .thenReturn(Mono.just(new LegalEntity()));

        Mono<LegalEntityTask> result = legalEntitySaga.executeTask(task);
        result.block();

        verify(userService).setupRealm(task.getLegalEntity());
        verify(userService).linkLegalEntityToRealm(task.getLegalEntity());
    }

    @Test
    void masterServiceAgreementCreation() {
        String leExternalId = "someLeExternalId";
        String leInternalId = "someLeInternalId";
        String customSaExId = "someCustomSaExId";

        SavingsAccount account = new SavingsAccount();
        account.externalId("someAccountExId").productTypeExternalId("Account").currency("GBP");
        ProductGroup productGroup = new ProductGroup();
        productGroup.productGroupType(BaseProductGroup.ProductGroupTypeEnum.ARRANGEMENTS).name("somePgName")
            .description("somePgDescription").savingAccounts(Collections.singletonList(account));

        ProductGroupTask productGroupTask = new ProductGroupTask(productGroup);
        Mono<ProductGroupTask> productGroupTaskMono = Mono.just(productGroupTask);

        BusinessFunctionGroup functionGroup = new BusinessFunctionGroup().name("someFunctionGroup");
        JobRole jobRole = new JobRole().functionGroups(Collections.singletonList(functionGroup)).name("someJobRole");
        ServiceAgreement sa = new ServiceAgreement().externalId(customSaExId).addJobRolesItem(jobRole).creatorLegalEntity(leExternalId);
        LegalEntity legalEntity = new LegalEntity().internalId(leInternalId).externalId(leExternalId).parentExternalId(leExternalId)
            .activateSingleServiceAgreement(false).masterServiceAgreement(sa).productGroups(Collections.singletonList(productGroup));

        LegalEntityTask task = mockLegalEntityTask(legalEntity);

        when(task.getLegalEntity()).thenReturn(legalEntity);
        when(legalEntityService.getLegalEntityByExternalId(eq(leExternalId))).thenReturn(Mono.empty());
        when(legalEntityService.getLegalEntityByInternalId(eq(leInternalId))).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.getMasterServiceAgreementForInternalLegalEntityId(eq(leInternalId))).thenReturn(Mono.just(sa));
        when(legalEntityService.createLegalEntity(any())).thenReturn(Mono.just(legalEntity));
        when(accessGroupService.setupJobRole(any(), any(), any())).thenReturn(Mono.just(jobRole));
        when(batchProductIngestionSaga.process(any(ProductGroupTask.class)))
            .thenReturn(productGroupTaskMono);
        when(userService.setupRealm(task.getLegalEntity()))
            .thenReturn(Mono.just(new Realm()));
        when(userService.linkLegalEntityToRealm(task.getLegalEntity()))
            .thenReturn(Mono.just(new LegalEntity()));

        Mono<LegalEntityTask> result = legalEntitySaga.executeTask(task);
        result.block();

        verify(userService).setupRealm(task.getLegalEntity());
        verify(userService).linkLegalEntityToRealm(task.getLegalEntity());

        when(legalEntityService.getLegalEntityByExternalId(eq(leExternalId))).thenReturn(Mono.just(legalEntity));
        result = legalEntitySaga.executeTask(task);
        result.block();

        verify(userService, times(2)).setupRealm(task.getLegalEntity());
        verify(userService, times(2)).linkLegalEntityToRealm(task.getLegalEntity());
    }

    @Test
    void masterServiceAgreementCreation_activateSingleServiceAgreement() {
        String leExternalId = "someLeExternalId";
        String leInternalId = "someLeInternalId";
        String customSaExId = "someCustomSaExId";

        SavingsAccount account = new SavingsAccount();
        account.externalId("someAccountExId").productTypeExternalId("Account").currency("GBP");
        ProductGroup productGroup = new ProductGroup();
        productGroup.productGroupType(BaseProductGroup.ProductGroupTypeEnum.ARRANGEMENTS).name("somePgName")
            .description("somePgDescription").savingAccounts(Collections.singletonList(account));

        ProductGroupTask productGroupTask = new ProductGroupTask(productGroup);
        Mono<ProductGroupTask> productGroupTaskMono = Mono.just(productGroupTask);

        BusinessFunctionGroup functionGroup = new BusinessFunctionGroup().name("someFunctionGroup");
        JobRole jobRole = new JobRole().functionGroups(Collections.singletonList(functionGroup)).name("someJobRole");
        LegalEntity legalEntity = new LegalEntity().internalId(leInternalId).externalId(leExternalId).parentExternalId(leExternalId)
            .productGroups(Collections.singletonList(productGroup));
        ServiceAgreement sa = new ServiceAgreement().externalId(customSaExId).addJobRolesItem(jobRole).creatorLegalEntity(leExternalId);

        LegalEntityTask task = mockLegalEntityTask(legalEntity);

        when(task.getLegalEntity()).thenReturn(legalEntity);
        when(legalEntityService.getLegalEntityByExternalId(eq(leExternalId))).thenReturn(Mono.empty());
        when(legalEntityService.getLegalEntityByInternalId(eq(leInternalId))).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.getMasterServiceAgreementForInternalLegalEntityId(eq(leInternalId))).thenReturn(Mono.empty());
        when(legalEntityService.createLegalEntity(any())).thenReturn(Mono.just(legalEntity));
        when(accessGroupService.setupJobRole(any(), any(), any())).thenReturn(Mono.just(jobRole));
        when(accessGroupService.createServiceAgreement(any(), any())).thenReturn(Mono.just(sa));
        when(batchProductIngestionSaga.process(any(ProductGroupTask.class)))
            .thenReturn(productGroupTaskMono);
        when(userService.setupRealm(task.getLegalEntity()))
            .thenReturn(Mono.just(new Realm()));
        when(userService.linkLegalEntityToRealm(task.getLegalEntity()))
            .thenReturn(Mono.just(new LegalEntity()));

        Mono<LegalEntityTask> result = legalEntitySaga.executeTask(task);
        result.block();

        verify(userService).setupRealm(task.getLegalEntity());
        verify(userService).linkLegalEntityToRealm(task.getLegalEntity());

        when(legalEntityService.getLegalEntityByExternalId(eq(leExternalId))).thenReturn(Mono.just(legalEntity));
        result = legalEntitySaga.executeTask(task);
        result.block();

        verify(userService, times(2)).setupRealm(task.getLegalEntity());
        verify(userService, times(2)).linkLegalEntityToRealm(task.getLegalEntity());
    }

    /**
     * Intention of this test is to verify that {@link ProductGroupTask} are processed in order at
     * {@link LegalEntitySaga#processProducts(LegalEntityTask)} (in short that .concatMap is used instead of .flatMap).
     * Otherwise it may happen that during permission assignment at
     * {@link AccessGroupService#assignPermissionsBatch(BatchProductGroupTask, Map)} there will be stale set of
     * permissions which will lead to state when not all of desired applied
     */
    @Test
    void productGroupsProcessedSequentially() {
        // Given
        LegalEntity legalEntity = new LegalEntity()
            .name("Legal Entity")
            .externalId("100000")
            .internalId("100001")
            .parentExternalId("parent-100000")
            .legalEntityType(LegalEntityType.CUSTOMER)
            .realmName("customer-bank")
            .productGroups(Arrays.asList(
                (ProductGroup) new ProductGroup()
                    .productGroupType(BaseProductGroup.ProductGroupTypeEnum.ARRANGEMENTS)
                    .name("Default PG")
                    .users(Collections.singletonList(
                        new JobProfileUser()
                            .user(
                                new User()
                                    .externalId("john.doe")
                                    .fullName("John Doe")
                                    .identityLinkStrategy(IdentityUserLinkStrategy.IDENTITY_AGNOSTIC)
                            )
                            .referenceJobRoleNames(Collections.singletonList("Private - Read only"))
                    ))
                    .currentAccounts(Collections.singletonList(
                        (CurrentAccount) new CurrentAccount()
                            .BBAN("01318000")
                            .externalId("7155000")
                            .productTypeExternalId("privateCurrentAccount")
                            .name("Account 1")
                            .currency("GBP")
                    )),
                (ProductGroup) new ProductGroup()
                    .productGroupType(BaseProductGroup.ProductGroupTypeEnum.ARRANGEMENTS)
                    .name("Mixed PG")
                    .users(Collections.singletonList(
                        new JobProfileUser()
                            .user(
                                new User()
                                    .externalId("john.doe")
                                    .fullName("John Doe")
                                    .identityLinkStrategy(IdentityUserLinkStrategy.IDENTITY_AGNOSTIC)
                            )
                            .referenceJobRoleNames(Collections.singletonList("Private - Full access"))
                    ))
                    .currentAccounts(Collections.singletonList(
                        (CurrentAccount) new CurrentAccount()
                            .BBAN("01318001")
                            .externalId("7155001")
                            .productTypeExternalId("privateCurrentAccount")
                            .name("Account 2")
                            .currency("GBP")
                    ))
            ))
            .users(Collections.singletonList(
                new JobProfileUser()
                    .user(
                        new User()
                            .externalId("john.doe")
                            .fullName("John Doe")
                            .identityLinkStrategy(IdentityUserLinkStrategy.CREATE_IN_IDENTITY)
                            .locked(false)
                            .emailAddress(new EmailAddress().address("test@example.com"))
                            .mobileNumber(new PhoneNumber().number("+12345"))
                    )
                    .referenceJobRoleNames(Arrays.asList(
                        "Private - Read only", "Private - Full access"
                    ))
            ))
            .administrators(Collections.emptyList())
            .customServiceAgreement(
                new ServiceAgreement()
                    .externalId("Service_Agreement_Id")
                    .name("Service Agreement")
                    .description("Custom Service Agreement")
                    .participants(Collections.singletonList(
                        new LegalEntityParticipant()
                            .externalId("user-external-id")
                            .sharingUsers(true)
                            .sharingAccounts(true)
                            .users(Collections.singletonList("john.doe"))
                    ))
                    .status(LegalEntityStatus.ENABLED)
                    .isMaster(false)
            );

        LegalEntityTask legalEntityTask = new LegalEntityTask(legalEntity);

        List<String> productGroupTaskProcessingOrder = new CopyOnWriteArrayList<>();

        when(legalEntityService.getLegalEntityByExternalId("100000"))
            .thenReturn(Mono.just(legalEntityTask.getLegalEntity()));
        when(legalEntityService.getLegalEntityByInternalId("100001"))
            .thenReturn(Mono.just(legalEntityTask.getLegalEntity()));
        when(legalEntityService.createLegalEntity(legalEntityTask.getLegalEntity()))
            .thenReturn(Mono.empty());
        when(legalEntitySagaConfigurationProperties.isUseIdentityIntegration())
            .thenReturn(true);
        when(userService.setupRealm(legalEntityTask.getLegalEntity()))
            .thenReturn(Mono.empty());
        when(userService.linkLegalEntityToRealm(legalEntityTask.getLegalEntity()))
            .thenReturn(Mono.empty());
        when(userService.getUserByExternalId("john.doe"))
            .thenReturn(Mono.just(new User().internalId("100").externalId("john.doe")));
        when(userService.createOrImportIdentityUser(any(), any(), any()))
            .thenReturn(Mono.empty());
        when(accessGroupService.getServiceAgreementByExternalId("Service_Agreement_Id"))
            .thenReturn(Mono.just(new ServiceAgreement().internalId("101").externalId("Service_Agreement_Id")));
        when(accessGroupService.updateServiceAgreementAssociations(any(), any(), any()))
            .thenReturn(Mono.just(new ServiceAgreement().internalId("101").externalId("Service_Agreement_Id")));
        when(accessGroupService.createServiceAgreement(any(), any()))
            .thenReturn(Mono.empty());
        when(accessGroupService.updateServiceAgreementRegularUsers(any(), any(), any()))
            .thenReturn(Mono.empty());
        when(accessGroupService.getFunctionGroupsForServiceAgreement("101"))
            .thenReturn(Mono.empty());
        when(batchProductIngestionSaga.process(any(ProductGroupTask.class)))
            .thenAnswer((Answer<Mono<ProductGroupTask>>) invocationOnMock -> {
                ProductGroupTask productGroupTask = invocationOnMock.getArgument(0);
                Duration delay = productGroupTask.getName().contains("100000-Default PG")
                    ? Duration.ofMillis(500) // First product group will be processed with delay
                    : Duration.ofMillis(1);

                return Mono.delay(delay)
                    .then(Mono.just(productGroupTask))
                    .map(productGroup -> {
                        productGroupTaskProcessingOrder.add(productGroup.getName());
                        return productGroup;
                    });
            });

        // When
        legalEntitySaga.executeTask(legalEntityTask)
            .block(Duration.ofSeconds(10));

        // Then
        Assertions.assertEquals(
            Arrays.asList("100000-Default PG", "100000-Mixed PG"),
            productGroupTaskProcessingOrder
        );
    }

    @Test
    void deleteLegalEntity_usersPrefixedWithRemovedNotProcessed() {
        String leExternalId = "someLeExternalId";
        String leInternalId = "someLeInternalId";
        String customSaExId = "someCustomSaExId";

        LegalEntity legalEntity = new LegalEntity().internalId(leInternalId).externalId(leExternalId)
            .parentExternalId(leExternalId);
        ServiceAgreement sa = new ServiceAgreement().externalId(customSaExId).creatorLegalEntity(leExternalId);

        when(legalEntityService.getMasterServiceAgreementForExternalLegalEntityId(leExternalId)).thenReturn(
            Mono.just(sa));
        when(legalEntityService.getLegalEntityByExternalId(leExternalId)).thenReturn(Mono.just(legalEntity));

        Long totalUsers = 5L;

        List<GetUser> users = getUsers(totalUsers.intValue());
        users.get(0).setExternalId(REMOVED_PREFIX + "user0");
        users.get(1).setExternalId(REMOVED_PREFIX + "user1");
        GetUsersList getUsersList1 = new GetUsersList();
        getUsersList1.setTotalElements(totalUsers);
        getUsersList1.users(users);

        when(userService.getUsersByLegalEntity(eq(leInternalId), anyInt(), anyInt()))
            .thenReturn(Mono.just(getUsersList1));

        when(accessGroupService.removePermissionsForUser(any(), any())).thenReturn(Mono.empty());

        when(accessGroupService.deleteFunctionGroupsForServiceAgreement(any())).thenReturn(Mono.empty());
        when(accessGroupService.deleteAdmins(any())).thenReturn(Mono.empty());
        when(userService.archiveUsers(any(), any())).thenReturn(Mono.empty());
        when(legalEntityService.deleteLegalEntity(any())).thenReturn(Mono.empty());

        Mono<Void> result = legalEntitySaga.deleteLegalEntity(leExternalId);
        result.block();

        verify(accessGroupService, times(3)).removePermissionsForUser(any(), any());
    }

    @Test
    void deleteLegalEntity_processesPaginatedListOfUsers() {
        String leExternalId = "someLeExternalId";
        String leInternalId = "someLeInternalId";
        String customSaExId = "someCustomSaExId";

        LegalEntity legalEntity = new LegalEntity().internalId(leInternalId).externalId(leExternalId)
            .parentExternalId(leExternalId);
        ServiceAgreement sa = new ServiceAgreement().externalId(customSaExId).creatorLegalEntity(leExternalId);

        when(legalEntityService.getMasterServiceAgreementForExternalLegalEntityId(leExternalId)).thenReturn(
            Mono.just(sa));
        when(legalEntityService.getLegalEntityByExternalId(leExternalId)).thenReturn(Mono.just(legalEntity));

        Long totalUsers = 22L;

        List<GetUser> users = getUsers(totalUsers.intValue());
        GetUsersList getUsersList1 = new GetUsersList();
        getUsersList1.setTotalElements(totalUsers);
        getUsersList1.users(users.subList(0, 10));

        GetUsersList getUsersList2 = new GetUsersList();
        getUsersList2.setTotalElements(totalUsers);
        getUsersList2.users(users.subList(10, 20));

        GetUsersList getUsersList3 = new GetUsersList();
        getUsersList3.setTotalElements(totalUsers);
        getUsersList3.users(users.subList(20, 22));

        when(userService.getUsersByLegalEntity(eq(leInternalId), anyInt(), anyInt()))
            .thenReturn(Mono.just(getUsersList1))
            .thenReturn(Mono.just(getUsersList2))
            .thenReturn(Mono.just(getUsersList3));

        when(accessGroupService.removePermissionsForUser(any(), any())).thenReturn(Mono.empty());

        when(accessGroupService.deleteFunctionGroupsForServiceAgreement(any())).thenReturn(Mono.empty());
        when(accessGroupService.deleteAdmins(any())).thenReturn(Mono.empty());
        when(userService.archiveUsers(any(), any())).thenReturn(Mono.empty());
        when(legalEntityService.deleteLegalEntity(any())).thenReturn(Mono.empty());

        Mono<Void> result = legalEntitySaga.deleteLegalEntity(leExternalId);
        result.block();

        verify(userService, times(3)).getUsersByLegalEntity(eq(leInternalId), anyInt(), anyInt());
    }

    private List<GetUser> getUsers(int amount) {
        List<GetUser> users = new ArrayList<>();

        for (var i = 0; i < amount; i++) {
            GetUser user = new GetUser();
            user.externalId("user_" + i);
            users.add(user);
        }
        return users;
    }

    @NotNull
    private LegalEntityTask mockLegalEntityTask(LegalEntity legalEntity) {
        LegalEntityTask task = Mockito.mock(LegalEntityTask.class);
        when(task.getData()).thenReturn(legalEntity);
        when(task.data(any())).thenReturn(task);
        when(task.addHistory(any())).thenReturn(task);
        return task;
    }

    @NotNull
    private LegalEntitySagaConfigurationProperties getLegalEntitySagaConfigurationProperties() {
        LegalEntitySagaConfigurationProperties sagaConfiguration =  new LegalEntitySagaConfigurationProperties();
        sagaConfiguration.setUseIdentityIntegration(true);
        return sagaConfiguration;
    }
}
