package com.backbase.stream;

import static com.backbase.stream.FixtureUtils.reflectiveAlphaFixtureMonkey;
import static com.backbase.stream.mapper.UserToCdpEventMapper.PROFILE_CREATED_EVENT;
import static com.backbase.stream.service.UserService.REMOVED_PREFIX;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.cdp.ingestion.api.service.v1.model.CdpEvent;
import com.backbase.customerprofile.api.integration.v1.model.PartyResponseUpsertDto;
import com.backbase.dbs.contact.api.service.v2.model.AccessContextScope;
import com.backbase.dbs.contact.api.service.v2.model.ContactsBulkPostRequestBody;
import com.backbase.dbs.contact.api.service.v2.model.ContactsBulkPostResponseBody;
import com.backbase.dbs.contact.api.service.v2.model.ExternalAccessContext;
import com.backbase.dbs.contact.api.service.v2.model.IngestMode;
import com.backbase.dbs.user.api.service.v2.model.GetUser;
import com.backbase.dbs.user.api.service.v2.model.GetUsersList;
import com.backbase.dbs.user.api.service.v2.model.Realm;
import com.backbase.stream.audiences.UserKindSegmentationSaga;
import com.backbase.stream.audiences.UserKindSegmentationTask;
import com.backbase.stream.cdp.CdpSaga;
import com.backbase.stream.cdp.CdpTask;
import com.backbase.stream.configuration.LegalEntitySagaConfigurationProperties;
import com.backbase.stream.contact.ContactsSaga;
import com.backbase.stream.contact.ContactsTask;
import com.backbase.stream.legalentity.model.Address;
import com.backbase.stream.legalentity.model.CustomerCategory;
import com.backbase.stream.legalentity.model.EmailAddress;
import com.backbase.stream.legalentity.model.ExternalAccountInformation;
import com.backbase.stream.legalentity.model.ExternalContact;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.LegalEntityType;
import com.backbase.stream.legalentity.model.LegalEntityV2;
import com.backbase.stream.legalentity.model.Name;
import com.backbase.stream.legalentity.model.Party;
import com.backbase.stream.legalentity.model.PersonalInformation;
import com.backbase.stream.legalentity.model.PhoneNumber;
import com.backbase.stream.legalentity.model.SavingsAccount;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.legalentity.model.ServiceAgreementV2;
import com.backbase.stream.legalentity.model.User;
import com.backbase.stream.legalentity.model.UserProfile;
import com.backbase.stream.mapper.LegalEntityV2toV1Mapper;
import com.backbase.stream.mapper.ServiceAgreementV2ToV1Mapper;
import com.backbase.stream.service.AccessGroupService;
import com.backbase.stream.service.CustomerProfileService;
import com.backbase.stream.service.LegalEntityService;
import com.backbase.stream.service.UserService;
import com.backbase.stream.worker.exception.StreamTaskException;
import com.navercorp.fixturemonkey.FixtureMonkey;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LegalEntitySagaV2Test {

    @InjectMocks
    private LegalEntitySagaV2 legalEntitySaga;

    @Mock
    private LegalEntityService legalEntityService;

    @Mock
    private UserService userService;

    @Mock
    private AccessGroupService accessGroupService;

    @Mock
    private CustomerAccessGroupSaga customerAccessGroupSaga;

    @Mock
    private ContactsSaga contactsSaga;

    @Mock
    private UserKindSegmentationSaga userKindSegmentationSaga;

    @Mock
    private CdpSaga cdpSaga;

    @Mock
    private CustomerProfileService customerProfileService;

    @Spy
    private final LegalEntitySagaConfigurationProperties legalEntitySagaConfigurationProperties =
        getLegalEntitySagaConfigurationProperties();

    private final FixtureMonkey fixtureMonkey = reflectiveAlphaFixtureMonkey;

    private static final int PARTY_SIZE = 20;

    String leExternalId = "someLeExternalId";
    String leParentExternalId = "someParentLeExternalId";
    String leInternalId = "someLeInternalId";
    String adminExId = "someAdminExId";
    String regularUserExId = "someRegularUserExId";
    LegalEntityV2 legalEntityV2;
    User regularUser;

    private final static LegalEntityV2toV1Mapper leMapper = Mappers.getMapper(LegalEntityV2toV1Mapper.class);
    private final static ServiceAgreementV2ToV1Mapper saMapper = Mappers.getMapper(ServiceAgreementV2ToV1Mapper.class);

    @Test
    void masterServiceAgreementCreation() {
        ServiceAgreement sa = new ServiceAgreement().creatorLegalEntity(leExternalId);
        legalEntityV2 = new LegalEntityV2().internalId(leInternalId).externalId(leExternalId)
            .parentExternalId(leExternalId)
            .activateSingleServiceAgreement(false).masterServiceAgreement(saMapper.mapV2(sa));
        LegalEntity legalEntity = leMapper.mapLegalEntityV2ToLegalEntity(legalEntityV2);

        LegalEntityTaskV2 task = mockLegalEntityTask(legalEntityV2);

        when(task.getLegalEntityV2()).thenReturn(legalEntityV2);
        when(legalEntityService.getLegalEntityByExternalId(leExternalId)).thenReturn(Mono.empty());
        when(legalEntityService.getLegalEntityByInternalId(leInternalId)).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.getMasterServiceAgreementForInternalLegalEntityId(leInternalId)).thenReturn(
            Mono.just(sa));
        when(legalEntityService.createLegalEntity(any())).thenReturn(Mono.just(legalEntity));
        when(userService.setupRealm(legalEntity))
            .thenReturn(Mono.just(new Realm()));
        when(userService.linkLegalEntityToRealm(legalEntity))
            .thenReturn(Mono.just(legalEntity));

        Mono<LegalEntityTaskV2> result = legalEntitySaga.executeTask(task);
        result.block();

        verify(userService).setupRealm(legalEntity);
        verify(userService).linkLegalEntityToRealm(legalEntity);

        when(legalEntityService.getLegalEntityByExternalId(leExternalId)).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.putLegalEntity(any())).thenReturn(Mono.just(legalEntity));
        result = legalEntitySaga.executeTask(task);
        result.block();

        verify(userService, times(2)).setupRealm(legalEntity);
        verify(userService, times(2)).linkLegalEntityToRealm(legalEntity);
    }

    @Test
    void masterServiceAgreementCreation_activateSingleServiceAgreement() {
        SavingsAccount account = new SavingsAccount();
        account.externalId("someAccountExId").productTypeExternalId("Account").currency("GBP");
        legalEntityV2 = new LegalEntityV2().internalId(leInternalId).externalId(leExternalId)
            .parentExternalId(leExternalId);
        ServiceAgreement sa = new ServiceAgreement().creatorLegalEntity(leExternalId);
        LegalEntityTaskV2 task = mockLegalEntityTask(legalEntityV2);
        LegalEntity legalEntity = leMapper.mapLegalEntityV2ToLegalEntity(legalEntityV2);

        when(task.getLegalEntityV2()).thenReturn(legalEntityV2);
        when(legalEntityService.getLegalEntityByExternalId(leExternalId)).thenReturn(Mono.empty());
        when(legalEntityService.getLegalEntityByInternalId(leInternalId)).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.getMasterServiceAgreementForInternalLegalEntityId(leInternalId)).thenReturn(
            Mono.empty());
        when(legalEntityService.createLegalEntity(any())).thenReturn(Mono.just(legalEntity));
        when(accessGroupService.createServiceAgreement(any(), any())).thenReturn(Mono.just(sa));
        when(userService.setupRealm(leMapper.mapLegalEntityV2ToLegalEntity(task.getLegalEntityV2())))
            .thenReturn(Mono.just(new Realm()));
        when(userService.linkLegalEntityToRealm(leMapper.mapLegalEntityV2ToLegalEntity(task.getLegalEntityV2())))
            .thenReturn(Mono.just(new LegalEntity()));

        Mono<LegalEntityTaskV2> result = legalEntitySaga.executeTask(task);
        result.block();

        verify(userService).setupRealm(legalEntity);
        verify(userService).linkLegalEntityToRealm(legalEntity);
        when(userService.setupRealm(leMapper.mapLegalEntityV2ToLegalEntity(task.getLegalEntityV2())))
            .thenReturn(Mono.just(new Realm()));
        when(userService.linkLegalEntityToRealm(leMapper.mapLegalEntityV2ToLegalEntity(task.getLegalEntityV2())))
            .thenReturn(Mono.just(new LegalEntity()));
        when(legalEntityService.getLegalEntityByExternalId(leExternalId)).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.putLegalEntity(any())).thenReturn(Mono.just(legalEntity));
        result = legalEntitySaga.executeTask(task);
        result.block();

        verify(userService, times(2)).setupRealm(leMapper.mapLegalEntityV2ToLegalEntity(task.getLegalEntityV2()));
        verify(userService, times(2)).linkLegalEntityToRealm(
            leMapper.mapLegalEntityV2ToLegalEntity(task.getLegalEntityV2()));
    }

    @Test
    void deleteLegalEntity_usersPrefixedWithRemovedNotProcessed() {
        legalEntityV2 = new LegalEntityV2().internalId(leInternalId).externalId(leExternalId)
            .parentExternalId(leExternalId);
        ServiceAgreement sa = new ServiceAgreement().creatorLegalEntity(leExternalId);

        when(legalEntityService.getMasterServiceAgreementForExternalLegalEntityId(leExternalId)).thenReturn(
            Mono.just(sa));
        when(legalEntityService.getLegalEntityByExternalId(leExternalId)).thenReturn(
            Mono.just(leMapper.mapLegalEntityV2ToLegalEntity(legalEntityV2)));

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

        when(accessGroupService.deleteFunctionGroupsForServiceAgreement(any(), any())).thenReturn(Mono.empty());
        when(accessGroupService.deleteAdmins((ServiceAgreementV2) any())).thenReturn(Mono.empty());
        when(userService.archiveUsers(any(), any())).thenReturn(Mono.empty());
        when(legalEntityService.deleteLegalEntity(any())).thenReturn(Mono.empty());

        Mono<Void> result = legalEntitySaga.deleteLegalEntity(leExternalId);
        result.block();

        verify(accessGroupService, times(3)).removePermissionsForUser(any(), any());
    }

    @Test
    void deleteLegalEntity_processesPaginatedListOfUsers() {
        legalEntityV2 = new LegalEntityV2().internalId(leInternalId).externalId(leExternalId)
            .parentExternalId(leExternalId);
        ServiceAgreement sa = new ServiceAgreement().creatorLegalEntity(leExternalId);

        when(legalEntityService.getMasterServiceAgreementForExternalLegalEntityId(leExternalId)).thenReturn(
            Mono.just(sa));
        when(legalEntityService.getLegalEntityByExternalId(leExternalId)).thenReturn(
            Mono.just(leMapper.mapLegalEntityV2ToLegalEntity(legalEntityV2)));

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

        when(accessGroupService.deleteFunctionGroupsForServiceAgreement(any(), any())).thenReturn(Mono.empty());
        when(accessGroupService.deleteAdmins((ServiceAgreementV2) any())).thenReturn(Mono.empty());
        when(userService.archiveUsers(any(), any())).thenReturn(Mono.empty());
        when(legalEntityService.deleteLegalEntity(any())).thenReturn(Mono.empty());

        Mono<Void> result = legalEntitySaga.deleteLegalEntity(leExternalId);
        result.block();

        verify(userService, times(3)).getUsersByLegalEntity(eq(leInternalId), anyInt(), anyInt());
    }

    @Test
    void updateLegalEntityName() {
        regularUser = new User().internalId("someRegularUserInId")
            .externalId(regularUserExId);
        User adminUser = new User().internalId("someAdminInId").externalId(adminExId);
        ServiceAgreement sa = new ServiceAgreement().creatorLegalEntity(leExternalId);
        legalEntityV2 = new LegalEntityV2().internalId(leInternalId).externalId(leExternalId)
            .name("Model Bank")
            .addAdministratorsItem(adminUser).parentExternalId(leParentExternalId)
            .users(singletonList(regularUser))
            .subsidiaries(singletonList(
                new LegalEntityV2().externalId(leExternalId)));

        LegalEntityV2 newLE = new LegalEntityV2().internalId(leInternalId).externalId(leExternalId)
            .name("New Model Bank")
            .addAdministratorsItem(adminUser).parentExternalId(leParentExternalId)
            .users(singletonList(regularUser))
            .subsidiaries(singletonList(
                new LegalEntityV2().externalId(leExternalId)
            ));

        LegalEntityTaskV2 task = mockLegalEntityTask(newLE);

        LegalEntity legalEntity = leMapper.mapLegalEntityV2ToLegalEntity(legalEntityV2);

        when(legalEntityService.getMasterServiceAgreementForInternalLegalEntityId(leInternalId)).thenReturn(
            Mono.just(sa));
        when(accessGroupService.createServiceAgreement(any(), any())).thenReturn(Mono.just(sa));
        when(legalEntityService.getLegalEntityByExternalId(leExternalId)).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.getLegalEntityByInternalId(leInternalId)).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.putLegalEntity(any())).thenReturn(
            Mono.just(leMapper.mapLegalEntityV2ToLegalEntity(newLE)));
        when(userService.getUserByExternalId(regularUserExId)).thenReturn(Mono.just(regularUser));
        when(userService.getUserByExternalId(adminExId)).thenReturn(Mono.just(adminUser));
        when(userService.createUser(any(), any(), any())).thenReturn(Mono.empty());
        when(userService.setupRealm(any())).thenReturn(Mono.just(new Realm()));
        when(userService.linkLegalEntityToRealm(any())).thenReturn(Mono.just(new LegalEntity()));
        when(userService.updateUser(any())).thenReturn(Mono.empty());

        LegalEntityTaskV2 result = legalEntitySaga.executeTask(task)
            .block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(newLE.getName(), result.getData().getName());
    }

    @Test
    void updateUserName() {
        User newRegularUser = new User().internalId("someRegularUserInId")
            .externalId(regularUserExId).fullName("New Name Regular User");
        User adminUser = new User().internalId("someAdminInId").externalId(adminExId).fullName("Admin");
        ServiceAgreement sa = new ServiceAgreement().creatorLegalEntity(leExternalId);
        LegalEntityV2 legalEntityV2 = new LegalEntityV2().internalId(leInternalId).externalId(leExternalId)
            .name("Model Bank")
            .addAdministratorsItem(adminUser).parentExternalId(leParentExternalId)
            .users(singletonList(newRegularUser))
            .subsidiaries(singletonList(
                new LegalEntityV2().externalId(leExternalId).internalId("internalSubsidiary")
            ));

        LegalEntity legalEntity = leMapper.mapLegalEntityV2ToLegalEntity(legalEntityV2);

        LegalEntityTaskV2 task = mockLegalEntityTask(legalEntityV2);
        when(legalEntityService.getMasterServiceAgreementForInternalLegalEntityId(leInternalId)).thenReturn(
            Mono.just(sa));
        when(legalEntityService.getMasterServiceAgreementForInternalLegalEntityId("internalSubsidiary")).thenReturn(
            Mono.just(sa));
        when(accessGroupService.createServiceAgreement(any(), any())).thenReturn(Mono.just(sa));
        when(legalEntityService.getLegalEntityByExternalId(leExternalId)).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.getLegalEntityByInternalId(leInternalId)).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.createLegalEntity(any())).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.putLegalEntity(any())).thenReturn(Mono.empty());
        when(userService.getUserByExternalId(adminExId)).thenReturn(Mono.just(adminUser));
        when(userService.getUserByExternalId(regularUserExId)).thenReturn(Mono.just(newRegularUser));
        when(userService.createUser(any(), any(), any())).thenReturn(Mono.just(adminUser));
        when(userService.updateUser(any())).thenReturn(Mono.empty());
        when(userService.setupRealm(any())).thenReturn(Mono.just(new Realm()));
        when(userService.linkLegalEntityToRealm(any())).thenReturn(Mono.just(new LegalEntity()));
        when(userService.updateUser(any())).thenReturn(Mono.just(newRegularUser));

        LegalEntityTaskV2 result = legalEntitySaga.executeTask(task)
            .block();

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getData().getUsers());
        Assertions.assertNotNull(result.getData().getUsers().getFirst());
        Assertions.assertEquals(newRegularUser.getFullName(),
            result.getData().getUsers().getFirst().getFullName());
    }

    void getMockLegalEntity() {
        String userInternalId = UUID.randomUUID().toString();
        regularUser = new User().internalId(userInternalId)
            .externalId(regularUserExId)
            .fullName("User Full Name")
            .userProfile(new UserProfile()
                .userId(userInternalId)
                .externalId(regularUserExId)
                .name(new Name()
                    .familyName("User")
                    .givenName("Name")
                    .middleName("Full")
                    .formatted("Full Name User"))
                .active(true)
                .addresses(List.of(new Address()
                    .streetAddress("Some street 1")
                    .locality("Some city")
                    .postalCode("02147")
                    .country("Some country")))
                .personalInformation(new PersonalInformation()
                    .dateOfBirth("1990-01-01")
                    .employer("BACKBASE")
                    .gender("male")
                    .maritalStatus("single")))
            .legalEntityId(leInternalId)
            .emailAddress(new EmailAddress("home", "home", regularUserExId.concat("@domain.com")))
            .mobileNumber(new PhoneNumber("home", "home", "1234567890"));
        User adminUser = new User().internalId("someAdminInId").externalId(adminExId);
        ServiceAgreement sa = new ServiceAgreement().creatorLegalEntity(leExternalId);
        legalEntityV2 = new LegalEntityV2()
            .internalId(leInternalId)
            .externalId(leExternalId)
            .legalEntityType(LegalEntityType.CUSTOMER)
            .name("le-name")
            .customerCategory(CustomerCategory.RETAIL)
            .parties(fixtureMonkey.giveMe(Party.class, PARTY_SIZE))
            .addAdministratorsItem(adminUser)
            .parentExternalId(leParentExternalId)
            .users(singletonList(regularUser))
            .subsidiaries(singletonList(
                new LegalEntityV2().externalId(leExternalId).internalId("internalSubsidiaries")
            ));

        LegalEntity legalEntity = leMapper.mapLegalEntityV2ToLegalEntity(legalEntityV2);

        when(legalEntityService.getMasterServiceAgreementForInternalLegalEntityId(leInternalId)).thenReturn(
            Mono.just(sa));
        when(legalEntityService.getMasterServiceAgreementForInternalLegalEntityId("internalSubsidiaries")).thenReturn(
            Mono.just(sa));
        when(accessGroupService.createServiceAgreement(any(), any())).thenReturn(Mono.just(sa));
        when(legalEntityService.getLegalEntityByExternalId(leExternalId)).thenReturn(Mono.empty());
        when(legalEntityService.getLegalEntityByInternalId(leInternalId)).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.createLegalEntity(any())).thenReturn(Mono.just(legalEntity));
        when(userService.getUserByExternalId(regularUserExId)).thenReturn(Mono.just(regularUser));
        when(userService.getUserByExternalId(adminExId)).thenReturn(Mono.just(adminUser));
        when(userService.createUser(any(), any(), any())).thenReturn(Mono.empty());
        when(userService.setupRealm(any())).thenReturn(Mono.just(new Realm()));
        when(userService.linkLegalEntityToRealm(any())).thenReturn(Mono.just(new LegalEntity()));
        when(userService.updateUser(any())).thenReturn(Mono.just(regularUser));
    }

    @Test
    void test_PostLegalContacts() {
        getMockLegalEntity();
        LegalEntityTaskV2 task = mockLegalEntityTask(legalEntityV2);
        when(contactsSaga.executeTask(any(ContactsTask.class))).thenReturn(getContactsTask(AccessContextScope.LE));

        when(customerProfileService.upsertParty(any(Party.class), anyString())).thenReturn(
            Mono.just(fixtureMonkey.giveMeOne(PartyResponseUpsertDto.class)));

        LegalEntityTaskV2 result = legalEntitySaga.executeTask(task).block();

        Assertions.assertNotNull(result);

        ExternalContact contact = getMockExternalContact();
        legalEntityV2.setContacts(Collections.singletonList(contact));
        result = legalEntitySaga.executeTask(task).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(leExternalId, result.getData().getContacts().getFirst().getExternalId());
    }

    @Test
    void test_PostLegalContacts_NoUser() {

        regularUser = new User().internalId("someRegularUserInId")
            .externalId(regularUserExId);
        User adminUser = new User().internalId("someAdminInId").externalId(adminExId);

        ExternalContact contact = getMockExternalContact();
        ServiceAgreement sa = new ServiceAgreement().creatorLegalEntity(leExternalId);
        legalEntityV2 = new LegalEntityV2()
            .internalId(leInternalId)
            .externalId(leExternalId)
            .addAdministratorsItem(adminUser)
            .parentExternalId(leParentExternalId)
            .contacts(Collections.singletonList(contact))
            .subsidiaries(singletonList(
                new LegalEntityV2().externalId(leExternalId).internalId("internalSubsidiary")
            ));

        LegalEntity legalEntity = leMapper.mapLegalEntityV2ToLegalEntity(legalEntityV2);

        LegalEntityTaskV2 task = mockLegalEntityTask(legalEntityV2);
        when(legalEntityService.getMasterServiceAgreementForInternalLegalEntityId("internalSubsidiary")).thenReturn(
            Mono.just(sa));
        when(legalEntityService.getMasterServiceAgreementForInternalLegalEntityId(leInternalId)).thenReturn(
            Mono.just(sa));
        when(accessGroupService.createServiceAgreement(any(), any())).thenReturn(Mono.just(sa));
        when(legalEntityService.getLegalEntityByExternalId(leExternalId)).thenReturn(Mono.empty());
        when(legalEntityService.getLegalEntityByInternalId(leInternalId)).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.createLegalEntity(any())).thenReturn(Mono.just(legalEntity));
        when(userService.getUserByExternalId(adminExId)).thenReturn(Mono.just(adminUser));
        when(userService.createUser(any(), any(), any())).thenReturn(Mono.empty());
        when(userService.setupRealm(any())).thenReturn(Mono.just(new Realm()));
        when(userService.linkLegalEntityToRealm(any())).thenReturn(Mono.just(new LegalEntity()));
        when(userService.updateUser(any())).thenReturn(Mono.just(regularUser));

        LegalEntityTaskV2 result = legalEntitySaga.executeTask(task).block();

        Assertions.assertNotNull(result);
    }

    @Test
    void test_PostUserContacts() {
        getMockLegalEntity();
        LegalEntityTaskV2 task = mockLegalEntityTask(legalEntityV2);

        when(contactsSaga.executeTask(any(ContactsTask.class))).thenReturn(getContactsTask(AccessContextScope.USER));

        when(customerProfileService.upsertParty(any(Party.class), anyString())).thenReturn(
            Mono.just(fixtureMonkey.giveMeOne(PartyResponseUpsertDto.class)));

        LegalEntityTaskV2 result = legalEntitySaga.executeTask(task).block();

        Assertions.assertNotNull(result);

        result = legalEntitySaga.executeTask(task).block();
        Assertions.assertNotNull(result);
    }

    @Test
    void userKindSegmentationIsDisabled() {
        getMockLegalEntity();

        when(userKindSegmentationSaga.isEnabled()).thenReturn(false);

        when(customerProfileService.upsertParty(any(Party.class), anyString())).thenReturn(
            Mono.just(fixtureMonkey.giveMeOne(PartyResponseUpsertDto.class)));

        legalEntitySaga.executeTask(mockLegalEntityTask(legalEntityV2)).block();

        verify(userKindSegmentationSaga, never()).executeTask(Mockito.any());
    }

    @Test
    void userKindSegmentationUsesLegalEntityCustomerCategory() {
        getMockLegalEntity();
        legalEntityV2.setCustomerCategory(CustomerCategory.RETAIL);

        when(userKindSegmentationSaga.isEnabled()).thenReturn(true);

        when(customerProfileService.upsertParty(any(Party.class), anyString())).thenReturn(
            Mono.just(fixtureMonkey.giveMeOne(PartyResponseUpsertDto.class)));

        legalEntitySaga.executeTask(mockLegalEntityTask(legalEntityV2)).block();

        verify(userKindSegmentationSaga, times(0)).getDefaultCustomerCategory();
    }

    @Test
    void userKindSegmentationUsesDefaultCustomerCategory() {
        getMockLegalEntity();
        legalEntityV2.legalEntityType(LegalEntityType.CUSTOMER);

        when(userKindSegmentationSaga.isEnabled()).thenReturn(true);
        when(userKindSegmentationSaga.getDefaultCustomerCategory()).thenReturn(CustomerCategory.RETAIL.getValue());
        when(userKindSegmentationSaga.executeTask(any())).thenReturn(
            Mono.just(Mockito.mock(UserKindSegmentationTask.class)));
        when(customerProfileService.upsertParty(any(Party.class), anyString())).thenReturn(
            Mono.just(fixtureMonkey.giveMeOne(PartyResponseUpsertDto.class)));

        legalEntitySaga.executeTask(mockLegalEntityTask(legalEntityV2)).block();

        verify(userKindSegmentationSaga, times(1)).getDefaultCustomerCategory();
    }

    @Test
    void whenUserKindSegmentationIsEnabledAndNoCustomerCategoryCanBeDeterminedReturnsError() {
        getMockLegalEntity();
        legalEntityV2.legalEntityType(LegalEntityType.CUSTOMER);

        when(userKindSegmentationSaga.isEnabled()).thenReturn(true);
        when(userKindSegmentationSaga.getDefaultCustomerCategory()).thenReturn(null);

        when(customerProfileService.upsertParty(any(Party.class), anyString())).thenReturn(
            Mono.just(fixtureMonkey.giveMeOne(PartyResponseUpsertDto.class)));

        var task = mockLegalEntityTask(legalEntityV2);

        Assertions.assertThrows(
            StreamTaskException.class,
            () -> executeLegalEntityTaskAndBlock(task),
            "Failed to determine LE customerCategory for UserKindSegmentationSage."
        );
    }

    @Test
    void cdpIsDisabled() {
        getMockLegalEntity();

        when(cdpSaga.isEnabled()).thenReturn(false);

        when(customerProfileService.upsertParty(any(Party.class), anyString())).thenReturn(
            Mono.just(fixtureMonkey.giveMeOne(PartyResponseUpsertDto.class)));

        legalEntitySaga.executeTask(mockLegalEntityTask(legalEntityV2)).block();

        verify(cdpSaga, never()).executeTask(Mockito.any());
    }

    @Test
    void cdpIsEnabledTestMappings() {
        ArgumentCaptor<CdpTask> argumentCaptor = ArgumentCaptor.forClass(CdpTask.class);
        getMockLegalEntity();

        when(cdpSaga.isEnabled()).thenReturn(true);
        when(legalEntitySagaConfigurationProperties.isUserProfileEnabled()).thenReturn(false);

        legalEntityV2.setLegalEntityType(LegalEntityType.CUSTOMER);

        when(customerProfileService.upsertParty(any(Party.class), anyString())).thenReturn(
            Mono.just(fixtureMonkey.giveMeOne(PartyResponseUpsertDto.class)));

        when(cdpSaga.executeTask(any())).thenReturn(
            Mono.just(Mockito.mock(com.backbase.stream.cdp.CdpTask.class)));

        legalEntitySaga.executeTask(mockLegalEntityTask(legalEntityV2)).block();

        verify(cdpSaga, atLeastOnce()).executeTask(Mockito.any());
        verify(cdpSaga).executeTask(argumentCaptor.capture());
        CdpTask capturedCdpTask = argumentCaptor.getValue();
        assertThat(capturedCdpTask).isNotNull();
        assertThat(capturedCdpTask.getCdpEvents()).isNotNull();
        assertThat(capturedCdpTask.getCdpEvents().getCdpEvents()).isNotNull().hasSize(1);
        CdpEvent cdpEvent = capturedCdpTask.getCdpEvents().getCdpEvents().getFirst();
        assertThat(cdpEvent.getEventType()).isEqualTo(PROFILE_CREATED_EVENT);
    }

    @Test
    void testSetupParties_IfPartyFound_ThenUpsertParty() {
        getMockLegalEntity();
        var task = mockLegalEntityTask(legalEntityV2);
        when(contactsSaga.executeTask(any(ContactsTask.class))).thenReturn(getContactsTask(AccessContextScope.LE));
        when(customerProfileService.upsertParty(any(Party.class), anyString())).thenReturn(
            Mono.just(fixtureMonkey.giveMeOne(PartyResponseUpsertDto.class)));
        var result = legalEntitySaga.executeTask(task).block();
        verify(customerProfileService, times(PARTY_SIZE)).upsertParty(any(Party.class), anyString());
        Assertions.assertNotNull(result);
    }

    @Test
    void testProcessCustomerProfile_IfUpsertPartyError_ThenTrowException() {
        getMockLegalEntity();
        var task = mockLegalEntityTask(legalEntityV2);
        when(contactsSaga.executeTask(any(ContactsTask.class))).thenReturn(getContactsTask(AccessContextScope.LE));
        var mockException = new WebClientResponseException(
            "CPS Error",
            400,
            "Bad Request",
            null,
            null,
            null
        );
        when(customerProfileService.upsertParty(any(Party.class), anyString())).thenReturn(Mono.error(mockException));
        var result = legalEntitySaga.executeTask(task).block();
        verify(customerProfileService, times(PARTY_SIZE)).upsertParty(any(Party.class), anyString());
        Assertions.assertNotNull(result);
    }

    @ParameterizedTest
    @MethodSource("parameters_upster_error")
    void upster_error(Exception ex, String error) {

        // Given
        LegalEntityTaskV2 leTask = new LegalEntityTaskV2(new LegalEntityV2().externalId(leExternalId));
        when(legalEntityService.getLegalEntityByExternalId(leExternalId)).thenReturn(Mono.error(ex));

        // When
        Mono<LegalEntityTaskV2> result = legalEntitySaga.executeTask(leTask);
        StreamTaskException stEx = null;
        try {
            result.block();
        } catch (StreamTaskException e) {
            stEx = e;
        }

        // Then
        verify(legalEntityService).getLegalEntityByExternalId(any());
        assertThat(stEx)
            .isNotNull()
            .extracting(e -> e.getTask().getHistory().get(1).getErrorMessage())
            .isEqualTo(error);
    }

    private static Stream<Arguments> parameters_upster_error() {
        return Stream.of(
            Arguments.of(new RuntimeException("Fake error"), "Fake error"),
            Arguments.of(new WebClientResponseException(400, "Bad request", null,
                "Fake validation error".getBytes(), Charset.defaultCharset()), "Fake validation error"));
    }


    private LegalEntityTaskV2 executeLegalEntityTaskAndBlock(LegalEntityTaskV2 task) {
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

    private LegalEntityTaskV2 mockLegalEntityTask(LegalEntityV2 legalEntity) {
        LegalEntityTaskV2 task = Mockito.mock(LegalEntityTaskV2.class);
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
