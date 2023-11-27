package com.backbase.stream;

import static com.backbase.stream.service.UserService.REMOVED_PREFIX;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.dbs.contact.api.service.v2.model.AccessContextScope;
import com.backbase.dbs.contact.api.service.v2.model.ContactsBulkPostRequestBody;
import com.backbase.dbs.contact.api.service.v2.model.ContactsBulkPostResponseBody;
import com.backbase.dbs.contact.api.service.v2.model.ExternalAccessContext;
import com.backbase.dbs.contact.api.service.v2.model.IngestMode;
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
import com.backbase.stream.legalentity.model.BusinessFunctionGroup;
import com.backbase.stream.legalentity.model.CustomerCategory;
import com.backbase.stream.legalentity.model.EmailAddress;
import com.backbase.stream.legalentity.model.ExternalAccountInformation;
import com.backbase.stream.legalentity.model.ExternalContact;
import com.backbase.stream.legalentity.model.JobRole;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.LegalEntityParticipant;
import com.backbase.stream.legalentity.model.LegalEntityType;
import com.backbase.stream.legalentity.model.LegalEntityV2;
import com.backbase.stream.legalentity.model.Multivalued;
import com.backbase.stream.legalentity.model.PhoneNumber;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.legalentity.model.SavingsAccount;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.legalentity.model.ServiceAgreementV2;
import com.backbase.stream.legalentity.model.User;
import com.backbase.stream.legalentity.model.UserProfile;
import com.backbase.stream.service.AccessGroupService;
import com.backbase.stream.service.LegalEntityService;
import com.backbase.stream.service.UserService;
import com.backbase.stream.worker.exception.StreamTaskException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private ContactsSaga contactsSaga;

    @Mock
    private UserKindSegmentationSaga userKindSegmentationSaga;

    @Spy
    private final LegalEntitySagaConfigurationProperties legalEntitySagaConfigurationProperties =
        getLegalEntitySagaConfigurationProperties();

    String leExternalId = "someLeExternalId";
    String leParentExternalId = "someParentLeExternalId";
    String leInternalId = "someLeInternalId";
    String adminExId = "someAdminExId";
    String regularUserExId = "someRegularUserExId";
    LegalEntityV2 legalEntity;
    User regularUser;

    @Test
    void masterServiceAgreementCreation() {
        ServiceAgreementV2 sa = new ServiceAgreementV2().creatorLegalEntity(leExternalId);
        legalEntity = new LegalEntityV2().internalId(leInternalId).externalId(leExternalId).parentExternalId(leExternalId)
            .activateSingleServiceAgreement(false).masterServiceAgreement(sa);

        LegalEntityTaskV2 task = mockLegalEntityTask(legalEntity);

        when(task.getLegalEntity()).thenReturn(legalEntity);
        when(legalEntityService.getLegalEntityByExternalIdV2(eq(leExternalId))).thenReturn(Mono.empty());
        when(legalEntityService.getLegalEntityByInternalIdV2(eq(leInternalId))).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.getMasterServiceAgreementForInternalLegalEntityIdV2(eq(leInternalId))).thenReturn(Mono.just(sa));
        when(legalEntityService.createLegalEntity((LegalEntityV2) any())).thenReturn(Mono.just(legalEntity));
        when(userService.setupRealm(task.getLegalEntity()))
            .thenReturn(Mono.just(new Realm()));
        when(userService.linkLegalEntityToRealm(task.getLegalEntity()))
            .thenReturn(Mono.just(new LegalEntityV2()));

        Mono<LegalEntityTaskV2> result = legalEntitySaga.executeTask(task);
        result.block();

        verify(userService).setupRealm(task.getLegalEntity());
        verify(userService).linkLegalEntityToRealm(task.getLegalEntity());

        when(legalEntityService.getLegalEntityByExternalIdV2(eq(leExternalId))).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.putLegalEntity((LegalEntityV2) any())).thenReturn(Mono.just(legalEntity));
        result = legalEntitySaga.executeTask(task);
        result.block();

        verify(userService, times(2)).setupRealm(task.getLegalEntity());
        verify(userService, times(2)).linkLegalEntityToRealm(task.getLegalEntity());
    }

    @Test
    void masterServiceAgreementCreation_activateSingleServiceAgreement() {
        SavingsAccount account = new SavingsAccount();
        account.externalId("someAccountExId").productTypeExternalId("Account").currency("GBP");
        legalEntity = new LegalEntityV2().internalId(leInternalId).externalId(leExternalId).parentExternalId(leExternalId);
        ServiceAgreementV2 sa = new ServiceAgreementV2().creatorLegalEntity(leExternalId);

        LegalEntityTaskV2 task = mockLegalEntityTask(legalEntity);

        when(task.getLegalEntity()).thenReturn(legalEntity);
        when(legalEntityService.getLegalEntityByExternalIdV2(eq(leExternalId))).thenReturn(Mono.empty());
        when(legalEntityService.getLegalEntityByInternalIdV2(eq(leInternalId))).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.getMasterServiceAgreementForInternalLegalEntityIdV2(eq(leInternalId))).thenReturn(Mono.empty());
        when(legalEntityService.createLegalEntity((LegalEntityV2) any())).thenReturn(Mono.just(legalEntity));
        when(accessGroupService.createServiceAgreement(any(), (ServiceAgreementV2) any())).thenReturn(Mono.just(sa));
        when(userService.setupRealm(task.getLegalEntity()))
            .thenReturn(Mono.just(new Realm()));
        when(userService.linkLegalEntityToRealm(task.getLegalEntity()))
            .thenReturn(Mono.just(new LegalEntityV2()));

        Mono<LegalEntityTaskV2> result = legalEntitySaga.executeTask(task);
        result.block();

        verify(userService).setupRealm(task.getLegalEntity());
        verify(userService).linkLegalEntityToRealm(task.getLegalEntity());

        when(legalEntityService.getLegalEntityByExternalIdV2(eq(leExternalId))).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.putLegalEntity((LegalEntityV2) any())).thenReturn(Mono.just(legalEntity));
        result = legalEntitySaga.executeTask(task);
        result.block();

        verify(userService, times(2)).setupRealm(task.getLegalEntity());
        verify(userService, times(2)).linkLegalEntityToRealm(task.getLegalEntity());
    }

    @Test
    void deleteLegalEntity_usersPrefixedWithRemovedNotProcessed() {
        legalEntity = new LegalEntityV2().internalId(leInternalId).externalId(leExternalId)
            .parentExternalId(leExternalId);
        ServiceAgreementV2 sa = new ServiceAgreementV2().creatorLegalEntity(leExternalId);

        when(legalEntityService.getMasterServiceAgreementForExternalLegalEntityIdV2(leExternalId)).thenReturn(
            Mono.just(sa));
        when(legalEntityService.getLegalEntityByExternalIdV2(leExternalId)).thenReturn(Mono.just(legalEntity));

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
        when(accessGroupService.deleteAdmins((ServiceAgreementV2) any())).thenReturn(Mono.empty());
        when(userService.archiveUsers(any(), any())).thenReturn(Mono.empty());
        when(legalEntityService.deleteLegalEntity(any())).thenReturn(Mono.empty());

        Mono<Void> result = legalEntitySaga.deleteLegalEntity(leExternalId);
        result.block();

        verify(accessGroupService, times(3)).removePermissionsForUser(any(), any());
    }

    @Test
    void deleteLegalEntity_processesPaginatedListOfUsers() {
        legalEntity = new LegalEntityV2().internalId(leInternalId).externalId(leExternalId)
            .parentExternalId(leExternalId);
        ServiceAgreementV2 sa = new ServiceAgreementV2().creatorLegalEntity(leExternalId);

        when(legalEntityService.getMasterServiceAgreementForExternalLegalEntityIdV2(leExternalId)).thenReturn(
            Mono.just(sa));
        when(legalEntityService.getLegalEntityByExternalIdV2(leExternalId)).thenReturn(Mono.just(legalEntity));

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
        ServiceAgreementV2 sa = new ServiceAgreementV2().creatorLegalEntity(leExternalId);
        legalEntity = new LegalEntityV2().internalId(leInternalId).externalId(leExternalId).name("Model Bank")
                .addAdministratorsItem(adminUser).parentExternalId(leParentExternalId)
                .users(singletonList(regularUser))
                .subsidiaries(singletonList(
                        new LegalEntityV2().externalId(leExternalId)));

        LegalEntityV2 newLE = new LegalEntityV2().internalId(leInternalId).externalId(leExternalId).name("New Model Bank")
                .addAdministratorsItem(adminUser).parentExternalId(leParentExternalId)
                .users(singletonList(regularUser))
                .subsidiaries(singletonList(
                        new LegalEntityV2().externalId(leExternalId)
                ));

        LegalEntityTaskV2 task = mockLegalEntityTask(newLE);

        when(legalEntityService.getMasterServiceAgreementForInternalLegalEntityIdV2(leInternalId)).thenReturn(Mono.just(sa));
        when(accessGroupService.createServiceAgreement(any(), (ServiceAgreementV2) any())).thenReturn(Mono.just(sa));
        when(legalEntityService.getLegalEntityByExternalIdV2(eq(leExternalId))).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.getLegalEntityByInternalIdV2(eq(leInternalId))).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.putLegalEntity((LegalEntityV2) any())).thenReturn(Mono.just(newLE));
        when(userService.getUserByExternalId(eq(regularUserExId))).thenReturn(Mono.just(regularUser));
        when(userService.getUserByExternalId(eq(adminExId))).thenReturn(Mono.just(adminUser));
        when(userService.createUser(any(), any(), any())).thenReturn(Mono.empty());
        when(userService.setupRealm((LegalEntityV2) any())).thenReturn(Mono.just(new Realm()));
        when(userService.linkLegalEntityToRealm((LegalEntityV2) any())).thenReturn(Mono.just(new LegalEntityV2()));
        when(userService.updateUser(any())).thenReturn(Mono.empty());

        LegalEntityTaskV2 result = legalEntitySaga.executeTask(task)
                .block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(newLE.getName(), result.getData().getName());

        verify(legalEntityService).putLegalEntity(eq(newLE));
    }

    @Test
    void updateUserName() {
        User newRegularUser = new User().internalId("someRegularUserInId")
                .externalId(regularUserExId).fullName("New Name Regular User");
        User adminUser = new User().internalId("someAdminInId").externalId(adminExId).fullName("Admin");
        ServiceAgreementV2 sa = new ServiceAgreementV2().creatorLegalEntity(leExternalId);
        legalEntity = new LegalEntityV2().internalId(leInternalId).externalId(leExternalId).name("Model Bank")
            .addAdministratorsItem(adminUser).parentExternalId(leParentExternalId)
                .users(singletonList(newRegularUser))
                .subsidiaries(singletonList(
                        new LegalEntityV2().externalId(leExternalId).internalId("internalSubsidiary")
                ));

        LegalEntityTaskV2 task = mockLegalEntityTask(legalEntity);
        when(legalEntityService.getMasterServiceAgreementForInternalLegalEntityIdV2(leInternalId)).thenReturn(Mono.just(sa));
        when(legalEntityService.getMasterServiceAgreementForInternalLegalEntityIdV2("internalSubsidiary")).thenReturn(Mono.just(sa));
        when(accessGroupService.createServiceAgreement(any(), (ServiceAgreementV2) any())).thenReturn(Mono.just(sa));
        when(legalEntityService.getLegalEntityByExternalIdV2(eq(leExternalId))).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.getLegalEntityByInternalIdV2(eq(leInternalId))).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.createLegalEntity((LegalEntityV2) any())).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.putLegalEntity((LegalEntityV2) any())).thenReturn(Mono.empty());
        when(userService.getUserByExternalId(eq(adminExId))).thenReturn(Mono.just(adminUser));
        when(userService.getUserByExternalId(eq(regularUserExId))).thenReturn(Mono.just(newRegularUser));
        when(userService.createUser(any(), any(), any())).thenReturn(Mono.just(adminUser));
        when(userService.updateUser(any())).thenReturn(Mono.empty());
        when(userService.setupRealm((LegalEntityV2) any())).thenReturn(Mono.just(new Realm()));
        when(userService.linkLegalEntityToRealm((LegalEntityV2) any())).thenReturn(Mono.just(new LegalEntityV2()));
        when(userService.updateUser(any())).thenReturn(Mono.just(newRegularUser));

        LegalEntityTaskV2 result = legalEntitySaga.executeTask(task)
                .block();

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getData().getUsers());
        Assertions.assertNotNull(result.getData().getUsers().get(0));
        Assertions.assertEquals(newRegularUser.getFullName(),
                result.getData().getUsers().get(0).getFullName());
    }

    void getMockLegalEntity() {
        regularUser = new User().internalId("someRegularUserInId")
                .externalId(regularUserExId);
        User adminUser = new User().internalId("someAdminInId").externalId(adminExId);
        ServiceAgreementV2 sa = new ServiceAgreementV2().creatorLegalEntity(leExternalId);
        legalEntity = new LegalEntityV2()
                .internalId(leInternalId)
                .externalId(leExternalId)
                .addAdministratorsItem(adminUser)
                .parentExternalId(leParentExternalId)
                .users(singletonList(regularUser))
                .subsidiaries(singletonList(
                        new LegalEntityV2().externalId(leExternalId).internalId("internalSubsidiaries")
                ));

        when(legalEntityService.getMasterServiceAgreementForInternalLegalEntityIdV2(leInternalId)).thenReturn(Mono.just(sa));
        when(legalEntityService.getMasterServiceAgreementForInternalLegalEntityIdV2("internalSubsidiaries")).thenReturn(Mono.just(sa));
        when(accessGroupService.createServiceAgreement(any(), (ServiceAgreementV2) any())).thenReturn(Mono.just(sa));
        when(legalEntityService.getLegalEntityByExternalIdV2(leExternalId)).thenReturn(Mono.empty());
        when(legalEntityService.getLegalEntityByInternalIdV2(leInternalId)).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.createLegalEntity((LegalEntityV2) any())).thenReturn(Mono.just(legalEntity));
        when(userService.getUserByExternalId(regularUserExId)).thenReturn(Mono.just(regularUser));
        when(userService.getUserByExternalId(adminExId)).thenReturn(Mono.just(adminUser));
        when(userService.createUser(any(), any(), any())).thenReturn(Mono.empty());
        when(userService.setupRealm((LegalEntityV2) any())).thenReturn(Mono.just(new Realm()));
        when(userService.linkLegalEntityToRealm((LegalEntityV2) any())).thenReturn(Mono.just(new LegalEntityV2()));
        when(userService.updateUser(any())).thenReturn(Mono.just(regularUser));
    }

    @Test
    void test_PostLegalContacts() {
        getMockLegalEntity();
        LegalEntityTaskV2 task = mockLegalEntityTask(legalEntity);
        when(contactsSaga.executeTask(any(ContactsTask.class))).thenReturn(getContactsTask(AccessContextScope.LE));

        LegalEntityTaskV2 result = legalEntitySaga.executeTask(task).block();

        Assertions.assertNotNull(result);

        ExternalContact contact = getMockExternalContact();
        legalEntity.setContacts(Collections.singletonList(contact));
        result = legalEntitySaga.executeTask(task).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(leExternalId, result.getData().getContacts().get(0).getExternalId());
    }

    @Test
    void test_PostLegalContacts_NoUser() {

        regularUser = new User().internalId("someRegularUserInId")
                .externalId(regularUserExId);
        User adminUser = new User().internalId("someAdminInId").externalId(adminExId);

        ExternalContact contact = getMockExternalContact();
        ServiceAgreementV2 sa = new ServiceAgreementV2().creatorLegalEntity(leExternalId);
        legalEntity = new LegalEntityV2()
                .internalId(leInternalId)
                .externalId(leExternalId)
                .addAdministratorsItem(adminUser)
                .parentExternalId(leParentExternalId)
                .contacts(Collections.singletonList(contact))
                .subsidiaries(singletonList(
                        new LegalEntityV2().externalId(leExternalId).internalId("internalSubsidiary")
                ));

        LegalEntityTaskV2 task = mockLegalEntityTask(legalEntity);
        when(legalEntityService.getMasterServiceAgreementForInternalLegalEntityIdV2("internalSubsidiary")).thenReturn(Mono.just(sa));
        when(legalEntityService.getMasterServiceAgreementForInternalLegalEntityIdV2(leInternalId)).thenReturn(Mono.just(sa));
        when(accessGroupService.createServiceAgreement(any(), (ServiceAgreementV2) any())).thenReturn(Mono.just(sa));
        when(legalEntityService.getLegalEntityByExternalIdV2(leExternalId)).thenReturn(Mono.empty());
        when(legalEntityService.getLegalEntityByInternalIdV2(leInternalId)).thenReturn(Mono.just(legalEntity));
        when(legalEntityService.createLegalEntity((LegalEntityV2) any())).thenReturn(Mono.just(legalEntity));
        when(userService.getUserByExternalId(adminExId)).thenReturn(Mono.just(adminUser));
        when(userService.createUser(any(), any(), any())).thenReturn(Mono.empty());
        when(userService.setupRealm((LegalEntityV2) any())).thenReturn(Mono.just(new Realm()));
        when(userService.linkLegalEntityToRealm((LegalEntityV2) any())).thenReturn(Mono.just(new LegalEntityV2()));
        when(userService.updateUser(any())).thenReturn(Mono.just(regularUser));

        LegalEntityTaskV2 result = legalEntitySaga.executeTask(task).block();

        Assertions.assertNotNull(result);
    }

    @Test
    void test_PostUserContacts() {
        getMockLegalEntity();
        LegalEntityTaskV2 task = mockLegalEntityTask(legalEntity);

        when(contactsSaga.executeTask(any(ContactsTask.class))).thenReturn(getContactsTask(AccessContextScope.USER));

        LegalEntityTaskV2 result = legalEntitySaga.executeTask(task).block();

        Assertions.assertNotNull(result);

        result = legalEntitySaga.executeTask(task).block();
        Assertions.assertNotNull(result);
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
        LegalEntityTaskV2 leTask = new LegalEntityTaskV2(new LegalEntityV2().externalId(leExternalId));
        when(legalEntityService.getLegalEntityByExternalIdV2(leExternalId)).thenReturn(Mono.error(ex));

        // When
        Mono<LegalEntityTaskV2> result = legalEntitySaga.executeTask(leTask);
        StreamTaskException stEx = null;
        try {
            result.block();
        } catch (StreamTaskException e) {
            stEx = e;
        }

        // Then
        verify(legalEntityService).getLegalEntityByExternalIdV2(any());
        org.assertj.core.api.Assertions.assertThat(stEx)
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
        LegalEntitySagaConfigurationProperties sagaConfiguration =  new LegalEntitySagaConfigurationProperties();
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
