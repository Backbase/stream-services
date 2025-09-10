package com.backbase.stream;

import static com.backbase.stream.FixtureUtils.reflectiveAlphaFixtureMonkey;
import static com.backbase.stream.service.UserService.REMOVED_PREFIX;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.accesscontrol.serviceagreement.api.service.v1.model.Participant;
import com.backbase.customerprofile.api.integration.v1.model.PartyResponseUpsertDto;
import com.backbase.dbs.contact.api.service.v2.model.AccessContextScope;
import com.backbase.dbs.contact.api.service.v2.model.ContactsBulkPostRequestBody;
import com.backbase.dbs.contact.api.service.v2.model.ContactsBulkPostResponseBody;
import com.backbase.dbs.contact.api.service.v2.model.ExternalAccessContext;
import com.backbase.dbs.contact.api.service.v2.model.IngestMode;
import com.backbase.dbs.limit.api.service.v2.model.CreateLimitRequestBody;
import com.backbase.dbs.user.api.service.v2.model.GetUser;
import com.backbase.dbs.user.api.service.v2.model.GetUsersList;
import com.backbase.dbs.user.api.service.v2.model.Realm;
import com.backbase.dbs.user.profile.api.service.v2.model.GetUserProfile;
import com.backbase.stream.audiences.UserKindSegmentationSaga;
import com.backbase.stream.audiences.UserKindSegmentationTask;
import com.backbase.stream.configuration.LegalEntitySagaConfigurationProperties;
import com.backbase.stream.contact.ContactsSaga;
import com.backbase.stream.contact.ContactsTask;
import com.backbase.stream.legalentity.model.BaseProductGroup;
import com.backbase.stream.legalentity.model.BusinessFunction;
import com.backbase.stream.legalentity.model.BusinessFunctionGroup;
import com.backbase.stream.legalentity.model.CurrentAccount;
import com.backbase.stream.legalentity.model.CustomerCategory;
import com.backbase.stream.legalentity.model.EmailAddress;
import com.backbase.stream.legalentity.model.ExternalAccountInformation;
import com.backbase.stream.legalentity.model.ExternalContact;
import com.backbase.stream.legalentity.model.IdentityUserLinkStrategy;
import com.backbase.stream.legalentity.model.JobProfileUser;
import com.backbase.stream.legalentity.model.JobRole;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.LegalEntityParticipant;
import com.backbase.stream.legalentity.model.LegalEntityStatus;
import com.backbase.stream.legalentity.model.LegalEntityType;
import com.backbase.stream.legalentity.model.Limit;
import com.backbase.stream.legalentity.model.Loan;
import com.backbase.stream.legalentity.model.Multivalued;
import com.backbase.stream.legalentity.model.Party;
import com.backbase.stream.legalentity.model.PhoneNumber;
import com.backbase.stream.legalentity.model.Privilege;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.legalentity.model.SavingsAccount;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.legalentity.model.User;
import com.backbase.stream.legalentity.model.UserProfile;
import com.backbase.stream.limit.LimitsSaga;
import com.backbase.stream.limit.LimitsTask;
import com.backbase.stream.product.BatchProductIngestionSaga;
import com.backbase.stream.product.task.BatchProductGroupTask;
import com.backbase.stream.product.task.ProductGroupTask;
import com.backbase.stream.service.AccessGroupService;
import com.backbase.stream.service.CustomerProfileService;
import com.backbase.stream.service.LegalEntityService;
import com.backbase.stream.service.UserProfileService;
import com.backbase.stream.service.UserService;
import com.backbase.stream.worker.exception.StreamTaskException;
import com.navercorp.fixturemonkey.FixtureMonkey;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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

    @Mock
    private LimitsSaga limitsSaga;

    @Mock
    private ContactsSaga contactsSaga;

    @Mock
    private UserKindSegmentationSaga userKindSegmentationSaga;

    @Mock
    private CustomerProfileService customerProfileService;

    @Spy
    private final LegalEntitySagaConfigurationProperties legalEntitySagaConfigurationProperties =
        getLegalEntitySagaConfigurationProperties();

    private final FixtureMonkey fixtureMonkey = reflectiveAlphaFixtureMonkey;

    private static final int PARTY_SIZE = 10;

    String leExternalId = "someLeExternalId";
    String leParentExternalId = "someParentLeExternalId";
    String leInternalId = "someLeInternalId";
    String adminExId = "someAdminExId";
    String regularUserExId = "someRegularUserExId";
    String userId = "user_id";
    String customSaExId = "someCustomSaExId";
    LegalEntity legalEntity;
    ServiceAgreement customSa;
    JobProfileUser regularUser;

    @Test
    void customServiceAgreementCreation() {
        SavingsAccount account = new SavingsAccount();
        account.externalId("someAccountExId").productTypeExternalId("Account").currency("GBP");
        ProductGroup productGroup = new ProductGroup();
        productGroup.productGroupType(BaseProductGroup.ProductGroupTypeEnum.ARRANGEMENTS).name("somePgName")
            .description("somePgDescription").savingAccounts(singletonList(account));

        ProductGroupTask productGroupTask = new ProductGroupTask(productGroup);
        Mono<ProductGroupTask> productGroupTaskMono = Mono.just(productGroupTask);

        regularUser = new JobProfileUser().user(new User().internalId("someRegularUserInId")
            .externalId(regularUserExId));
        BusinessFunctionGroup functionGroup = new BusinessFunctionGroup().name("someFunctionGroup");
        JobRole jobRole = new JobRole().functionGroups(singletonList(functionGroup)).name("someJobRole");
        customSa = new ServiceAgreement().externalId(customSaExId).addJobRolesItem(jobRole)
            .creatorLegalEntity(leExternalId);
        User adminUser = new User().internalId("someAdminInId").externalId(adminExId);
        legalEntity = new LegalEntity().internalId(leInternalId).externalId(leExternalId)
            .addAdministratorsItem(adminUser)
            .parentExternalId(leParentExternalId).customServiceAgreement(customSa).users(singletonList(regularUser))
            .productGroups(singletonList(productGroup)).subsidiaries(singletonList(
                new LegalEntity().externalId(leExternalId).customServiceAgreement(customSa)
            ));

        LegalEntityTask task = mockLegalEntityTask(legalEntity);

        when(legalEntityService.getLegalEntityByExternalId(eq(leExternalId))).thenReturn(Mono.empty());
        when(legalEntityService.getLegalEntityByInternalId(eq(leInternalId))).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.createLegalEntity(any())).thenReturn(Mono.just(legalEntity));
        when(accessGroupService.getServiceAgreementByExternalId(eq(customSaExId))).thenReturn(Mono.empty());
        when(accessGroupService.createServiceAgreement(any(), eq(customSa))).thenReturn(Mono.just(customSa));
        when(accessGroupService.setupJobRole(any(), any(), any())).thenReturn(Mono.just(jobRole));
        when(accessGroupService.updateServiceAgreementRegularUsers(any(), eq(customSa), any())).thenReturn(
            Mono.just(customSa));
        when(userService.getUserByExternalId(eq(regularUserExId))).thenReturn(Mono.just(regularUser.getUser()));
        when(userService.getUserByExternalId(eq(adminExId))).thenReturn(Mono.just(adminUser));
        when(userService.createUser(any(), any(), any())).thenReturn(Mono.empty());
        when(batchProductIngestionSaga.process(any(ProductGroupTask.class))).thenReturn(productGroupTaskMono);
        when(userService.setupRealm(any())).thenReturn(Mono.just(new Realm()));
        when(userService.linkLegalEntityToRealm(any())).thenReturn(Mono.just(new LegalEntity()));
        when(userService.updateUser(any())).thenReturn(Mono.just(regularUser.getUser()));

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
        SavingsAccount account = new SavingsAccount();
        account.externalId("someAccountExId").productTypeExternalId("Account").currency("GBP");
        ProductGroup productGroup = new ProductGroup();
        productGroup.productGroupType(BaseProductGroup.ProductGroupTypeEnum.ARRANGEMENTS).name("somePgName")
            .description("somePgDescription").savingAccounts(singletonList(account));

        ProductGroupTask productGroupTask = new ProductGroupTask(productGroup);
        Mono<ProductGroupTask> productGroupTaskMono = Mono.just(productGroupTask);

        Limit limit = new Limit().currencyCode("GBP").transactional(BigDecimal.valueOf(10000));
        BusinessFunctionGroup functionGroup = new BusinessFunctionGroup().name("someFunctionGroup")
            .addFunctionsItem(new BusinessFunction().functionId("1071").name("US Domestic Wire")
                .addPrivilegesItem(new Privilege().privilege("create").limit(limit)));
        JobRole jobRole = new JobRole().functionGroups(singletonList(functionGroup)).name("someJobRole");
        customSa = new ServiceAgreement().externalId(customSaExId).addJobRolesItem(jobRole)
            .participants(Collections.singletonList(new LegalEntityParticipant().externalId(leExternalId)
                .users(List.of("externalUserId")).limit(limit))).creatorLegalEntity(leExternalId).limit(limit);
        legalEntity = new LegalEntity().internalId(leInternalId).externalId(leExternalId).limit(limit)
            .customServiceAgreement(customSa).parentExternalId(leExternalId).productGroups(singletonList(productGroup));

        LegalEntityTask task = mockLegalEntityTask(legalEntity);

        when(legalEntityService.getLegalEntityByExternalId(eq(leExternalId))).thenReturn(Mono.empty());
        when(legalEntityService.getLegalEntityByInternalId(eq(leInternalId))).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.createLegalEntity(any())).thenReturn(Mono.just(legalEntity));
        when(accessGroupService.getServiceAgreementByExternalId(eq(customSaExId))).thenReturn(Mono.empty());
        when(accessGroupService.createServiceAgreement(any(), eq(customSa))).thenReturn(Mono.just(customSa));
        when(accessGroupService.setupJobRole(any(), any(), any())).thenReturn(Mono.just(jobRole));
        when(accessGroupService.updateServiceAgreementRegularUsers(eq(task), eq(customSa), any()))
            .thenReturn(Mono.just(customSa));
        when(accessGroupService.getServiceAgreementParticipants(any(), any()))
            .thenReturn(Flux.just(new Participant().externalId(customSaExId)));
        when(batchProductIngestionSaga.process(any(ProductGroupTask.class)))
            .thenReturn(productGroupTaskMono);
        when(userService.setupRealm(task.getLegalEntity()))
            .thenReturn(Mono.just(new Realm()));
        when(userService.linkLegalEntityToRealm(task.getLegalEntity()))
            .thenReturn(Mono.just(new LegalEntity()));
        when(limitsSaga.executeTask(any(LimitsTask.class)))
            .thenReturn(Mono.just(new LimitsTask("1", new CreateLimitRequestBody())));

        Mono<LegalEntityTask> result = legalEntitySaga.executeTask(task);
        result.block();

        verify(userService).setupRealm(task.getLegalEntity());
        verify(userService).linkLegalEntityToRealm(task.getLegalEntity());
    }

    @Test
    void masterServiceAgreementCreation() {
        SavingsAccount account = new SavingsAccount();
        account.externalId("someAccountExId").productTypeExternalId("Account").currency("GBP");
        ProductGroup productGroup = new ProductGroup();
        productGroup.productGroupType(BaseProductGroup.ProductGroupTypeEnum.ARRANGEMENTS).name("somePgName")
            .description("somePgDescription").savingAccounts(singletonList(account));

        ProductGroupTask productGroupTask = new ProductGroupTask(productGroup);
        Mono<ProductGroupTask> productGroupTaskMono = Mono.just(productGroupTask);

        BusinessFunctionGroup functionGroup = new BusinessFunctionGroup().name("someFunctionGroup");
        JobRole jobRole = new JobRole().functionGroups(singletonList(functionGroup)).name("someJobRole");
        ServiceAgreement sa = new ServiceAgreement().externalId(customSaExId).addJobRolesItem(jobRole)
            .creatorLegalEntity(leExternalId);
        legalEntity = new LegalEntity().internalId(leInternalId).externalId(leExternalId).parentExternalId(leExternalId)
            .activateSingleServiceAgreement(false).masterServiceAgreement(sa)
            .productGroups(singletonList(productGroup));

        LegalEntityTask task = mockLegalEntityTask(legalEntity);

        when(task.getLegalEntity()).thenReturn(legalEntity);
        when(legalEntityService.getLegalEntityByExternalId(eq(leExternalId))).thenReturn(Mono.empty());
        when(legalEntityService.getLegalEntityByInternalId(eq(leInternalId))).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.getMasterServiceAgreementForInternalLegalEntityId(eq(leInternalId))).thenReturn(
            Mono.just(sa));
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
        when(legalEntityService.putLegalEntity(any())).thenReturn(Mono.just(legalEntity));
        result = legalEntitySaga.executeTask(task);
        result.block();

        verify(userService, times(2)).setupRealm(task.getLegalEntity());
        verify(userService, times(2)).linkLegalEntityToRealm(task.getLegalEntity());
    }

    @Test
    void masterServiceAgreementCreation_activateSingleServiceAgreement() {
        SavingsAccount account = new SavingsAccount();
        account.externalId("someAccountExId").productTypeExternalId("Account").currency("GBP");
        ProductGroup productGroup = new ProductGroup();
        productGroup.productGroupType(BaseProductGroup.ProductGroupTypeEnum.ARRANGEMENTS).name("somePgName")
            .description("somePgDescription").savingAccounts(singletonList(account));

        ProductGroupTask productGroupTask = new ProductGroupTask(productGroup);
        Mono<ProductGroupTask> productGroupTaskMono = Mono.just(productGroupTask);

        BusinessFunctionGroup functionGroup = new BusinessFunctionGroup().name("someFunctionGroup");
        JobRole jobRole = new JobRole().functionGroups(singletonList(functionGroup)).name("someJobRole");
        legalEntity = new LegalEntity().internalId(leInternalId).externalId(leExternalId).parentExternalId(leExternalId)
            .productGroups(singletonList(productGroup));
        ServiceAgreement sa = new ServiceAgreement().externalId(customSaExId).addJobRolesItem(jobRole)
            .creatorLegalEntity(leExternalId);

        LegalEntityTask task = mockLegalEntityTask(legalEntity);

        when(task.getLegalEntity()).thenReturn(legalEntity);
        when(legalEntityService.getLegalEntityByExternalId(eq(leExternalId))).thenReturn(Mono.empty());
        when(legalEntityService.getLegalEntityByInternalId(eq(leInternalId))).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.getMasterServiceAgreementForInternalLegalEntityId(eq(leInternalId))).thenReturn(
            Mono.empty());
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
        when(legalEntityService.putLegalEntity(any())).thenReturn(Mono.just(legalEntity));
        result = legalEntitySaga.executeTask(task);
        result.block();

        verify(userService, times(2)).setupRealm(task.getLegalEntity());
        verify(userService, times(2)).linkLegalEntityToRealm(task.getLegalEntity());
    }

    @Test
    void testCustomServiceAgreement_IfFetchedServiceAgreementExists_ThenSettingUp() {
        var task = setupLegalEntityTask();

        when(customerProfileService.upsertParty(any(Party.class), anyString())).thenReturn(
            Mono.just(fixtureMonkey.giveMeOne(PartyResponseUpsertDto.class)));

        mockAccessGroupService(userId);
        mockUserService(userId);

        executeAndVerifyTask(task, 0);

        verifyAccessGroupService();
        verifyUserService();
    }

    @Test
    void testCustomServiceAgreement_IfNoCustomServiceAgreementExists_ThenCreateMaster() {
        var task = setupLegalEntityTask();

        when(customerProfileService.upsertParty(any(Party.class), anyString())).thenReturn(
            Mono.just(fixtureMonkey.giveMeOne(PartyResponseUpsertDto.class)));
        when(accessGroupService.getUserContextsByUserId(userId)).thenReturn(Mono.empty());
        mockUserService(userId);

        executeAndVerifyTask(task, 1);

        verifyAccessGroupService();
        verifyUserService();
    }

    @Test
    void testCustomServiceAgreement_IfNoMatchingCustomServiceAgreementExists_ThenCreateMaster() {
        LegalEntityTask task = setupLegalEntityTask();

        when(customerProfileService.upsertParty(any(Party.class), anyString())).thenReturn(
            Mono.just(fixtureMonkey.giveMeOne(PartyResponseUpsertDto.class)));

        when(accessGroupService.getUserContextsByUserId(userId))
            .thenReturn(
                Mono.just(List.of(new ServiceAgreement().internalId("sa_id").externalId("sa_ext_id").purpose("TEST"))));
        mockUserService(userId);

        executeAndVerifyTask(task, 1);

        verifyAccessGroupService();
        verifyUserService();
    }

    @Test
    void testCustomServiceAgreement_IfNoUserDataFoundWhileServiceAgreementFetch_throwError() {
        LegalEntityTask task = setupLegalEntityTask();
        when(userService.getUsersByLegalEntity(any(), anyInt(), anyInt()))
            .thenReturn(Mono.just(new GetUsersList().totalElements(0L).users(null)));
        when(customerProfileService.upsertParty(any(Party.class), anyString())).thenReturn(
            Mono.just(fixtureMonkey.giveMeOne(PartyResponseUpsertDto.class)));
        Assertions.assertThrows(
            StreamTaskException.class,
            () -> executeLegalEntityTaskAndBlock(task),
            "No users found for Legal Entity"
        );
        verifyUserService();
    }

    @Test
    void testSetupParties_IfPartyFound_ThenUpsertParty() {
        var task = setupLegalEntityTask();
        when(customerProfileService.upsertParty(any(Party.class), anyString())).thenReturn(
            Mono.just(fixtureMonkey.giveMeOne(PartyResponseUpsertDto.class)));
        mockAccessGroupService(userId);
        mockUserService(userId);
        legalEntitySaga.executeTask(task).block();
        verify(customerProfileService, times(PARTY_SIZE)).upsertParty(any(Party.class), anyString());
    }

    @Test
    void testProcessCustomerProfile_IfUpsertPartyError_ThenTrowException() {
        var task = setupLegalEntityTask();
        var mockException = new WebClientResponseException(
            "CPS Error",
            400,
            "Bad Request",
            null,
            null,
            null
        );
        when(customerProfileService.upsertParty(any(Party.class), anyString())).thenReturn(Mono.error(mockException));

        mockAccessGroupService(userId);
        mockUserService(userId);
        legalEntitySaga.executeTask(task).block();
        verify(customerProfileService, times(PARTY_SIZE)).upsertParty(any(Party.class), anyString());
        verify(task, times(PARTY_SIZE)).error(
            eq(LegalEntitySaga.PARTY),
            eq(LegalEntitySaga.PROCESS_CUSTOMER_PROFILE),
            eq("failed"),
            anyString(),
            isNull(),
            any(Throwable.class),
            anyString(),
            eq("Error upserting party: %s for LE: %s"),
            anyString(),
            anyString()
        );
    }


    private void mockUserService(String userId) {
        when(userService.getUsersByLegalEntity(any(), anyInt(), anyInt()))
            .thenReturn(Mono.just(new GetUsersList().totalElements(1L)
                .users(List.of(new GetUser().externalId("external_id").id(userId)))));
    }

    private void mockAccessGroupService(String userId) {
        when(accessGroupService.getUserContextsByUserId(userId))
            .thenReturn(Mono.just(List.of(new ServiceAgreement().internalId("sa_id")
                .externalId("sa_ext_id").purpose("FAMILY_BANKING"))));
    }

    private void verifyAccessGroupService() {
        verify(accessGroupService).getUserContextsByUserId(userId);
    }

    private void verifyUserService() {
        verify(userService).getUsersByLegalEntity(any(), anyInt(), anyInt());
    }

    private LegalEntityTask setupLegalEntityTask() {
        var account = new SavingsAccount();
        account.externalId("someAccountExId").productTypeExternalId("Account").currency("GBP");
        var productGroup = new ProductGroup();
        productGroup.productGroupType(BaseProductGroup.ProductGroupTypeEnum.ARRANGEMENTS).name("somePgName")
            .description("somePgDescription").savingAccounts(singletonList(account));

        var productGroupTask = new ProductGroupTask(productGroup);
        var productGroupTaskMono = Mono.justOrEmpty(productGroupTask);

        var functionGroup = new BusinessFunctionGroup("someFunctionGroup");
        var jobRole = new JobRole("someJobRole", "someJobRole");
        jobRole.setFunctionGroups(singletonList(functionGroup));

        var setuplegalEntity = new LegalEntity("le_name", null, null);
        setuplegalEntity.setInternalId(leInternalId);
        setuplegalEntity.setExternalId(leExternalId);
        setuplegalEntity.setParentExternalId(leExternalId);
        setuplegalEntity.setProductGroups(singletonList(productGroup));
        setuplegalEntity.setParties(fixtureMonkey.giveMe(Party.class, PARTY_SIZE));
        var sa = new ServiceAgreement().externalId(customSaExId).addJobRolesItem(jobRole)
            .creatorLegalEntity(leExternalId);

        var task = mockLegalEntityTask(setuplegalEntity);

        when(task.getLegalEntity()).thenReturn(setuplegalEntity);
        when(legalEntityService.getLegalEntityByExternalId(leExternalId)).thenReturn(Mono.empty());
        when(legalEntityService.getLegalEntityByInternalId(leInternalId)).thenReturn(Mono.just(setuplegalEntity));
        when(legalEntityService.getMasterServiceAgreementForInternalLegalEntityId(leInternalId)).thenReturn(
            Mono.empty());
        when(legalEntityService.createLegalEntity(any())).thenReturn(Mono.just(setuplegalEntity));
        when(accessGroupService.setupJobRole(any(), any(), any())).thenReturn(Mono.just(jobRole));
        when(accessGroupService.createServiceAgreement(any(), any())).thenReturn(Mono.just(sa));
        when(batchProductIngestionSaga.process(any(ProductGroupTask.class))).thenReturn(productGroupTaskMono);
        when(legalEntitySagaConfigurationProperties.getServiceAgreementPurposes()).thenReturn(
            Set.of("FAMILY_BANKING"));
        when(userService.setupRealm(task.getLegalEntity())).thenReturn(Mono.just(new Realm()));
        when(userService.linkLegalEntityToRealm(task.getLegalEntity())).thenReturn(Mono.just(setuplegalEntity));
        when(legalEntityService.getLegalEntityByExternalId(leExternalId)).thenReturn(Mono.just(setuplegalEntity));
        when(legalEntityService.putLegalEntity(any())).thenReturn(Mono.just(setuplegalEntity));

        return task;
    }

    private void executeAndVerifyTask(LegalEntityTask task, int createServiceAgreementTimes) {
        var result = legalEntitySaga.executeTask(task);
        result.block();

        verify(userService).setupRealm(task.getLegalEntity());
        verify(userService).linkLegalEntityToRealm(task.getLegalEntity());
        verify(accessGroupService).getUserContextsByUserId(userId);
        verify(userService).getUsersByLegalEntity(any(), anyInt(), anyInt());
        verify(accessGroupService, times(createServiceAgreementTimes)).createServiceAgreement(any(), any());
    }

    /**
     * Intention of this test is to verify that {@link ProductGroupTask} are processed in order at (in short that
     * .concatMap is used instead of .flatMap). Otherwise it may happen that during permission assignment at
     * {@link AccessGroupService#assignPermissionsBatch(BatchProductGroupTask, Map)} there will be stale set of
     * permissions which will lead to state when not all of desired applied
     */
    @Test
    void productGroupsProcessedSequentially() {
        // Given
        Limit limit = new Limit().currencyCode("GBP").transactional(BigDecimal.valueOf(10000));
        legalEntity = new LegalEntity()
            .name("Legal Entity")
            .externalId("100000")
            .internalId("100001")
            .parentExternalId("parent-100000")
            .legalEntityType(LegalEntityType.CUSTOMER)
            .realmName("customer-bank")
            .referenceJobRoles(Collections.singletonList(new JobRole()
                .name("Job Role with Limits").functionGroups(Collections.singletonList(new BusinessFunctionGroup()
                    .name("someFunctionGroup")
                    .addFunctionsItem(new BusinessFunction().functionId("1071").name("US Domestic Wire")
                        .addPrivilegesItem(new Privilege().privilege("create").limit(limit)))))))
            .productGroups(Arrays.asList(
                (ProductGroup) new ProductGroup()
                    .productGroupType(BaseProductGroup.ProductGroupTypeEnum.ARRANGEMENTS)
                    .name("Default PG")
                    .users(singletonList(
                        new JobProfileUser()
                            .user(
                                new User()
                                    .externalId("john.doe")
                                    .fullName("John Doe")
                                    .supportsLimit(true)
                                    .identityLinkStrategy(IdentityUserLinkStrategy.IDENTITY_AGNOSTIC)
                            )
                            .referenceJobRoleNames(List.of("Private - Read only", "Job Role with Limits"))
                    ))
                    .currentAccounts(singletonList(
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
                    .users(singletonList(
                        new JobProfileUser()
                            .user(
                                new User()
                                    .externalId("john.doe")
                                    .fullName("John Doe")
                                    .identityLinkStrategy(IdentityUserLinkStrategy.IDENTITY_AGNOSTIC)
                            )
                            .referenceJobRoleNames(singletonList("Private - Full access"))
                    ))
                    .currentAccounts(singletonList(
                        (CurrentAccount) new CurrentAccount()
                            .BBAN("01318001")
                            .externalId("7155001")
                            .productTypeExternalId("privateCurrentAccount")
                            .name("Account 2")
                            .currency("GBP")
                    ))
                    .loans(singletonList(new Loan().IBAN("IBAN123321")))
            ))
            .users(singletonList(
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
                    .participants(singletonList(
                        new LegalEntityParticipant()
                            .externalId("user-external-id")
                            .sharingUsers(true)
                            .sharingAccounts(true)
                            .users(singletonList("john.doe"))
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
        when(legalEntityService.putLegalEntity(any())).thenReturn(Mono.just(legalEntityTask.getLegalEntity()));
        when(legalEntitySagaConfigurationProperties.isUseIdentityIntegration())
            .thenReturn(true);
        when(legalEntitySagaConfigurationProperties.isServiceAgreementUpdateEnabled())
            .thenReturn(true);
        when(userService.setupRealm(legalEntityTask.getLegalEntity()))
            .thenReturn(Mono.empty());
        when(userService.linkLegalEntityToRealm(legalEntityTask.getLegalEntity()))
            .thenReturn(Mono.empty());
        when(userService.updateIdentity(any()))
            .thenReturn(Mono.empty());
        when(userService.getUserByExternalId("john.doe"))
            .thenReturn(Mono.just(new User().internalId("100").externalId("john.doe")));
        when(userService.createOrImportIdentityUser(any(), any(), any()))
            .thenReturn(Mono.empty());
        when(accessGroupService.getServiceAgreementByExternalId("Service_Agreement_Id"))
            .thenReturn(Mono.just(new ServiceAgreement().internalId("101").externalId("Service_Agreement_Id")));
        lenient().when(accessGroupService.updateServiceAgreementItem(any(), any()))
            .thenReturn(Mono.just(new ServiceAgreement().internalId("101").externalId("Service_Agreement_Id")));
        when(accessGroupService.updateServiceAgreementAssociations(any(), any(), any()))
            .thenReturn(Mono.just(new ServiceAgreement().internalId("101").externalId("Service_Agreement_Id")));
        when(accessGroupService.createServiceAgreement(any(), (ServiceAgreement) any()))
            .thenReturn(Mono.empty());
        when(accessGroupService.updateServiceAgreementRegularUsers(any(), any(), any()))
            .thenReturn(Mono.empty());
        when(accessGroupService.getFunctionGroupsForServiceAgreement("101"))
            .thenReturn(Mono.empty());
        when(accessGroupService.setupJobRole(any(), any(), any())).thenReturn(Mono.just(new JobRole()));
        when(accessGroupService.getUserByExternalId("john.doe", true))
            .thenReturn(Mono.just(new GetUser().externalId("john.doe").id("internalId")));
        when(limitsSaga.executeTask(any())).thenReturn(Mono.just(new LimitsTask("1", new CreateLimitRequestBody())));
        //       when(contactsSaga.executeTask(any())).thenReturn(Mono.just(new ContactsTask("1", new ContactsBulkPostRequestBody())));
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
            .block(Duration.ofSeconds(20));

        // Then
        Assertions.assertEquals(
            Arrays.asList("100000-Default PG", "100000-Mixed PG"),
            productGroupTaskProcessingOrder
        );
    }

    @Test
    void deleteLegalEntity_usersPrefixedWithRemovedNotProcessed() {
        legalEntity = new LegalEntity().internalId(leInternalId).externalId(leExternalId)
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

        when(accessGroupService.deleteFunctionGroupsForServiceAgreement(any(), sa.getExternalId())).thenReturn(
            Mono.empty());
        when(accessGroupService.deleteAdmins((ServiceAgreement) any())).thenReturn(Mono.empty());
        when(userService.archiveUsers(any(), any())).thenReturn(Mono.empty());
        when(legalEntityService.deleteLegalEntity(any())).thenReturn(Mono.empty());

        Mono<Void> result = legalEntitySaga.deleteLegalEntity(leExternalId);
        result.block();

        verify(accessGroupService, times(3)).removePermissionsForUser(any(), any());
    }

    @Test
    void deleteLegalEntity_processesPaginatedListOfUsers() {
        legalEntity = new LegalEntity().internalId(leInternalId).externalId(leExternalId)
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

        when(accessGroupService.deleteFunctionGroupsForServiceAgreement(any(), sa.getExternalId())).thenReturn(
            Mono.empty());
        when(accessGroupService.deleteAdmins((ServiceAgreement) any())).thenReturn(Mono.empty());
        when(userService.archiveUsers(any(), any())).thenReturn(Mono.empty());
        when(legalEntityService.deleteLegalEntity(any())).thenReturn(Mono.empty());

        Mono<Void> result = legalEntitySaga.deleteLegalEntity(leExternalId);
        result.block();

        verify(userService, times(3)).getUsersByLegalEntity(eq(leInternalId), anyInt(), anyInt());
    }

    @Test
    void updateLegalEntityName() {
        SavingsAccount account = new SavingsAccount();
        account.externalId("someAccountExId").productTypeExternalId("Account").currency("GBP");
        ProductGroup productGroup = new ProductGroup();
        productGroup.productGroupType(BaseProductGroup.ProductGroupTypeEnum.ARRANGEMENTS).name("somePgName")
            .description("somePgDescription").savingAccounts(singletonList(account));

        ProductGroupTask productGroupTask = new ProductGroupTask(productGroup);
        Mono<ProductGroupTask> productGroupTaskMono = Mono.just(productGroupTask);

        regularUser = new JobProfileUser().user(new User().internalId("someRegularUserInId")
            .externalId(regularUserExId));
        BusinessFunctionGroup functionGroup = new BusinessFunctionGroup().name("someFunctionGroup");
        JobRole jobRole = new JobRole().functionGroups(singletonList(functionGroup)).name("someJobRole");
        customSa = new ServiceAgreement().externalId(customSaExId).addJobRolesItem(jobRole)
            .creatorLegalEntity(leExternalId);
        User adminUser = new User().internalId("someAdminInId").externalId(adminExId);
        legalEntity = new LegalEntity().internalId(leInternalId).externalId(leExternalId).name("Model Bank")
            .addAdministratorsItem(adminUser).parentExternalId(leParentExternalId)
            .customServiceAgreement(customSa).users(singletonList(regularUser))
            .productGroups(singletonList(productGroup)).subsidiaries(singletonList(
                new LegalEntity().externalId(leExternalId).customServiceAgreement(customSa)
            ));

        LegalEntity newLE = new LegalEntity().internalId(leInternalId).externalId(leExternalId).name("New Model Bank")
            .addAdministratorsItem(adminUser).parentExternalId(leParentExternalId)
            .customServiceAgreement(customSa).users(singletonList(regularUser))
            .productGroups(singletonList(productGroup)).subsidiaries(singletonList(
                new LegalEntity().externalId(leExternalId).customServiceAgreement(customSa)
            ));

        LegalEntityTask task = mockLegalEntityTask(newLE);

        when(legalEntityService.getLegalEntityByExternalId(eq(leExternalId))).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.getLegalEntityByInternalId(eq(leInternalId))).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.putLegalEntity(any())).thenReturn(Mono.just(newLE));
        when(accessGroupService.getServiceAgreementByExternalId(eq(customSaExId))).thenReturn(Mono.empty());
        when(accessGroupService.createServiceAgreement(any(), eq(customSa))).thenReturn(Mono.just(customSa));
        when(accessGroupService.setupJobRole(any(), any(), any())).thenReturn(Mono.just(jobRole));
        when(accessGroupService.updateServiceAgreementRegularUsers(any(), eq(customSa), any())).thenReturn(
            Mono.just(customSa));
        when(userService.getUserByExternalId(eq(regularUserExId))).thenReturn(Mono.just(regularUser.getUser()));
        when(userService.getUserByExternalId(eq(adminExId))).thenReturn(Mono.just(adminUser));
        when(userService.createUser(any(), any(), any())).thenReturn(Mono.empty());
        when(batchProductIngestionSaga.process(any(ProductGroupTask.class))).thenReturn(productGroupTaskMono);
        when(userService.setupRealm(any())).thenReturn(Mono.just(new Realm()));
        when(userService.linkLegalEntityToRealm(any())).thenReturn(Mono.just(new LegalEntity()));
        when(userService.updateUser(any())).thenReturn(Mono.empty());

        LegalEntityTask result = legalEntitySaga.executeTask(task)
            .block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(newLE.getName(), result.getData().getName());

        verify(accessGroupService).createServiceAgreement(eq(task), eq(customSa));
        verify(accessGroupService).updateServiceAgreementRegularUsers(eq(task), eq(customSa), any());
        verify(accessGroupService).setupJobRole(eq(task), eq(customSa), eq(jobRole));
        verify(legalEntityService).putLegalEntity(eq(newLE));
    }

    @Test
    void updateUserName() {
        SavingsAccount account = new SavingsAccount();
        account.externalId("someAccountExId").productTypeExternalId("Account").currency("GBP");
        ProductGroup productGroup = new ProductGroup();
        productGroup.productGroupType(BaseProductGroup.ProductGroupTypeEnum.ARRANGEMENTS).name("somePgName")
            .description("somePgDescription").savingAccounts(singletonList(account));

        ProductGroupTask productGroupTask = new ProductGroupTask(productGroup);
        Mono<ProductGroupTask> productGroupTaskMono = Mono.just(productGroupTask);

        JobProfileUser newRegularUser = new JobProfileUser().user(new User().internalId("someRegularUserInId")
            .externalId(regularUserExId).fullName("New Name Regular User"));
        JobProfileUser oldRegularUser = new JobProfileUser().user(new User().internalId("someRegularUserInId")
            .externalId(regularUserExId).fullName("Old Name Regular User"));
        BusinessFunctionGroup functionGroup = new BusinessFunctionGroup().name("someFunctionGroup");
        JobRole jobRole = new JobRole().functionGroups(singletonList(functionGroup)).name("someJobRole");
        customSa = new ServiceAgreement().externalId(customSaExId).addJobRolesItem(jobRole)
            .creatorLegalEntity(leExternalId);
        User adminUser = new User().internalId("someAdminInId").externalId(adminExId).fullName("Admin");
        legalEntity = new LegalEntity().internalId(leInternalId).externalId(leExternalId).name("Model Bank")
            .addAdministratorsItem(adminUser).parentExternalId(leParentExternalId)
            .customServiceAgreement(customSa).users(singletonList(newRegularUser))
            .productGroups(singletonList(productGroup)).subsidiaries(singletonList(
                new LegalEntity().externalId(leExternalId).customServiceAgreement(customSa)
            ));

        LegalEntityTask task = mockLegalEntityTask(legalEntity);

        when(legalEntityService.getLegalEntityByExternalId(eq(leExternalId))).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.getLegalEntityByInternalId(eq(leInternalId))).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.createLegalEntity(any())).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.putLegalEntity(any())).thenReturn(Mono.empty());
        when(accessGroupService.getServiceAgreementByExternalId(eq(customSaExId))).thenReturn(Mono.empty());
        when(accessGroupService.createServiceAgreement(any(), eq(customSa))).thenReturn(Mono.just(customSa));
        when(accessGroupService.setupJobRole(any(), any(), any())).thenReturn(Mono.just(jobRole));
        when(accessGroupService.updateServiceAgreementRegularUsers(any(), eq(customSa), any())).thenReturn(
            Mono.just(customSa));
        when(userService.getUserByExternalId(eq(regularUserExId))).thenReturn(Mono.just(oldRegularUser.getUser()));
        when(userService.getUserByExternalId(eq(adminExId))).thenReturn(Mono.just(adminUser));
        when(userService.createUser(any(), any(), any())).thenReturn(Mono.just(adminUser));
        when(userService.updateUser(any())).thenReturn(Mono.empty());
        when(batchProductIngestionSaga.process(any(ProductGroupTask.class))).thenReturn(productGroupTaskMono);
        when(userService.setupRealm(any())).thenReturn(Mono.just(new Realm()));
        when(userService.linkLegalEntityToRealm(any())).thenReturn(Mono.just(new LegalEntity()));
        when(userService.updateUser(any())).thenReturn(Mono.just(newRegularUser.getUser()));

        LegalEntityTask result = legalEntitySaga.executeTask(task)
            .block();

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getData().getUsers());
        Assertions.assertNotNull(result.getData().getUsers().get(0));
        Assertions.assertEquals(newRegularUser.getUser().getFullName(),
            result.getData().getUsers().get(0).getUser().getFullName());

        verify(accessGroupService).createServiceAgreement(eq(task), eq(customSa));
        verify(accessGroupService).updateServiceAgreementRegularUsers(eq(task), eq(customSa), any());
        verify(accessGroupService).setupJobRole(eq(task), eq(customSa), eq(jobRole));
        verify(userService).updateUser(eq(newRegularUser.getUser()));
    }

    void getMockLegalEntity() {
        SavingsAccount account = new SavingsAccount();
        account.externalId("someAccountExId").productTypeExternalId("Account").currency("GBP");
        ProductGroup productGroup = new ProductGroup();
        productGroup.productGroupType(BaseProductGroup.ProductGroupTypeEnum.ARRANGEMENTS).name("somePgName")
            .description("somePgDescription").savingAccounts(singletonList(account));

        ProductGroupTask productGroupTask = new ProductGroupTask(productGroup);
        Mono<ProductGroupTask> productGroupTaskMono = Mono.just(productGroupTask);

        regularUser = new JobProfileUser().user(new User().internalId("someRegularUserInId")
            .externalId(regularUserExId));
        BusinessFunctionGroup functionGroup = new BusinessFunctionGroup().name("someFunctionGroup");
        JobRole jobRole = new JobRole().functionGroups(singletonList(functionGroup)).name("someJobRole");
        customSa = new ServiceAgreement().externalId(customSaExId).addJobRolesItem(jobRole)
            .creatorLegalEntity(leExternalId);
        User adminUser = new User().internalId("someAdminInId").externalId(adminExId);

        legalEntity = new LegalEntity()
            .internalId(leInternalId)
            .externalId(leExternalId)
            .addAdministratorsItem(adminUser)
            .parentExternalId(leParentExternalId)
            .customServiceAgreement(customSa)
            .users(singletonList(regularUser))
            .productGroups(singletonList(productGroup))
            .subsidiaries(singletonList(
                new LegalEntity().externalId(leExternalId).customServiceAgreement(customSa)
            ));

        when(legalEntityService.getLegalEntityByExternalId(leExternalId)).thenReturn(Mono.empty());
        when(legalEntityService.getLegalEntityByInternalId(leInternalId)).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.createLegalEntity(any())).thenReturn(Mono.just(legalEntity));
        when(accessGroupService.getServiceAgreementByExternalId(customSaExId)).thenReturn(Mono.empty());
        when(accessGroupService.createServiceAgreement(any(), eq(customSa))).thenReturn(Mono.just(customSa));
        when(accessGroupService.setupJobRole(any(), any(), any())).thenReturn(Mono.just(jobRole));
        when(accessGroupService.updateServiceAgreementRegularUsers(any(), eq(customSa), any())).thenReturn(
            Mono.just(customSa));
        when(userService.getUserByExternalId(regularUserExId)).thenReturn(Mono.just(regularUser.getUser()));
        when(userService.getUserByExternalId(adminExId)).thenReturn(Mono.just(adminUser));
        when(userService.createUser(any(), any(), any())).thenReturn(Mono.empty());
        when(batchProductIngestionSaga.process(any(ProductGroupTask.class))).thenReturn(productGroupTaskMono);
        when(userService.setupRealm(any())).thenReturn(Mono.just(new Realm()));
        when(userService.linkLegalEntityToRealm(any())).thenReturn(Mono.just(new LegalEntity()));
        when(userService.updateUser(any())).thenReturn(Mono.just(regularUser.getUser()));
    }

    @Test
    void test_PostLegalContacts() {
        getMockLegalEntity();
        LegalEntityTask task = mockLegalEntityTask(legalEntity);
        when(contactsSaga.executeTask(any(ContactsTask.class))).thenReturn(getContactsTask(AccessContextScope.LE));

        LegalEntityTask result = legalEntitySaga.executeTask(task).block();

        Assertions.assertNotNull(result);

        ExternalContact contact = getMockExternalContact();
        legalEntity.setContacts(Collections.singletonList(contact));
        result = legalEntitySaga.executeTask(task).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(leExternalId, result.getData().getContacts().get(0).getExternalId());
    }

    @Test
    void test_PostLegalContacts_NoUser() {
        SavingsAccount account = new SavingsAccount();
        account.externalId("someAccountExId").productTypeExternalId("Account").currency("GBP");
        ProductGroup productGroup = new ProductGroup();
        productGroup.productGroupType(BaseProductGroup.ProductGroupTypeEnum.ARRANGEMENTS).name("somePgName")
            .description("somePgDescription").savingAccounts(singletonList(account));

        ProductGroupTask productGroupTask = new ProductGroupTask(productGroup);
        Mono<ProductGroupTask> productGroupTaskMono = Mono.just(productGroupTask);

        regularUser = new JobProfileUser().user(new User().internalId("someRegularUserInId")
            .externalId(regularUserExId));
        BusinessFunctionGroup functionGroup = new BusinessFunctionGroup().name("someFunctionGroup");
        JobRole jobRole = new JobRole().functionGroups(singletonList(functionGroup)).name("someJobRole");
        customSa = new ServiceAgreement().externalId(customSaExId).addJobRolesItem(jobRole)
            .creatorLegalEntity(leExternalId);
        User adminUser = new User().internalId("someAdminInId").externalId(adminExId);

        ExternalContact contact = getMockExternalContact();

        legalEntity = new LegalEntity()
            .internalId(leInternalId)
            .externalId(leExternalId)
            .addAdministratorsItem(adminUser)
            .parentExternalId(leParentExternalId)
            .customServiceAgreement(customSa)
            .productGroups(singletonList(productGroup))
            .contacts(Collections.singletonList(contact))
            .subsidiaries(singletonList(
                new LegalEntity().externalId(leExternalId).customServiceAgreement(customSa)
            ));

        LegalEntityTask task = mockLegalEntityTask(legalEntity);

        when(legalEntityService.getLegalEntityByExternalId(leExternalId)).thenReturn(Mono.empty());
        when(legalEntityService.getLegalEntityByInternalId(leInternalId)).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.createLegalEntity(any())).thenReturn(Mono.just(legalEntity));
        when(accessGroupService.getServiceAgreementByExternalId(customSaExId)).thenReturn(Mono.empty());
        when(accessGroupService.createServiceAgreement(any(), eq(customSa))).thenReturn(Mono.just(customSa));
        when(accessGroupService.setupJobRole(any(), any(), any())).thenReturn(Mono.just(jobRole));
        when(accessGroupService.updateServiceAgreementRegularUsers(any(), eq(customSa), any())).thenReturn(
            Mono.just(customSa));
        when(userService.getUserByExternalId(adminExId)).thenReturn(Mono.just(adminUser));
        when(userService.createUser(any(), any(), any())).thenReturn(Mono.empty());
        when(batchProductIngestionSaga.process(any(ProductGroupTask.class))).thenReturn(productGroupTaskMono);
        when(userService.setupRealm(any())).thenReturn(Mono.just(new Realm()));
        when(userService.linkLegalEntityToRealm(any())).thenReturn(Mono.just(new LegalEntity()));
        when(userService.updateUser(any())).thenReturn(Mono.just(regularUser.getUser()));

        LegalEntityTask result = legalEntitySaga.executeTask(task).block();

        Assertions.assertNotNull(result);
    }

    @Test
    void test_PostSAContacts() {
        getMockLegalEntity();
        LegalEntityTask task = mockLegalEntityTask(legalEntity);
        when(contactsSaga.executeTask(any(ContactsTask.class))).thenReturn(getContactsTask(AccessContextScope.SA));

        LegalEntityTask result = legalEntitySaga.executeTask(task).block();

        Assertions.assertNotNull(result);

        ExternalContact contact = getMockExternalContact();
        customSa.setContacts(Collections.singletonList(contact));
        result = legalEntitySaga.executeTask(task).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(leExternalId,
            result.getData().getCustomServiceAgreement().getContacts().get(0).getExternalId());

        LegalEntityParticipant participant = new LegalEntityParticipant()
            .externalId(leExternalId)
            .sharingUsers(true)
            .users(singletonList("USER1"));
        customSa.setParticipants(singletonList(participant));
        result = legalEntitySaga.executeTask(task).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals("USER1",
            result.getData().getCustomServiceAgreement().getParticipants().get(0).getUsers().get(0));

        participant = participant.users(null).admins(singletonList("ADMIN1"));
        customSa.setParticipants(singletonList(participant));
        result = legalEntitySaga.executeTask(task).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals("ADMIN1",
            result.getData().getCustomServiceAgreement().getParticipants().get(0).getAdmins().get(0));
    }


    @Test
    void test_PostUserContacts() {
        getMockLegalEntity();
        LegalEntityTask task = mockLegalEntityTask(legalEntity);

        when(contactsSaga.executeTask(any(ContactsTask.class))).thenReturn(getContactsTask(AccessContextScope.USER));

        LegalEntityTask result = legalEntitySaga.executeTask(task).block();

        Assertions.assertNotNull(result);

        ExternalContact contact = getMockExternalContact();
        regularUser.setContacts(Collections.singletonList(contact));
        result = legalEntitySaga.executeTask(task).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(leExternalId, result.getData().getUsers().get(0).getContacts().get(0).getExternalId());
    }

    @Test
    void userKindSegmentationIsDisabled() {
        getMockLegalEntity();

        when(userKindSegmentationSaga.isEnabled()).thenReturn(false);

        legalEntitySaga.executeTask(mockLegalEntityTask(legalEntity)).block();

        verify(userKindSegmentationSaga, never()).executeTask(Mockito.any());
    }

    @Test
    void userKindSegmentationUsesLegalEntityCustomerCategory() {
        getMockLegalEntity();
        legalEntity.setCustomerCategory(CustomerCategory.RETAIL);

        when(userKindSegmentationSaga.isEnabled()).thenReturn(true);

        legalEntitySaga.executeTask(mockLegalEntityTask(legalEntity)).block();

        verify(userKindSegmentationSaga, times(0)).getDefaultCustomerCategory();
    }

    @Test
    void userKindSegmentationUsesDefaultCustomerCategory() {
        getMockLegalEntity();
        legalEntity.legalEntityType(LegalEntityType.CUSTOMER);

        when(userKindSegmentationSaga.isEnabled()).thenReturn(true);
        when(userKindSegmentationSaga.getDefaultCustomerCategory()).thenReturn(CustomerCategory.RETAIL.getValue());
        when(userKindSegmentationSaga.executeTask(any())).thenReturn(
            Mono.just(Mockito.mock(UserKindSegmentationTask.class)));

        legalEntitySaga.executeTask(mockLegalEntityTask(legalEntity)).block();

        verify(userKindSegmentationSaga, times(1)).getDefaultCustomerCategory();
    }

    @Test
    void whenUserKindSegmentationIsEnabledAndNoCustomerCategoryCanBeDeterminedReturnsError() {
        getMockLegalEntity();
        legalEntity.legalEntityType(LegalEntityType.CUSTOMER);

        when(userKindSegmentationSaga.isEnabled()).thenReturn(true);
        when(userKindSegmentationSaga.getDefaultCustomerCategory()).thenReturn(null);

        var task = mockLegalEntityTask(legalEntity);

        Assertions.assertThrows(
            StreamTaskException.class,
            () -> executeLegalEntityTaskAndBlock(task),
            "Failed to determine LE customerCategory for UserKindSegmentationSage."
        );
    }

    @ParameterizedTest
    @MethodSource("parameters_upster_error")
    void upster_error(Exception ex, String error) {

        // Given
        LegalEntityTask leTask = new LegalEntityTask(new LegalEntity().externalId(leExternalId));
        when(legalEntityService.getLegalEntityByExternalId(leExternalId)).thenReturn(Mono.error(ex));

        // When
        Mono<LegalEntityTask> result = legalEntitySaga.executeTask(leTask);
        StreamTaskException stEx = null;
        try {
            result.block();
        } catch (StreamTaskException e) {
            stEx = e;
        }

        // Then
        verify(legalEntityService).getLegalEntityByExternalId(any());
        org.assertj.core.api.Assertions.assertThat(stEx)
            .isNotNull()
            .extracting(e -> e.getTask().getHistory().get(1).getErrorMessage())
            .isEqualTo(error);
    }

    @Test
    void upsertUserWithProfile() {
        getMockLegalEntity();
        User user = regularUser.getUser();

        user.fullName("User With Profile")
            .emailAddress(new EmailAddress().address("userwp@test.com").primary(true))
            .mobileNumber(new PhoneNumber().number("0123456").primary(true))
            .userProfile(new UserProfile()
                .userId(user.getInternalId())
                .profileUrl("http://backbase.eu")
                .addAdditionalEmailsItem(new Multivalued().primary(true).value("userwp@test.com"))
                .addAdditionalPhoneNumbersItem(new Multivalued().primary(true).value("0123456")));

        legalEntity.setUsers(singletonList(new JobProfileUser().user(user)));

        GetUserProfile getUserProfile = new GetUserProfile().userId(user.getInternalId());
        when(userProfileService.upsertUserProfile(any())).thenReturn(Mono.just(getUserProfile));
        when(userService.getUserProfile(user.getInternalId())).thenReturn(
            Mono.just(new com.backbase.dbs.user.api.service.v2.model.UserProfile().fullName("User With Profile")));

        legalEntitySaga.executeTask(mockLegalEntityTask(legalEntity)).block();

        verify(userService).getUserProfile(user.getInternalId());
    }

    private static Stream<Arguments> parameters_upster_error() {
        return Stream.of(
            Arguments.of(new RuntimeException("Fake error"), "Fake error"),
            Arguments.of(new WebClientResponseException(400, "Bad request", null,
                "Fake validation error".getBytes(), Charset.defaultCharset()), "Fake validation error"));
    }


    private LegalEntityTask executeLegalEntityTaskAndBlock(LegalEntityTask task) {
        return legalEntitySaga.executeTask(task).block();
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

    private LegalEntityTask mockLegalEntityTask(LegalEntity legalEntity) {
        LegalEntityTask task = Mockito.mock(LegalEntityTask.class);
        when(task.getData()).thenReturn(legalEntity);
        when(task.data(any())).thenReturn(task);
        when(task.addHistory(any())).thenReturn(task);
        return task;
    }

    private LegalEntitySagaConfigurationProperties getLegalEntitySagaConfigurationProperties() {
        LegalEntitySagaConfigurationProperties sagaConfiguration = new LegalEntitySagaConfigurationProperties();
        sagaConfiguration.setUseIdentityIntegration(true);
        sagaConfiguration.setUserProfileEnabled(true);
        return sagaConfiguration;
    }


    private ExternalContact getMockExternalContact() {
        ExternalAccountInformation externalAccount = new ExternalAccountInformation()
            .name("ACC1")
            .externalId("ACCEXT1")
            .accountNumber("12345");
        return new ExternalContact()
            .name("Sam")
            .externalId(leExternalId)
            .accounts(Collections.singletonList(externalAccount));
    }

    private ContactsBulkPostRequestBody getMockContactsBulkRequest(AccessContextScope accessContextScope) {
        var request = new ContactsBulkPostRequestBody();
        request.setIngestMode(IngestMode.UPSERT);

        ExternalAccessContext accessContext = new ExternalAccessContext();
        accessContext.setScope(accessContextScope);
        accessContext.setExternalUserId("USER1");
        request.setAccessContext(accessContext);

        com.backbase.dbs.contact.api.service.v2.model.ExternalContact contact = new com.backbase.dbs.contact.api.service.v2.model.ExternalContact();
        contact.setName("TEST1");
        contact.setExternalId("TEST101");

        com.backbase.dbs.contact.api.service.v2.model.ExternalAccountInformation account = new com.backbase.dbs.contact.api.service.v2.model.ExternalAccountInformation();
        account.setName("TESTACC1");
        account.setExternalId("TESTACC101");
        contact.setAccounts(Collections.singletonList(account));
        request.setContacts(Collections.singletonList(contact));

        return request;
    }

    private Mono<ContactsTask> getContactsTask(AccessContextScope accessContextScope) {
        ContactsTask task = new ContactsTask("TEST1", getMockContactsBulkRequest(accessContextScope));
        task.setResponse(getMockResponse());
        return Mono.just(task);
    }

    private ContactsBulkPostResponseBody getMockResponse() {
        ContactsBulkPostResponseBody responseBody = new ContactsBulkPostResponseBody();
        responseBody.setSuccessCount(2);
        return responseBody;

    }
}
