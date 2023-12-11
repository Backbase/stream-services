package com.backbase.stream;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.dbs.accesscontrol.api.service.v3.model.ServiceAgreementParticipantsGetResponseBody;
import com.backbase.dbs.contact.api.service.v2.model.AccessContextScope;
import com.backbase.dbs.contact.api.service.v2.model.ContactsBulkPostRequestBody;
import com.backbase.dbs.contact.api.service.v2.model.ContactsBulkPostResponseBody;
import com.backbase.dbs.contact.api.service.v2.model.ExternalAccessContext;
import com.backbase.dbs.contact.api.service.v2.model.IngestMode;
import com.backbase.dbs.limit.api.service.v2.model.CreateLimitRequestBody;
import com.backbase.dbs.user.api.service.v2.model.GetUser;
import com.backbase.dbs.user.api.service.v2.model.Realm;
import com.backbase.stream.configuration.LegalEntitySagaConfigurationProperties;
import com.backbase.stream.contact.ContactsSaga;
import com.backbase.stream.contact.ContactsTask;
import com.backbase.stream.legalentity.model.BaseProductGroup;
import com.backbase.stream.legalentity.model.BusinessFunction;
import com.backbase.stream.legalentity.model.BusinessFunctionGroup;
import com.backbase.stream.legalentity.model.CurrentAccount;
import com.backbase.stream.legalentity.model.EmailAddress;
import com.backbase.stream.legalentity.model.ExternalAccountInformation;
import com.backbase.stream.legalentity.model.ExternalContact;
import com.backbase.stream.legalentity.model.IdentityUserLinkStrategy;
import com.backbase.stream.legalentity.model.JobProfileUser;
import com.backbase.stream.legalentity.model.JobRole;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.LegalEntityParticipant;
import com.backbase.stream.legalentity.model.LegalEntityReference;
import com.backbase.stream.legalentity.model.Limit;
import com.backbase.stream.legalentity.model.Loan;
import com.backbase.stream.legalentity.model.PhoneNumber;
import com.backbase.stream.legalentity.model.Privilege;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.legalentity.model.ReferenceJobRole;
import com.backbase.stream.legalentity.model.SavingsAccount;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.legalentity.model.ServiceAgreementV2;
import com.backbase.stream.legalentity.model.User;
import com.backbase.stream.limit.LimitsSaga;
import com.backbase.stream.limit.LimitsTask;
import com.backbase.stream.product.BatchProductIngestionSaga;
import com.backbase.stream.product.task.BatchProductGroupTask;
import com.backbase.stream.product.task.ProductGroupTask;
import com.backbase.stream.service.AccessGroupService;
import com.backbase.stream.service.LegalEntityService;
import com.backbase.stream.service.UserService;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ServiceAgreementV2SagaTest {

    @InjectMocks
    private ServiceAgreementSagaV2 serviceAgreementSaga;

    @Mock
    private LegalEntityService legalEntityService;

    @Mock
    private AccessGroupService accessGroupService;

    @Mock
    private BatchProductIngestionSaga batchProductIngestionSaga;

    @Mock
    private LimitsSaga limitsSaga;

    @Mock
    private ContactsSaga contactsSaga;

    @Spy
    private final LegalEntitySagaConfigurationProperties legalEntitySagaConfigurationProperties =
        getLegalEntitySagaConfigurationProperties();

    String leExternalId = "someLeExternalId";
    String leParentExternalId = "someParentLeExternalId";
    String leInternalId = "someLeInternalId";
    String adminExId = "someAdminExId";
    String regularUserExId = "someRegularUserExId";
    String customSaExId = "someCustomSaExId";
    LegalEntity legalEntity;
    ServiceAgreementV2 customSa;
    JobProfileUser regularUser;

    @Test
    void customServiceAgreementCreation() {
        regularUser = new JobProfileUser().user(new User().internalId("someRegularUserInId")
            .externalId(regularUserExId)).legalEntityReference(new LegalEntityReference().externalId(leExternalId));
        SavingsAccount account = new SavingsAccount();
        account.externalId("someAccountExId").productTypeExternalId("Account").currency("GBP")
            .legalEntities(List.of(new LegalEntityReference().externalId(leExternalId).internalId(leExternalId)));
        ProductGroup productGroup = new ProductGroup();
        productGroup.productGroupType(BaseProductGroup.ProductGroupTypeEnum.ARRANGEMENTS).name("somePgName")
            .description("somePgDescription").savingAccounts(singletonList(account)).users(List.of(regularUser));

        ProductGroupTask productGroupTask = new ProductGroupTask(productGroup);
        Mono<ProductGroupTask> productGroupTaskMono = Mono.just(productGroupTask);

        BusinessFunctionGroup functionGroup = new BusinessFunctionGroup().name("someFunctionGroup");
        JobRole jobRole = new JobRole().functionGroups(singletonList(functionGroup)).name("someJobRole");
        User adminUser = new User().internalId("someAdminInId").externalId(adminExId);
        legalEntity = new LegalEntity().internalId(leInternalId)
            .externalId(leExternalId)
            .addAdministratorsItem(adminUser)
            .parentExternalId(leParentExternalId);
        customSa = new ServiceAgreementV2()
            .externalId(customSaExId)
            .isMaster(false)
            .addJobRolesItem(jobRole)
            .creatorLegalEntity(leExternalId)
            .addJobProfileUsersItem(regularUser)
            .participants(singletonList(new LegalEntityParticipant()
                .externalId(leExternalId)
                .sharingUsers(true)
                .sharingAccounts(true)))
            .productGroups(singletonList(productGroup));

        ServiceAgreementTaskV2 task = mockServiceAgreementTask(customSa);

        when(legalEntityService.getLegalEntityByExternalId(eq(leExternalId))).thenReturn(Mono.empty());
        when(legalEntityService.getLegalEntityByInternalId(eq(leInternalId))).thenReturn(Mono.just(legalEntity));
        when(accessGroupService.getServiceAgreementByExternalId(any())).thenReturn(Mono.empty());
        when(accessGroupService.createServiceAgreement(any(), any()))
            .thenReturn(Mono.just(transformServiceAgreement(customSa)));
        when(accessGroupService.setupJobRole(any(), any(), any())).thenReturn(Mono.just(jobRole));
        when(accessGroupService.updateServiceAgreementRegularUsers(any(), any(),
            any())).thenReturn(Mono.just(transformServiceAgreement(customSa)));
        when(accessGroupService.getUserByExternalId(any(), anyBoolean())).thenReturn(Mono.just(new GetUser()
            .externalId(regularUser.getUser().getExternalId())
            .id(regularUser.getUser().getInternalId())));
        when(batchProductIngestionSaga.process(any(ProductGroupTask.class))).thenReturn(productGroupTaskMono);

        ServiceAgreementTaskV2 result = serviceAgreementSaga.executeTask(task)
            .block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(customSaExId, result.getData().getExternalId());

        verify(accessGroupService).createServiceAgreement(eq(task), eq(transformServiceAgreement(customSa)));
        verify(accessGroupService).updateServiceAgreementRegularUsers(eq(task), eq(transformServiceAgreement(customSa)), any());
        verify(accessGroupService).setupJobRole(eq(task), eq(transformServiceAgreement(customSa)), eq(jobRole));

    }

    @Test
    void customServiceAgreementCreationNoUsersOrAdministrators() {
        regularUser = new JobProfileUser().user(new User().internalId("someRegularUserInId")
            .externalId(regularUserExId)).legalEntityReference(new LegalEntityReference().externalId(leExternalId));
        SavingsAccount account = new SavingsAccount();
        account.externalId("someAccountExId").productTypeExternalId("Account").currency("GBP")
            .legalEntities(List.of(new LegalEntityReference().externalId(leExternalId)));
        ProductGroup productGroup = new ProductGroup();
        productGroup.productGroupType(BaseProductGroup.ProductGroupTypeEnum.ARRANGEMENTS).name("somePgName")
            .description("somePgDescription").savingAccounts(singletonList(account)).users(List.of(regularUser));;

        ProductGroupTask productGroupTask = new ProductGroupTask(productGroup);
        Mono<ProductGroupTask> productGroupTaskMono = Mono.just(productGroupTask);

        BusinessFunctionGroup functionGroup = new BusinessFunctionGroup().name("someFunctionGroup");
        JobRole jobRole = new JobRole().functionGroups(singletonList(functionGroup)).name("someJobRole");
        User adminUser = new User().internalId("someAdminInId").externalId(adminExId);

        legalEntity = new LegalEntity().internalId(leInternalId)
            .externalId(leExternalId)
            .addAdministratorsItem(adminUser)
            .parentExternalId(leParentExternalId);
        customSa = new ServiceAgreementV2()
            .externalId(customSaExId)
            .addJobRolesItem(jobRole)
            .isMaster(false)
            .addJobProfileUsersItem(regularUser)
            .creatorLegalEntity(leExternalId)
            .participants(singletonList(new LegalEntityParticipant()
                .externalId(leExternalId)
                .sharingUsers(true)
                .sharingAccounts(true)))
            .productGroups(singletonList(productGroup));

        ServiceAgreementTaskV2 task = mockServiceAgreementTask(customSa);

        when(accessGroupService.getServiceAgreementByExternalId(eq(customSaExId))).thenReturn(Mono.empty());
        when(accessGroupService.createServiceAgreement(any(), eq(transformServiceAgreement(customSa))))
            .thenReturn(Mono.just(transformServiceAgreement(customSa)));
        when(accessGroupService.setupJobRole(any(), any(), any())).thenReturn(Mono.just(jobRole));
        when(accessGroupService.createServiceAgreement(any(), any()))
            .thenReturn(Mono.just(transformServiceAgreement(customSa)));
        when(accessGroupService.updateServiceAgreementRegularUsers(any(), any(), any()))
            .thenReturn(Mono.just(transformServiceAgreement(customSa)));
        when(accessGroupService.getServiceAgreementParticipants(any(), any()))
            .thenReturn(Flux.just(new ServiceAgreementParticipantsGetResponseBody().externalId(customSaExId)));
        when(batchProductIngestionSaga.process(any(ProductGroupTask.class)))
            .thenReturn(productGroupTaskMono);
        when(accessGroupService.getUserByExternalId(any(), anyBoolean())).thenReturn(Mono.just(new GetUser()
            .externalId(regularUser.getUser().getExternalId())
            .id(regularUser.getUser().getInternalId())));
        when(limitsSaga.executeTask(any(LimitsTask.class)))
            .thenReturn(Mono.just(new LimitsTask("1", new CreateLimitRequestBody())));

        Mono<ServiceAgreementTaskV2> result = serviceAgreementSaga.executeTask(task);
        result.block();

        Assertions.assertNotNull(result);
    }

    /**
     * Intention of this test is to verify that {@link ProductGroupTask} are processed in order at
     *  (in short that .concatMap is used instead of .flatMap).
     * Otherwise it may happen that during permission assignment at
     * {@link AccessGroupService#assignPermissionsBatch(BatchProductGroupTask, Map)} there will be stale set of
     * permissions which will lead to state when not all of desired applied
     */
    @Test
    void productGroupsProcessedSequentially() {
        // Given
        Limit limit = new Limit().currencyCode("GBP").transactional(BigDecimal.valueOf(10000));
        customSa = new ServiceAgreementV2()
            .name("Custom SA")
            .externalId("100000")
            .isMaster(false)
            .referenceJobRoles(Collections.singletonList((ReferenceJobRole) new ReferenceJobRole()
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
                            .legalEntityReference(new LegalEntityReference().externalId(leExternalId))
                    ))
                    .currentAccounts(singletonList(
                        (CurrentAccount) new CurrentAccount()
                            .BBAN("01318000")
                            .externalId("7155000")
                            .productTypeExternalId("privateCurrentAccount")
                            .name("Account 1")
                            .currency("GBP")
                            .legalEntities(List.of(new LegalEntityReference().externalId(leExternalId)))
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
                            .legalEntityReference(new LegalEntityReference().externalId(leExternalId))
                    ))
                    .currentAccounts(singletonList(
                        (CurrentAccount) new CurrentAccount()
                            .BBAN("01318001")
                            .externalId("7155001")
                            .productTypeExternalId("privateCurrentAccount")
                            .name("Account 2")
                            .currency("GBP")
                            .legalEntities(List.of(new LegalEntityReference().externalId(leExternalId)))
                    ))
                    .loans(singletonList((Loan) new Loan()
                        .IBAN("IBAN123321")
                        .legalEntities(List.of(new LegalEntityReference().externalId(leExternalId)))
            ))))
                .addJobProfileUsersItem(new JobProfileUser()
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
                .legalEntityReference(new LegalEntityReference().externalId(leExternalId))
            )
            .addParticipantsItem(new LegalEntityParticipant()
                .externalId(leExternalId)
                .sharingAccounts(true)
                .sharingUsers(true)
            );

        ServiceAgreementTaskV2 serviceAgreementTaskV2 = new ServiceAgreementTaskV2(customSa);

        List<String> productGroupTaskProcessingOrder = new CopyOnWriteArrayList<>();

        when(accessGroupService.getServiceAgreementByExternalId("100000"))
            .thenReturn(Mono.just(new ServiceAgreement().internalId("101").externalId("100000")));
        lenient().when(accessGroupService.updateServiceAgreementItem(any(), any()))
                .thenReturn(Mono.just(new ServiceAgreement().internalId("101").externalId("100000")));
        when(accessGroupService.updateServiceAgreementAssociations(any(), any(), any()))
            .thenReturn(Mono.just(new ServiceAgreement().internalId("101").externalId("100000")));
        when(accessGroupService.createServiceAgreement(any(), any()))
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
        serviceAgreementSaga.executeTask(serviceAgreementTaskV2)
            .block(Duration.ofSeconds(20));

        // Then
        Assertions.assertEquals(
            Arrays.asList("100000-Default PG", "100000-Mixed PG"),
            productGroupTaskProcessingOrder
        );
    }

    @Test
    void test_PostSAContacts() {
        getMockServiceAgreement();
        ServiceAgreementTaskV2 task = mockServiceAgreementTask(customSa);
        when(contactsSaga.executeTask(any(ContactsTask.class))).thenReturn(getContactsTask(AccessContextScope.SA));
        when(accessGroupService.createServiceAgreement(any(), any()))
            .thenReturn(Mono.just(transformServiceAgreement(customSa)));
        when(accessGroupService.updateServiceAgreementRegularUsers(any(), any(),
            any())).thenReturn(Mono.just(transformServiceAgreement(customSa)));

        ServiceAgreementTaskV2 result = serviceAgreementSaga.executeTask(task).block();

        Assertions.assertNotNull(result);

        ExternalContact contact = getMockExternalContact();
        customSa.setContacts(Collections.singletonList(contact));
        result = serviceAgreementSaga.executeTask(task).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(leExternalId, result.getData().getContacts().get(0).getExternalId());

        LegalEntityParticipant participant = new LegalEntityParticipant()
                .externalId(leExternalId)
                .sharingUsers(true)
                .users(singletonList("USER1"));
        customSa.setParticipants(singletonList(participant));
        result = serviceAgreementSaga.executeTask(task).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals("USER1", result.getData().getParticipants().get(0).getUsers().get(0));

        participant = participant.users(null).admins(singletonList("ADMIN1"));
        customSa.setParticipants(singletonList(participant));
        result = serviceAgreementSaga.executeTask(task).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals("ADMIN1", result.getData().getParticipants().get(0).getAdmins().get(0));
    }


    @Test
    void test_PostUserContacts() {
        getMockServiceAgreement();
        ServiceAgreementTaskV2 task = mockServiceAgreementTask(customSa);

        when(contactsSaga.executeTask(any(ContactsTask.class))).thenReturn(getContactsTask(AccessContextScope.USER));

        ServiceAgreementTaskV2 result = serviceAgreementSaga.executeTask(task).block();

        Assertions.assertNotNull(result);

        ExternalContact contact = getMockExternalContact();
        regularUser.setContacts(Collections.singletonList(contact));
        result = serviceAgreementSaga.executeTask(task).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(leExternalId,
            result.getData().getJobProfileUsers().get(0).getContacts().get(0).getExternalId());
    }

    private ServiceAgreementTaskV2 mockServiceAgreementTask(ServiceAgreementV2 serviceAgreement) {
        ServiceAgreementTaskV2 task = Mockito.mock(ServiceAgreementTaskV2.class);
        when(task.getData()).thenReturn(serviceAgreement);
        when(task.getServiceAgreement()).thenReturn(serviceAgreement);
        when(task.data(any())).thenReturn(task);
        when(task.addHistory(any())).thenReturn(task);
        return task;
    }

    void getMockServiceAgreement() {
        regularUser = new JobProfileUser().user(new User().internalId("someRegularUserInId")
            .externalId(regularUserExId))
            .legalEntityReference(new LegalEntityReference().externalId(leExternalId));

        SavingsAccount account = new SavingsAccount();
        account.externalId("someAccountExId").productTypeExternalId("Account").currency("GBP")
        .legalEntities(List.of(new LegalEntityReference().externalId(leExternalId)));
        ProductGroup productGroup = new ProductGroup();
        productGroup.productGroupType(BaseProductGroup.ProductGroupTypeEnum.ARRANGEMENTS).name("somePgName")
            .description("somePgDescription").savingAccounts(singletonList(account)).users(List.of(regularUser));;

        ProductGroupTask productGroupTask = new ProductGroupTask(productGroup);
        Mono<ProductGroupTask> productGroupTaskMono = Mono.just(productGroupTask);

        BusinessFunctionGroup functionGroup = new BusinessFunctionGroup().name("someFunctionGroup");
        JobRole jobRole = new JobRole().functionGroups(singletonList(functionGroup)).name("someJobRole");
        customSa = new ServiceAgreementV2()
            .externalId(customSaExId)
            .addJobProfileUsersItem(regularUser)
            .addJobRolesItem(jobRole)
            .participants(singletonList(new LegalEntityParticipant()
                .externalId(leExternalId)
                .sharingUsers(true)
                .sharingAccounts(true)))
            .creatorLegalEntity(leExternalId);
        User adminUser = new User().internalId("someAdminInId").externalId(adminExId);

        when(accessGroupService.getServiceAgreementByExternalId(customSaExId)).thenReturn(Mono.empty());
        when(accessGroupService.createServiceAgreement(any(), any()))
            .thenReturn(Mono.just(transformServiceAgreement(customSa)));
        when(accessGroupService.setupJobRole(any(), any(), any())).thenReturn(Mono.just(jobRole));
        when(accessGroupService.updateServiceAgreementRegularUsers(any(), eq(transformServiceAgreement(customSa)),
            any())).thenReturn(Mono.just(transformServiceAgreement(customSa)));
        when(batchProductIngestionSaga.process(any(ProductGroupTask.class))).thenReturn(productGroupTaskMono);
        when(accessGroupService.getUserByExternalId(any(), anyBoolean())).thenReturn(Mono.just(new GetUser()
            .externalId(regularUser.getUser().getExternalId())
            .id(regularUser.getUser().getInternalId())));
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

    private ServiceAgreement transformServiceAgreement(ServiceAgreementV2 serviceAgreementV2) {
        ServiceAgreement serviceAgreement = new ServiceAgreement();
        // Copy common fields
        serviceAgreement.setInternalId(serviceAgreementV2.getInternalId());
        serviceAgreement.setExternalId(serviceAgreementV2.getExternalId());
        serviceAgreement.setName(serviceAgreementV2.getName());
        serviceAgreement.setDescription(serviceAgreementV2.getDescription());
        serviceAgreement.setParticipants(serviceAgreementV2.getParticipants());
        serviceAgreement.setValidFromDate(serviceAgreementV2.getValidFromDate());
        serviceAgreement.setValidFromTime(serviceAgreementV2.getValidFromTime());
        serviceAgreement.setValidUntilDate(serviceAgreementV2.getValidUntilDate());
        serviceAgreement.setValidUntilTime(serviceAgreementV2.getValidUntilTime());
        serviceAgreement.setStatus(serviceAgreementV2.getStatus());
        serviceAgreement.setIsMaster(serviceAgreementV2.getIsMaster());
        serviceAgreement.setRegularUserAps(serviceAgreementV2.getRegularUserAps());
        serviceAgreement.setAdminUserAps(serviceAgreementV2.getAdminUserAps());
        serviceAgreement.setJobRoles(serviceAgreementV2.getJobRoles());
        serviceAgreement.setCreatorLegalEntity(serviceAgreementV2.getCreatorLegalEntity());
        serviceAgreement.setLimit(serviceAgreementV2.getLimit());
        serviceAgreement.setContacts(serviceAgreementV2.getContacts());
        serviceAgreement.setAdditions(serviceAgreementV2.getAdditions());

        return serviceAgreement;
    }

    private LegalEntitySagaConfigurationProperties getLegalEntitySagaConfigurationProperties() {
        LegalEntitySagaConfigurationProperties sagaConfiguration =  new LegalEntitySagaConfigurationProperties();
        sagaConfiguration.setUseIdentityIntegration(true);
        sagaConfiguration.setUserProfileEnabled(true);
        return sagaConfiguration;
    }

}
