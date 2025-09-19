package com.backbase.stream.service;

import static com.backbase.stream.LambdaAssertions.assertEqualsTo;
import static com.backbase.stream.WebClientTestUtils.buildWebResponseExceptionMono;
import static com.backbase.stream.legalentity.model.LegalEntityStatus.ENABLED;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import com.backbase.accesscontrol.assignpermissions.api.service.v1.AssignPermissionsApi;
import com.backbase.accesscontrol.assignpermissions.api.service.v1.model.UserPermissionItem;
import com.backbase.accesscontrol.assignpermissions.api.service.v1.model.UserPermissions;
import com.backbase.accesscontrol.datagroup.api.integration.v1.model.BatchResponseItemExtended.StatusEnum;
import com.backbase.accesscontrol.datagroup.api.integration.v1.model.DataItemBatchUpdate;
import com.backbase.accesscontrol.datagroup.api.service.v1.DataGroupApi;
import com.backbase.accesscontrol.datagroup.api.service.v1.model.DataGroup;
import com.backbase.accesscontrol.datagroup.api.service.v1.model.DataGroupUpdateRequest;
import com.backbase.accesscontrol.datagroup.api.service.v1.model.GetDataGroups;
import com.backbase.accesscontrol.functiongroup.api.integration.v1.model.FunctionGroupNameIdentifier;
import com.backbase.accesscontrol.functiongroup.api.service.v1.FunctionGroupApi;
import com.backbase.accesscontrol.functiongroup.api.service.v1.model.FunctionGroupItem;
import com.backbase.accesscontrol.functiongroup.api.service.v1.model.FunctionGroupItem.TypeEnum;
import com.backbase.accesscontrol.functiongroup.api.service.v1.model.GetFunctionGroups;
import com.backbase.accesscontrol.permissioncheck.api.service.v1.PermissionCheckApi;
import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.Action;
import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.BatchResponseItemExtended;
import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.ParticipantCreateRequest;
import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.ResultId;
import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.ServiceAgreementAdmin;
import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.ServiceAgreementAdminsBatchUpdateRequest;
import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.ServiceAgreementCreateRequest;
import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.ServiceAgreementDetails;
import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.ServiceAgreementUserExternal;
import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.ServiceAgreementUsersBatchUpdateRequest;
import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.Status;
import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.StatusCode;
import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.UpdateParticipantItem;
import com.backbase.accesscontrol.serviceagreement.api.service.v1.ServiceAgreementApi;
import com.backbase.accesscontrol.serviceagreement.api.service.v1.model.Participant;
import com.backbase.accesscontrol.serviceagreement.api.service.v1.model.ServiceAgreementParticipants;
import com.backbase.accesscontrol.serviceagreement.api.service.v1.model.ServiceAgreementUpdateRequest;
import com.backbase.accesscontrol.serviceagreement.api.service.v1.model.ServiceAgreementUsers;
import com.backbase.accesscontrol.usercontext.api.service.v1.UserContextApi;
import com.backbase.accesscontrol.usercontext.api.service.v1.model.ContextServiceAgreement;
import com.backbase.accesscontrol.usercontext.api.service.v1.model.GetContexts;
import com.backbase.dbs.user.api.service.v2.UserManagementApi;
import com.backbase.stream.configuration.AccessControlConfigurationProperties;
import com.backbase.stream.configuration.DeletionProperties;
import com.backbase.stream.legalentity.model.BaseProductGroup;
import com.backbase.stream.legalentity.model.BaseProductGroup.ProductGroupTypeEnum;
import com.backbase.stream.legalentity.model.BatchProductGroup;
import com.backbase.stream.legalentity.model.BusinessFunctionGroup;
import com.backbase.stream.legalentity.model.CurrentAccount;
import com.backbase.stream.legalentity.model.CustomDataGroupItem;
import com.backbase.stream.legalentity.model.JobProfileUser;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.LegalEntityParticipant;
import com.backbase.stream.legalentity.model.LegalEntityType;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.legalentity.model.ServiceAgreementUserAction;
import com.backbase.stream.legalentity.model.User;
import com.backbase.stream.product.task.BatchProductGroupTask;
import com.backbase.stream.product.task.BatchProductIngestionMode;
import com.backbase.stream.product.task.ProductGroupTask;
import com.backbase.stream.utils.BatchResponseUtils;
import com.backbase.stream.worker.exception.StreamTaskException;
import com.backbase.stream.worker.model.StreamTask;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AccessGroupServiceTest {

    @InjectMocks
    private AccessGroupService subject;
    @Mock
    private UserManagementApi usersApi;
    @Mock
    private PermissionCheckApi permissionCheckServiceApi;
    @Mock
    private DataGroupApi dataGroupServiceApi;
    @Mock
    private com.backbase.accesscontrol.datagroup.api.integration.v1.DataGroupApi dataGroupIntegrationApi;
    @Mock
    private FunctionGroupApi functionGroupServiceApi;
    @Mock
    private com.backbase.accesscontrol.functiongroup.api.integration.v1.FunctionGroupApi functionGroupIntegrationApi;
    @Mock
    private ServiceAgreementApi serviceAgreementServiceApi;
    @Mock
    private com.backbase.accesscontrol.serviceagreement.api.integration.v1.ServiceAgreementApi serviceAgreementIntegrationApi;
    @Mock
    private AssignPermissionsApi assignPermissionsServiceApi;
    @Mock
    private com.backbase.accesscontrol.assignpermissions.api.integration.v1.AssignPermissionsApi assignPermissionsIntegrationApi;
    @Mock
    private UserContextApi userContextApi;
    @Spy
    private DeletionProperties configurationProperties;
    @Spy
    private BatchResponseUtils batchResponseUtils;
    @Mock
    private AccessControlConfigurationProperties accessControlProperties;
    @Captor
    private ArgumentCaptor<List<DataItemBatchUpdate>> presentationDataGroupItemPutRequestBodyCaptor;

    String userInternalId = "userInternalId";
    private static final OffsetDateTime VALID_FROM = OffsetDateTime.of(LocalDateTime.now().minusYears(1L),
        ZoneOffset.UTC);
    private static final OffsetDateTime VALID_UNTIL = OffsetDateTime.of(LocalDateTime.now().plusMonths(10L),
        ZoneOffset.UTC);

    @Test
    void shouldCreateServiceAgreement() {
        String saExId = "someSaExId";
        String saName = "someSaName";
        String saDesc = "someSaDesc";
        String participantExId = "someParticipantExId";
        String saInId = "someSaInId";

        StreamTask streamTask = Mockito.mock(StreamTask.class);
        ServiceAgreement serviceAgreement = new ServiceAgreement()
            .externalId(saExId).name(saName).description(saDesc)
            .validFrom(VALID_FROM).validUntil(VALID_UNTIL)
            .participants(List.of(new LegalEntityParticipant().externalId(participantExId)));

        when(serviceAgreementIntegrationApi.ingestServiceAgreement(any())).thenReturn(
            Mono.just(new ResultId().id(saInId)));

        ServiceAgreement actual = subject.createServiceAgreement(streamTask, serviceAgreement).block();

        assertEquals(saInId, actual.getInternalId());

        ServiceAgreementCreateRequest expectedSA = new ServiceAgreementCreateRequest()
            .status(Status.ENABLED).isSingle(null)
            .validFrom(VALID_FROM).validUntil(VALID_UNTIL)
            .externalId(saExId).name(saName).description(saDesc)
            .participants(List.of(new ParticipantCreateRequest().externalId(participantExId)));
        verify(serviceAgreementIntegrationApi).ingestServiceAgreement(expectedSA);
    }

    @Test
    void shouldHandleErrorOnCreateServiceAgreement() {
        String saExId = "someSaExId";
        String saName = "someSaName";
        String saDesc = "someSaDesc";
        String participantExId = "someParticipantExId";

        StreamTask streamTask = Mockito.mock(StreamTask.class);
        ServiceAgreement serviceAgreement = new ServiceAgreement()
            .externalId(saExId).name(saName).description(saDesc)
            .participants(List.of(new LegalEntityParticipant().externalId(participantExId)));

        String errorMessage = "some error";
        WebClientResponseException.InternalServerError error =
            Mockito.mock(WebClientResponseException.InternalServerError.class);
        when(error.getResponseBodyAsString()).thenReturn(errorMessage);
        when(serviceAgreementIntegrationApi.ingestServiceAgreement(any()))
            .thenReturn(Mono.error(error));

        assertThrows(StreamTaskException.class,
            () -> subject.createServiceAgreement(streamTask, serviceAgreement).block());

        verify(streamTask).error("service-agreement", "create", "failed", saExId, "", error, errorMessage,
            "Failed to create Service Agreement");
    }

    @Test
    void shouldSetupProductGroupThatAlreadyExists() {
        String saInId = "someSaInId";
        String saExId = "someSaExId";
        String existingDgItemExId1 = "someDgItemExId1";
        String existingDgItemInId1 = existingDgItemExId1 + "in";
        String productGroupName = "somePgName";

        ProductGroupTask streamTask = new ProductGroupTask().data(new ProductGroup()
            .serviceAgreement(new ServiceAgreement().externalId(saExId).internalId(saInId))
            .productGroupType(ProductGroupTypeEnum.CUSTOM)
            .name(productGroupName)
            .customDataGroupItems(List.of(
                new CustomDataGroupItem().internalId(existingDgItemInId1).externalId(existingDgItemExId1))));

        when(dataGroupServiceApi.getDataGroups(saInId, ProductGroupTypeEnum.CUSTOM.name(), true, null, 1000))
            .thenReturn(Mono.just(new GetDataGroups().dataGroups(
                List.of(new DataGroup().id(existingDgItemInId1).name(productGroupName)))));

        when(dataGroupServiceApi.updateDataGroup(any(), any())).thenReturn(Mono.empty());

        subject.setupProductGroups(streamTask).block();

        DataGroupUpdateRequest expectedDGUpdate1 = new DataGroupUpdateRequest();
        expectedDGUpdate1.setDataItems(Set.of(existingDgItemInId1));
        expectedDGUpdate1.setName(productGroupName);

        verify(dataGroupServiceApi).updateDataGroup(existingDgItemInId1, expectedDGUpdate1);
    }

    @Test
    void shouldSetAdministrators() {
        String leName = "someLeName";
        String leInId = "someLeInId";
        String saExId = "someSaExId";
        String fullName = "John Smith";
        String userExId = "someUserExId";

        LegalEntity legalEntity =
            new LegalEntity(leName, LegalEntityType.CUSTOMER, List.of(
                new User().fullName(fullName).legalEntityId(leInId).externalId(userExId)))
                .masterServiceAgreement(new ServiceAgreement().externalId(saExId));

        when(serviceAgreementIntegrationApi.batchUpdateServiceAgreementAdmins(any()))
            .thenReturn(Flux.just(new BatchResponseItemExtended()));

        subject.setAdministrators(legalEntity).block();

        ServiceAgreementAdminsBatchUpdateRequest expected = new ServiceAgreementAdminsBatchUpdateRequest()
            .action(Action.ADD).addUsersItem(new ServiceAgreementAdmin()
                .externalServiceAgreementId(saExId).externalUserId(userExId));
        verify(serviceAgreementIntegrationApi).batchUpdateServiceAgreementAdmins(expected);
    }

    @Test
    void getServiceAgreementByExternalIdRetrievesServiceAgreementByExternalId() {
        final String externalId = "someExternalId";
        final Mono<ServiceAgreementDetails> dbsSa = Mono.just(new ServiceAgreementDetails().externalId(externalId));

        when(serviceAgreementIntegrationApi.getServiceAgreementByExternalId(eq(externalId))).thenReturn(dbsSa);

        Mono<ServiceAgreement> result = subject.getServiceAgreementByExternalId(externalId);

        ServiceAgreement expected = new ServiceAgreement().externalId(externalId);

        StepVerifier.create(result)
            .assertNext(serviceAgreement -> assertEquals(serviceAgreement, expected))
            .verifyComplete();
    }

    @Test
    void getServiceAgreementByExternalIdReturnsEmptyOnServiceAgreementNotFound() {
        final String externalId = "someExternalId";

        Mono<ServiceAgreementDetails> response =
            buildWebResponseExceptionMono(WebClientResponseException.NotFound.class, HttpMethod.GET);
        when(serviceAgreementIntegrationApi.getServiceAgreementByExternalId(eq(externalId)))
            .thenReturn(response);

        Mono<ServiceAgreement> actual = subject.getServiceAgreementByExternalId(externalId);

        actual.subscribe(assertEqualsTo(null));
    }

    @Test
    void updateServiceAgreement() {
        final String saInternalId = "someSaInternalId";
        final String saExternalId = "someSaExternalId";
        final String description = "someDescription";
        final String name = "someName";
        final List<ServiceAgreementUserAction> regularUsers = asList("userId1", "userId2").stream()
            .map(u -> new ServiceAgreementUserAction().userProfile(new JobProfileUser().user(new User()
                    .externalId("ex_" + u).internalId("in_" + u)))
                .action(ServiceAgreementUserAction.ActionEnum.ADD))
            .collect(Collectors.toList());

        StreamTask streamTask = Mockito.mock(StreamTask.class);

        ServiceAgreement serviceAgreement = buildInputServiceAgreement(saInternalId, saExternalId, description, name);

        serviceAgreement
            .addParticipantsItem(new LegalEntityParticipant().externalId("p1").sharingAccounts(true)
                .sharingUsers(true).action(LegalEntityParticipant.ActionEnum.ADD))
            .addParticipantsItem(new LegalEntityParticipant().externalId("p2").sharingAccounts(false)
                .sharingUsers(false).action(LegalEntityParticipant.ActionEnum.ADD));

        when(serviceAgreementIntegrationApi.batchUpdateParticipants(any()))
            .thenReturn(Flux.concat(
                Mono.just(new BatchResponseItemExtended().action(Action.ADD).resourceId("p1")
                    .status(StatusCode.HTTP_STATUS_OK)),
                Mono.just(new BatchResponseItemExtended().action(Action.ADD).resourceId("p2")
                    .status(StatusCode.HTTP_STATUS_OK))
            ));

        Flux<BatchResponseItemExtended> usersResponse = Flux.fromIterable(regularUsers.stream()
            .map(u -> new BatchResponseItemExtended().status(StatusCode.HTTP_STATUS_OK)
                .resourceId(u.getUserProfile().getUser().getExternalId()))
            .collect(Collectors.toList()));
        when(serviceAgreementIntegrationApi.batchUpdateServiceAgreementUsers(any())).thenReturn(usersResponse);

        when(serviceAgreementServiceApi.getServiceAgreementParticipants(eq(saInternalId)))
            .thenReturn(Mono.just(new ServiceAgreementParticipants()));

        Mono<ServiceAgreementUsers> emptyExistingUsersList = Mono.just(new ServiceAgreementUsers());
        when(serviceAgreementServiceApi.getServiceAgreementUsers(saInternalId, null, 1000)).thenReturn(
            emptyExistingUsersList);

        Mono<ServiceAgreement> result = subject.updateServiceAgreementAssociations(streamTask, serviceAgreement,
            regularUsers);
        result.block();

        InOrder inOrderValidator = inOrder(serviceAgreementIntegrationApi);
        thenUpdateParticipantsCall(inOrderValidator, saExternalId, Action.ADD,
            new ExpectedParticipantUpdate("p1", true, true),
            new ExpectedParticipantUpdate("p2", false, false));

        thenRegularUsersUpdateCall(saExternalId, Action.ADD, "ex_userId1", "ex_userId2");
    }

    @Test
    void updateServiceAgreementWithExistingParticipants() {
        final String saInternalId = "someSaInternalId";
        final String saExternalId = "someSaExternalId";
        final String description = "someDescription";
        final String name = "someName";
        final List<ServiceAgreementUserAction> regularUsersToAdd = asList("userId1", "userId2").stream()
            .map(u -> new ServiceAgreementUserAction().userProfile(new JobProfileUser().user(new User()
                    .externalId("ex_" + u).internalId("in_" + u)))
                .action(ServiceAgreementUserAction.ActionEnum.ADD))
            .toList();
        final List<ServiceAgreementUserAction> regularUsersToRemove = asList("userId3", "userId4").stream()
            .map(u -> new ServiceAgreementUserAction().userProfile(new JobProfileUser().user(new User()
                    .externalId("ex_" + u).internalId("in_" + u)))
                .action(ServiceAgreementUserAction.ActionEnum.REMOVE))
            .toList();
        final List<ServiceAgreementUserAction> regularUsers =
            Stream.concat(regularUsersToAdd.stream(), regularUsersToRemove.stream()).collect(Collectors.toList());

        StreamTask streamTask = Mockito.mock(StreamTask.class);

        ServiceAgreement serviceAgreement = buildInputServiceAgreement(saInternalId, saExternalId, description, name);

        // participants
        serviceAgreement
            .addParticipantsItem(new LegalEntityParticipant().externalId("p1").sharingAccounts(true)
                .sharingUsers(true).action(LegalEntityParticipant.ActionEnum.ADD))
            .addParticipantsItem(new LegalEntityParticipant().externalId("p2").sharingAccounts(false)
                .sharingUsers(false).action(LegalEntityParticipant.ActionEnum.REMOVE))
            .addParticipantsItem(new LegalEntityParticipant().externalId("p3").sharingAccounts(false)
                .sharingUsers(false).action(LegalEntityParticipant.ActionEnum.ADD));

        when(serviceAgreementIntegrationApi.batchUpdateParticipants(any()))
            .thenReturn(Flux.concat(Mono.just(new BatchResponseItemExtended().status(StatusCode.HTTP_STATUS_OK))));

        when(serviceAgreementServiceApi.getServiceAgreementParticipants(eq(saInternalId)))
            .thenReturn(Mono.just(new ServiceAgreementParticipants().participants(List.of(
                new Participant().externalId("p1"),
                new Participant().externalId("p2")
            ))));

        // users
        Flux<BatchResponseItemExtended> usersResponse = Flux.fromIterable(regularUsers.stream()
            .map(u -> new BatchResponseItemExtended().status(StatusCode.HTTP_STATUS_OK)
                .resourceId(u.getUserProfile().getUser().getExternalId()))
            .collect(Collectors.toList()));
        when(serviceAgreementIntegrationApi.batchUpdateServiceAgreementUsers(any())).thenReturn(usersResponse);

        Mono<ServiceAgreementUsers> existingUsersList =
            Mono.just(new ServiceAgreementUsers().userIds(List.of(
                new com.backbase.accesscontrol.serviceagreement.api.service.v1.model.User().userId("in_userId1"),
                new com.backbase.accesscontrol.serviceagreement.api.service.v1.model.User().userId("in_userId3"))));
        when(serviceAgreementServiceApi.getServiceAgreementUsers(saInternalId, null, 1000))
            .thenReturn(existingUsersList);

        Mono<ServiceAgreement> result = subject.updateServiceAgreementAssociations(streamTask, serviceAgreement,
            regularUsers);
        result.block();

        InOrder inOrderValidator = inOrder(serviceAgreementIntegrationApi);
        thenUpdateParticipantsCall(inOrderValidator, saExternalId, Action.ADD,
            new ExpectedParticipantUpdate("p3", false, false));
        thenUpdateParticipantsCall(inOrderValidator, saExternalId, Action.REMOVE,
            new ExpectedParticipantUpdate("p2", false, false));

        thenRegularUsersUpdateCall(saExternalId, Action.REMOVE, "ex_userId3");
        thenRegularUsersUpdateCall(saExternalId, Action.ADD, "ex_userId2");
    }

    @Test
    void updateParticipantsLogsAllErrors() {
        final String saExternalId = "someSaExternalId";
        final String saInternalId = "someSaInternalId";

        StreamTask streamTask = Mockito.spy(StreamTask.class);

        ServiceAgreement serviceAgreement = new ServiceAgreement().internalId(saInternalId).externalId(saExternalId);

        serviceAgreement
            .addParticipantsItem(new LegalEntityParticipant().externalId("p1").sharingAccounts(true)
                .sharingUsers(true).action(LegalEntityParticipant.ActionEnum.ADD))
            .addParticipantsItem(new LegalEntityParticipant().externalId("p2").sharingAccounts(false)
                .sharingUsers(false).action(LegalEntityParticipant.ActionEnum.ADD))
            .addParticipantsItem(new LegalEntityParticipant().externalId("p3").sharingAccounts(false)
                .sharingUsers(false).action(LegalEntityParticipant.ActionEnum.ADD))
            .addParticipantsItem(new LegalEntityParticipant().externalId("p4").sharingAccounts(false)
                .sharingUsers(false).action(LegalEntityParticipant.ActionEnum.ADD));

        when(serviceAgreementIntegrationApi.batchUpdateParticipants(any()))
            .thenReturn(Flux.concat(
                Mono.just(new BatchResponseItemExtended().action(Action.ADD).resourceId("p1")
                    .status(StatusCode.HTTP_STATUS_OK)),
                Mono.just(new BatchResponseItemExtended().action(Action.ADD).resourceId("p2")
                    .status(StatusCode.HTTP_STATUS_INTERNAL_SERVER_ERROR)),
                Mono.just(new BatchResponseItemExtended().action(Action.ADD).resourceId("p3")
                    .status(StatusCode.HTTP_STATUS_INTERNAL_SERVER_ERROR)),
                Mono.just(new BatchResponseItemExtended().action(Action.ADD).resourceId("p4")
                    .status(StatusCode.HTTP_STATUS_OK))
            ));

        when(serviceAgreementServiceApi.getServiceAgreementParticipants(eq(saInternalId)))
            .thenReturn(Mono.just(new ServiceAgreementParticipants()));

        Mono<Map<LegalEntityParticipant.ActionEnum, Mono<ServiceAgreement>>> result =
            subject.updateParticipants(streamTask, serviceAgreement);
        assertThrows(StreamTaskException.class, () -> {
            Map<LegalEntityParticipant.ActionEnum, Mono<ServiceAgreement>> map = result.block();
            map.get(LegalEntityParticipant.ActionEnum.ADD)
                .then(map.get(LegalEntityParticipant.ActionEnum.REMOVE))
                .block();
        });

        InOrder verifier = inOrder(streamTask);
        verifier.verify(streamTask).error(eq("participant"), eq("update-participant"), eq("failed"),
            eq("p2"), eq(null), any(String.class), any(String.class), any(String.class));
        verifier.verify(streamTask).error(eq("participant"), eq("update-participant"), eq("failed"),
            eq("p3"), eq(null), any(String.class), any(String.class), any(String.class));
    }

    @Test
    void assignPermissionsBatch() {
        // Given
        BatchProductGroupTask batchProductGroupTask = new BatchProductGroupTask().data(
            new BatchProductGroup().serviceAgreement(
                new ServiceAgreement().externalId("sa_benedict").internalId("sa-internal-id"))
        );
        batchProductGroupTask.setIngestionMode(BatchProductIngestionMode.UPSERT);

        Map<BusinessFunctionGroup, List<BaseProductGroup>> baseProductGroupMap = new HashMap<>();
        baseProductGroupMap.put(new BusinessFunctionGroup().id("business-function-group-id-1"),
            List.of(new BaseProductGroup().internalId("data-group-id")));

        Map<User, Map<BusinessFunctionGroup, List<BaseProductGroup>>> usersPermissions = new HashMap<>();
        usersPermissions.put(
            new User().internalId("user-internal-id").externalId("benedict"),
            baseProductGroupMap
        );

        List<UserPermissionItem> expectedPermissions = List.of(
            new UserPermissionItem().functionGroupId("system-group-id-2").dataGroupIds(Set.of()),
            new UserPermissionItem().functionGroupId("system-group-id-3").dataGroupIds(Set.of()),
            new UserPermissionItem().functionGroupId("business-function-group-id-1")
                .dataGroupIds(Set.of("data-group-id"))
        );

        when(functionGroupServiceApi.getFunctionGroups("sa-internal-id"))
            .thenReturn(Mono.just(new GetFunctionGroups()
                .functionGroups(List.of(
                    new FunctionGroupItem().id("system-group-id-1").name("SYSTEM_FUNCTION_GROUP")
                        .type(FunctionGroupItem.TypeEnum.SYSTEM),
                    new FunctionGroupItem().id("system-group-id-2").name("Full access")
                        .type(TypeEnum.REFERENCE)
                ))
            ));

        when(assignPermissionsServiceApi.getUserPermissions("user-internal-id", "sa-internal-id"))
            .thenReturn(Mono.just(new UserPermissions().permissions(List.of(
                new UserPermissionItem().functionGroupId("system-group-id-1").dataGroupIds(Set.of()),
                new UserPermissionItem().functionGroupId("system-group-id-2").dataGroupIds(Set.of()),
                new UserPermissionItem().functionGroupId("system-group-id-3").dataGroupIds(Set.of()),
                new UserPermissionItem().functionGroupId("business-function-group-id-1")
                    .dataGroupIds(Set.of("data-group-id"))
            ))));

        when(assignPermissionsServiceApi.putUserPermissions(any(), any(), any())).thenReturn(Mono.empty());

        // When
        BatchProductGroupTask result = subject.assignPermissionsBatch(batchProductGroupTask, usersPermissions)
            .block();

        // Then
        Assertions.assertSame(batchProductGroupTask, result);

        ArgumentCaptor<List<UserPermissionItem>> argumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(assignPermissionsServiceApi).putUserPermissions(eq("user-internal-id"), eq("sa-internal-id"),
            argumentCaptor.capture());
        assertEquals(expectedPermissions.size(), argumentCaptor.getValue().size());
        assertTrue(argumentCaptor.getValue().containsAll(expectedPermissions));
    }

    /*
       Request contains business-function-group-id-1, business-function-group-id-2
       Existing permissions are: system-group-id-1, function-group-id-1, business-function-group-id-1 and business-function-group-id-2
       Expectation is to have function-group-id-1, business-function-group-id-1 and business-function-group-id-2 in PUT permissions request together with data group ids specified in request
     */
    @Test
    void assignPermissionsBatchNoExistingFunctionGroups() {
        // Given
        BatchProductGroupTask batchProductGroupTask = new BatchProductGroupTask().data(
            new BatchProductGroup().serviceAgreement(
                new ServiceAgreement().externalId("sa_benedict").internalId("sa-internal-id"))
        );
        batchProductGroupTask.setIngestionMode(BatchProductIngestionMode.UPSERT);

        Map<BusinessFunctionGroup, List<BaseProductGroup>> baseProductGroupMap = new HashMap<>();
        baseProductGroupMap.put(
            new BusinessFunctionGroup().id("business-function-group-id-1"),
            Collections.singletonList(new BaseProductGroup().internalId("data-group-0"))
        );
        baseProductGroupMap.put(
            new BusinessFunctionGroup().id("business-function-group-id-2"),
            Collections.singletonList(new BaseProductGroup().internalId("data-group-2"))
        );

        Map<User, Map<BusinessFunctionGroup, List<BaseProductGroup>>> usersPermissions = new HashMap<>();
        usersPermissions.put(
            new User().internalId("user-internal-id").externalId("benedict"),
            baseProductGroupMap
        );

        List<UserPermissionItem> expectedPermissions = List.of(
            new UserPermissionItem().functionGroupId("function-group-id-1").dataGroupIds(Set.of()),
            new UserPermissionItem().functionGroupId("business-function-group-id-1").dataGroupIds(Set.of("data-group-0",
                "data-group-1")),
            new UserPermissionItem().functionGroupId("business-function-group-id-2")
                .dataGroupIds(Set.of("data-group-2"))
        );

        when(functionGroupServiceApi.getFunctionGroups("sa-internal-id"))
            .thenReturn(Mono.just(new GetFunctionGroups()
                .functionGroups(List.of(
                    new FunctionGroupItem().id("system-group-id-1").name("SFG")
                        .type(FunctionGroupItem.TypeEnum.SYSTEM),
                    new FunctionGroupItem().id("function-group-id-1").name("Full access")
                        .type(TypeEnum.REFERENCE)
                ))
            ));

        when(assignPermissionsServiceApi.getUserPermissions("user-internal-id", "sa-internal-id"))
            .thenReturn(Mono.just(new UserPermissions().permissions(List.of(
                new UserPermissionItem().functionGroupId("function-group-id-1").dataGroupIds(Set.of()),
                new UserPermissionItem().functionGroupId("business-function-group-id-1")
                    .dataGroupIds(Set.of("data-group-1"))
            ))));

        when(assignPermissionsServiceApi.putUserPermissions(any(), any(), any())).thenReturn(Mono.empty());

        // When
        BatchProductGroupTask result = subject.assignPermissionsBatch(batchProductGroupTask, usersPermissions)
            .block();

        // Then
        Assertions.assertSame(batchProductGroupTask, result);

        ArgumentCaptor<List<UserPermissionItem>> argumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(assignPermissionsServiceApi).putUserPermissions(eq("user-internal-id"), eq("sa-internal-id"),
            argumentCaptor.capture());
        assertEquals(expectedPermissions.size(), argumentCaptor.getValue().size());
        assertTrue(argumentCaptor.getValue().containsAll(expectedPermissions));
    }

    @Test
    void assignPermissionsBatchIngestionModeReplace() {
        // Given
        BatchProductGroupTask batchProductGroupTask = new BatchProductGroupTask().data(
            new BatchProductGroup().serviceAgreement(
                new ServiceAgreement().externalId("sa_benedict").internalId("sa-internal-id"))
        );
        batchProductGroupTask.setIngestionMode(BatchProductIngestionMode.REPLACE);

        Map<BusinessFunctionGroup, List<BaseProductGroup>> baseProductGroupMap = new HashMap<>();
        baseProductGroupMap.put(new BusinessFunctionGroup().id("business-function-group-id-1"),
            Collections.emptyList());

        Map<User, Map<BusinessFunctionGroup, List<BaseProductGroup>>> usersPermissions = new HashMap<>();
        usersPermissions.put(
            new User().internalId("user-internal-id").externalId("benedict"),
            baseProductGroupMap
        );

        List<UserPermissionItem> expectedPermissions = List.of(
            new UserPermissionItem().functionGroupId("business-function-group-id-1").dataGroupIds(Set.of()));

        when(assignPermissionsServiceApi.putUserPermissions(any(), any(), any())).thenReturn(Mono.empty());

        // When
        BatchProductGroupTask result = subject.assignPermissionsBatch(batchProductGroupTask, usersPermissions)
            .block();

        // Then
        Assertions.assertSame(batchProductGroupTask, result);

        verify(assignPermissionsServiceApi).putUserPermissions("user-internal-id", "sa-internal-id",
            expectedPermissions);
    }

    @Test
    void updateExistingDataGroupsBatchWithSameInDbsIngestionModeReplace() {
        // Given
        BatchProductGroupTask batchProductGroupTask = new BatchProductGroupTask();
        batchProductGroupTask.setIngestionMode(BatchProductIngestionMode.REPLACE);
        batchProductGroupTask.setBatchProductGroup(new BatchProductGroup().productGroups(
            List.of(new BaseProductGroup().name("Test product group"))));

        DataGroup dataGroupItemTemplateCustom = buildDataGroupItem("Repository Group Template Custom",
            "Repository Group Template Custom", "template-custom");
        DataGroup dataGroupItemEngagementTemplateCustom = buildDataGroupItem(
            "Repository Group Engagement Template Custom",
            "Repository Group Engagement Template Custom", "engagement-template-custom");
        DataGroup dataGroupItemEngagementTemplateNotification = buildDataGroupItem(
            "Repository Group Engagement Template Notification",
            "Repository Group Engagement Template Notification", "engagement-template-notification");

        BaseProductGroup baseProductGroupTemplateCustom = buildBaseProductGroup("Repository Group Template Custom",
            "Repository Group Template Custom", BaseProductGroup.ProductGroupTypeEnum.REPOSITORIES,
            "template-custom");
        BaseProductGroup baseProductGroupEngagementTemplateCustom = buildBaseProductGroup(
            "Repository Group Engagement Template Custom",
            "Repository Group Engagement Template Custom", BaseProductGroup.ProductGroupTypeEnum.REPOSITORIES,
            "engagement-template-custom");
        BaseProductGroup baseProductGroupEngagementTemplateNotification = buildBaseProductGroup(
            "Repository Group Engagement Template Notification",
            "Repository Group Engagement Template Notification", BaseProductGroup.ProductGroupTypeEnum.REPOSITORIES,
            "engagement-template-notification");

        // When
        subject.updateExistingDataGroupsBatch(batchProductGroupTask,
                List.of(dataGroupItemTemplateCustom,
                    dataGroupItemEngagementTemplateCustom,
                    dataGroupItemEngagementTemplateNotification),
                List.of(baseProductGroupTemplateCustom,
                    baseProductGroupEngagementTemplateCustom,
                    baseProductGroupEngagementTemplateNotification))
            .block();

        // Then
        verify(dataGroupIntegrationApi, times(0)).batchUpdateDataItems(any());
    }

    @Test
    void updateExistingDataGroupsBatchWithMissingInDbsIngestionModeReplace() {
        // Given
        BatchProductGroupTask batchProductGroupTask = new BatchProductGroupTask();
        batchProductGroupTask.setIngestionMode(BatchProductIngestionMode.REPLACE);
        batchProductGroupTask.setBatchProductGroup(new BatchProductGroup().productGroups(
                List.of(new BaseProductGroup().name("Test product group")))
            .serviceAgreement(new ServiceAgreement().externalId("sa-internal-id")));

        DataGroup dataGroupItemTemplateCustom = buildDataGroupItem("Repository Group Template Custom",
            "Repository Group Template Custom");
        DataGroup dataGroupItemEngagementTemplateCustom = buildDataGroupItem(
            "Repository Group Engagement Template Custom",
            "Repository Group Engagement Template Custom");
        DataGroup dataGroupItemEngagementTemplateNotification = buildDataGroupItem(
            "Repository Group Engagement Template Notification",
            "Repository Group Engagement Template Notification");

        BaseProductGroup baseProductGroupTemplateCustom = buildBaseProductGroup("Repository Group Template Custom",
            "Repository Group Template Custom", BaseProductGroup.ProductGroupTypeEnum.REPOSITORIES,
            "template-custom");
        BaseProductGroup baseProductGroupEngagementTemplateCustom = buildBaseProductGroup(
            "Repository Group Engagement Template Custom",
            "Repository Group Engagement Template Custom", BaseProductGroup.ProductGroupTypeEnum.REPOSITORIES,
            "engagement-template-custom");
        BaseProductGroup baseProductGroupEngagementTemplateNotification = buildBaseProductGroup(
            "Repository Group Engagement Template Notification",
            "Repository Group Engagement Template Notification", BaseProductGroup.ProductGroupTypeEnum.REPOSITORIES,
            "engagement-template-notification");
        when(dataGroupIntegrationApi.batchUpdateDataItems(any())).thenReturn(
            Flux.just(new com.backbase.accesscontrol.datagroup.api.integration.v1.model.BatchResponseItemExtended()
                .status(StatusEnum.HTTP_STATUS_OK)
                .resourceId("test-resource-id")));

        // When
        subject.updateExistingDataGroupsBatch(batchProductGroupTask,
                List.of(dataGroupItemTemplateCustom,
                    dataGroupItemEngagementTemplateCustom,
                    dataGroupItemEngagementTemplateNotification),
                List.of(baseProductGroupTemplateCustom,
                    baseProductGroupEngagementTemplateCustom,
                    baseProductGroupEngagementTemplateNotification))
            .block();

        // Then
        verify(dataGroupIntegrationApi).batchUpdateDataItems(presentationDataGroupItemPutRequestBodyCaptor.capture());
        assertEquals(3, presentationDataGroupItemPutRequestBodyCaptor.getValue().stream()
            .map(DataItemBatchUpdate::getAction)
            .filter(com.backbase.accesscontrol.datagroup.api.integration.v1.model.Action.ADD::equals)
            .toList()
            .size());
    }

    @Test
    void updateExistingDataGroupsBatchWithIncorrectInDbsIngestionModeReplace() {
        // Given
        BatchProductGroupTask batchProductGroupTask = new BatchProductGroupTask();
        batchProductGroupTask.setIngestionMode(BatchProductIngestionMode.REPLACE);
        batchProductGroupTask.setBatchProductGroup(new BatchProductGroup().productGroups(
                List.of(new BaseProductGroup().name("Test product group")))
            .serviceAgreement(new ServiceAgreement().externalId("sa-external-id")));

        DataGroup dataGroupItemTemplateCustom = buildDataGroupItem("Repository Group Template Custom",
            "Repository Group Template Custom", "template-custom-test");
        DataGroup dataGroupItemEngagementTemplateCustom = buildDataGroupItem(
            "Repository Group Engagement Template Custom",
            "Repository Group Engagement Template Custom", "engagement-template-custom-test");
        DataGroup dataGroupItemEngagementTemplateNotification = buildDataGroupItem(
            "Repository Group Engagement Template Notification",
            "Repository Group Engagement Template Notification", "engagement-template-notification-test");

        BaseProductGroup baseProductGroupTemplateCustom = buildBaseProductGroup("Repository Group Template Custom",
            "Repository Group Template Custom", BaseProductGroup.ProductGroupTypeEnum.REPOSITORIES,
            "template-custom");
        BaseProductGroup baseProductGroupEngagementTemplateCustom = buildBaseProductGroup(
            "Repository Group Engagement Template Custom",
            "Repository Group Engagement Template Custom", BaseProductGroup.ProductGroupTypeEnum.REPOSITORIES,
            "engagement-template-custom");
        BaseProductGroup baseProductGroupEngagementTemplateNotification = buildBaseProductGroup(
            "Repository Group Engagement Template Notification",
            "Repository Group Engagement Template Notification", BaseProductGroup.ProductGroupTypeEnum.REPOSITORIES,
            "engagement-template-notification");
        when(dataGroupIntegrationApi.batchUpdateDataItems(any()))
            .thenReturn(
                Flux.just(new com.backbase.accesscontrol.datagroup.api.integration.v1.model.BatchResponseItemExtended()
                    .status(StatusEnum.HTTP_STATUS_OK)
                    .resourceId("test-resource-id")));

        // When
        subject.updateExistingDataGroupsBatch(batchProductGroupTask,
                List.of(dataGroupItemTemplateCustom,
                    dataGroupItemEngagementTemplateCustom,
                    dataGroupItemEngagementTemplateNotification),
                List.of(baseProductGroupTemplateCustom,
                    baseProductGroupEngagementTemplateCustom,
                    baseProductGroupEngagementTemplateNotification))
            .block();

        // Then
        verify(dataGroupIntegrationApi).batchUpdateDataItems(presentationDataGroupItemPutRequestBodyCaptor.capture());
        List<DataItemBatchUpdate> actions = presentationDataGroupItemPutRequestBodyCaptor.getValue();
        assertEquals(6, actions.size());
        assertTrue(
            actionForItemIsPresent(actions, com.backbase.accesscontrol.datagroup.api.integration.v1.model.Action.REMOVE,
                "template-custom-test"));
        assertTrue(
            actionForItemIsPresent(actions, com.backbase.accesscontrol.datagroup.api.integration.v1.model.Action.REMOVE,
                "engagement-template-custom-test"));
        assertTrue(
            actionForItemIsPresent(actions, com.backbase.accesscontrol.datagroup.api.integration.v1.model.Action.REMOVE,
                "engagement-template-notification-test"));
        assertTrue(
            actionForItemIsPresent(actions, com.backbase.accesscontrol.datagroup.api.integration.v1.model.Action.ADD,
                "template-custom"));
        assertTrue(
            actionForItemIsPresent(actions, com.backbase.accesscontrol.datagroup.api.integration.v1.model.Action.ADD,
                "engagement-template-custom"));
        assertTrue(
            actionForItemIsPresent(actions, com.backbase.accesscontrol.datagroup.api.integration.v1.model.Action.ADD,
                "engagement-template-notification"));
    }

    @Test
    void updateExistingDataGroupsDoesNotRemoveCustomDataGroups() {
        // Given
        BatchProductGroupTask batchProductGroupTask = new BatchProductGroupTask();
        batchProductGroupTask.setIngestionMode(BatchProductIngestionMode.REPLACE);
        batchProductGroupTask.setBatchProductGroup(new BatchProductGroup().productGroups(
                List.of(new BaseProductGroup().name("Test product group")))
            .serviceAgreement(new ServiceAgreement().externalId("sa-external-id")));

        DataGroup existingDGroupItemCustom = buildDataGroupItem("Custom data group item",
            "custom desc", "custom-dg-item1", "custom-dg-item2");
        DataGroup existingDGroupItemRepository = buildDataGroupItem("Repository data group item",
            "rep desc", "repository-dg-item1");

        BaseProductGroup upsertProductGroupCustom = buildBaseProductGroup("Custom data group item",
            "custom desc", ProductGroupTypeEnum.CUSTOM,
            "custom-dg-item1", "custom-dg-item2");
        BaseProductGroup upsertProductGroupRepository = buildBaseProductGroup("Repository data group item",
            "rep desc", BaseProductGroup.ProductGroupTypeEnum.REPOSITORIES,
            "repository-dg-item2");

        when(dataGroupIntegrationApi.batchUpdateDataItems(any()))
            .thenReturn(
                Flux.just(new com.backbase.accesscontrol.datagroup.api.integration.v1.model.BatchResponseItemExtended()
                    .status(StatusEnum.HTTP_STATUS_OK)
                    .resourceId("test-resource-id")));

        // When
        subject.updateExistingDataGroupsBatch(batchProductGroupTask,
                List.of(existingDGroupItemCustom, existingDGroupItemRepository),
                List.of(upsertProductGroupCustom, upsertProductGroupRepository))
            .block();

        // Then
        verify(dataGroupIntegrationApi).batchUpdateDataItems(presentationDataGroupItemPutRequestBodyCaptor.capture());
        List<DataItemBatchUpdate> actions = presentationDataGroupItemPutRequestBodyCaptor.getValue();
        assertEquals(2, actions.size());

        assertTrue(
            actionForItemIsPresent(actions, com.backbase.accesscontrol.datagroup.api.integration.v1.model.Action.ADD,
                "repository-dg-item2"));
        assertTrue(
            actionForItemIsPresent(actions, com.backbase.accesscontrol.datagroup.api.integration.v1.model.Action.REMOVE,
                "repository-dg-item1"));
        // the following assertions is to test if for some reason "custom-dg-item1" and "custom-dg-item2" ended up paired with "repository-dg-item*" ;)
        assertFalse(
            actionForItemIsPresent(actions, com.backbase.accesscontrol.datagroup.api.integration.v1.model.Action.REMOVE,
                "custom-dg-item1"));
        assertFalse(
            actionForItemIsPresent(actions, com.backbase.accesscontrol.datagroup.api.integration.v1.model.Action.REMOVE,
                "custom-dg-item2"));
        assertFalse(
            actionForItemIsPresent(actions, com.backbase.accesscontrol.datagroup.api.integration.v1.model.Action.ADD,
                "custom-dg-item1"));
        assertFalse(
            actionForItemIsPresent(actions, com.backbase.accesscontrol.datagroup.api.integration.v1.model.Action.ADD,
                "custom-dg-item2"));
    }

    @Test
    void updateArrangementDataGroupsWhenArrangementAlreadyExists() {
        BatchProductGroupTask batchProductGroupTask = new BatchProductGroupTask();
        batchProductGroupTask.setIngestionMode(BatchProductIngestionMode.REPLACE);
        batchProductGroupTask.setBatchProductGroup(new BatchProductGroup().productGroups(
            List.of(new BaseProductGroup().name("Test product group"))));

        DataGroup existingDGroup = new DataGroup().id("dgId1").name("arrangement1")
            .addItemsItem("debitAccountInId").serviceAgreementId("saInId");

        BaseProductGroup upsertProductGroupArrangement = new BaseProductGroup()
            .name("arrangement1")
            .internalId("dgId1")
            .productGroupType(ProductGroupTypeEnum.ARRANGEMENTS)
            .addCurrentAccountsItem(new CurrentAccount().internalId("debitAccountInId").name("arrangement1")
                .externalId("debitAccountExId"));

        subject.updateExistingDataGroupsBatch(batchProductGroupTask,
                List.of(existingDGroup),
                List.of(upsertProductGroupArrangement))
            .block();

        verify(dataGroupIntegrationApi, times(0)).batchUpdateDataGroups(any());

    }

    @Test
    void updateArrangementDataGroupsWhenArrangementItemDoesNotExist() {
        BatchProductGroupTask batchProductGroupTask = new BatchProductGroupTask();
        batchProductGroupTask.setIngestionMode(BatchProductIngestionMode.REPLACE);
        batchProductGroupTask.setBatchProductGroup(new BatchProductGroup().productGroups(
                List.of(new BaseProductGroup().name("Test product group")))
            .serviceAgreement(new ServiceAgreement().externalId("sa-external-id")));

        DataGroup existingDGroup = new DataGroup().id("debitAccountInId1").name("arrangement1")
            .addItemsItem("debitAccountExId1").serviceAgreementId("saInId");

        when(dataGroupIntegrationApi.batchUpdateDataItems(any()))
            .thenReturn(
                Flux.just(new com.backbase.accesscontrol.datagroup.api.integration.v1.model.BatchResponseItemExtended()
                    .status(StatusEnum.HTTP_STATUS_OK)
                    .resourceId("test-resource-id")));

        BaseProductGroup upsertProductGroupArrangement = new BaseProductGroup()
            .name("arrangement1")
            .internalId("debitAccountInId")
            .productGroupType(ProductGroupTypeEnum.ARRANGEMENTS)
            .addCurrentAccountsItem(new CurrentAccount().name("arrangement1").internalId("debitAccountInId2")
                .externalId("debitAccountExId2"));

        subject.updateExistingDataGroupsBatch(batchProductGroupTask,
                List.of(existingDGroup),
                List.of(upsertProductGroupArrangement))
            .block();

        verify(dataGroupIntegrationApi).batchUpdateDataItems(presentationDataGroupItemPutRequestBodyCaptor.capture());
        List<DataItemBatchUpdate> actions = presentationDataGroupItemPutRequestBodyCaptor.getValue();
        assertEquals(2, actions.size());
        assertTrue(
            actionForItemIsPresent(actions, com.backbase.accesscontrol.datagroup.api.integration.v1.model.Action.ADD,
                "debitAccountInId2"));
        assertTrue(
            actionForItemIsPresent(actions, com.backbase.accesscontrol.datagroup.api.integration.v1.model.Action.REMOVE,
                "debitAccountExId1"));
    }

    @Test
    void updateExistingDataGroupsHandleError() {

        BatchProductGroupTask batchProductGroupTask = new BatchProductGroupTask();
        batchProductGroupTask.setIngestionMode(BatchProductIngestionMode.REPLACE);
        batchProductGroupTask.setBatchProductGroup(new BatchProductGroup()
            .serviceAgreement(new ServiceAgreement())
            .productGroups(List.of(new BaseProductGroup().name("Test product group"))));

        DataGroup existingDGroupItemCustom = buildDataGroupItem("Custom data group item",
            "custom desc", "custom-dg-item1");

        BaseProductGroup upsertProductGroupCustom = buildBaseProductGroup("Custom data group item",
            "custom desc", ProductGroupTypeEnum.CUSTOM,
            "custom-dg-item2");
        when(dataGroupIntegrationApi.batchUpdateDataItems(any())).thenReturn(
            Flux.error(WebClientResponseException.create(500, "Internal error", null, null, null, null)));

        assertThrows(StreamTaskException.class,
            () -> subject.updateExistingDataGroupsBatch(batchProductGroupTask, List.of(existingDGroupItemCustom),
                List.of(upsertProductGroupCustom)).block());

        assertEquals("Failed to update product groups", batchProductGroupTask.getError());
    }

    @Test
    void updateExistingDataGroupsBatchWithEmptyDbsIngestionModeReplace() {
        // Given
        BatchProductGroupTask batchProductGroupTask = new BatchProductGroupTask();
        batchProductGroupTask.setIngestionMode(BatchProductIngestionMode.REPLACE);
        batchProductGroupTask.setBatchProductGroup(new BatchProductGroup().productGroups(
            List.of(new BaseProductGroup().name("Test product group"))));

        BaseProductGroup baseProductGroupTemplateCustom = buildBaseProductGroup("Repository Group Template Custom",
            "Repository Group Template Custom", BaseProductGroup.ProductGroupTypeEnum.REPOSITORIES,
            "template-custom");
        BaseProductGroup baseProductGroupEngagementTemplateCustom = buildBaseProductGroup(
            "Repository Group Engagement Template Custom",
            "Repository Group Engagement Template Custom", BaseProductGroup.ProductGroupTypeEnum.REPOSITORIES,
            "engagement-template-custom");
        BaseProductGroup baseProductGroupEngagementTemplateNotification = buildBaseProductGroup(
            "Repository Group Engagement Template Notification",
            "Repository Group Engagement Template Notification", BaseProductGroup.ProductGroupTypeEnum.REPOSITORIES,
            "engagement-template-notification");

        // When
        subject.updateExistingDataGroupsBatch(batchProductGroupTask,
                List.of(),
                List.of(baseProductGroupTemplateCustom,
                    baseProductGroupEngagementTemplateCustom,
                    baseProductGroupEngagementTemplateNotification))
            .block();

        // Then
        verify(dataGroupIntegrationApi, times(0)).batchUpdateDataItems(any());
    }

    /*
       Request contains business-function-group-id-1
       Existing permissions are empty
       Expectation is to have business-function-group-id-1 in PUT permissions request
     */
    @Test
    void assignPermissionsBatchEmptyExistingPermissions() {
        // Given
        BatchProductGroupTask batchProductGroupTask = new BatchProductGroupTask().data(
            new BatchProductGroup().serviceAgreement(
                new ServiceAgreement().externalId("sa_benedict").internalId("sa-internal-id"))
        );
        batchProductGroupTask.setIngestionMode(BatchProductIngestionMode.UPSERT);

        Map<BusinessFunctionGroup, List<BaseProductGroup>> baseProductGroupMap = new HashMap<>();
        baseProductGroupMap.put(new BusinessFunctionGroup().id("business-function-group-id-1"),
            Collections.emptyList());

        Map<User, Map<BusinessFunctionGroup, List<BaseProductGroup>>> usersPermissions = new HashMap<>();
        usersPermissions.put(
            new User().internalId("user-internal-id").externalId("benedict"),
            baseProductGroupMap
        );

        List<UserPermissionItem> expectedPermissions = List.of(
            new UserPermissionItem().functionGroupId("business-function-group-id-1").dataGroupIds(Set.of()));

        when(functionGroupServiceApi.getFunctionGroups("sa-internal-id"))
            .thenReturn(Mono.just(new GetFunctionGroups()
                .functionGroups(List.of(
                    new FunctionGroupItem().id("system-group-id-1").name("SYSTEM_FUNCTION_GROUP")
                        .type(FunctionGroupItem.TypeEnum.SYSTEM),
                    new FunctionGroupItem().id("system-group-id-2").name("Full access")
                        .type(TypeEnum.REFERENCE)
                ))
            ));

        when(assignPermissionsServiceApi.getUserPermissions("user-internal-id", "sa-internal-id"))
            .thenReturn(Mono.just(new UserPermissions().permissions(List.of())));

        when(assignPermissionsServiceApi.putUserPermissions(any(), any(), any())).thenReturn(Mono.empty());

        // When
        BatchProductGroupTask result = subject.assignPermissionsBatch(batchProductGroupTask, usersPermissions)
            .block();

        // Then
        Assertions.assertSame(batchProductGroupTask, result);

        verify(assignPermissionsServiceApi).putUserPermissions("user-internal-id", "sa-internal-id",
            expectedPermissions);
    }

    /*
       Request contains business-function-group-id-1
       Existing permissions returns only SFG system-group-id-1
       Expectation is to have business-function-group-id-1 in PUT permissions request
     */
    @Test
    void assignPermissionsBatchOnlySystemFunctionGroupExists() {
        // Given
        BatchProductGroupTask batchProductGroupTask = new BatchProductGroupTask().data(
            new BatchProductGroup().serviceAgreement(
                new ServiceAgreement().externalId("sa_benedict").internalId("sa-internal-id"))
        );
        batchProductGroupTask.setIngestionMode(BatchProductIngestionMode.UPSERT);

        Map<BusinessFunctionGroup, List<BaseProductGroup>> baseProductGroupMap = new HashMap<>();
        baseProductGroupMap.put(
            new BusinessFunctionGroup().id("business-function-group-id-1"),
            Collections.singletonList(new BaseProductGroup().internalId("data-group-0"))
        );

        Map<User, Map<BusinessFunctionGroup, List<BaseProductGroup>>> usersPermissions = new HashMap<>();
        usersPermissions.put(
            new User().internalId("user-internal-id").externalId("benedict"),
            baseProductGroupMap
        );

        List<UserPermissionItem> expectedPermissions = List.of(
            new UserPermissionItem().functionGroupId("business-function-group-id-1")
                .dataGroupIds(Set.of("data-group-0")));

        when(functionGroupServiceApi.getFunctionGroups("sa-internal-id"))
            .thenReturn(Mono.just(new GetFunctionGroups()
                .functionGroups(List.of(
                    new FunctionGroupItem().id("system-group-id-1").name("SYSTEM_FUNCTION_GROUP")
                        .type(FunctionGroupItem.TypeEnum.SYSTEM)
                ))
            ));

        when(assignPermissionsServiceApi.getUserPermissions("user-internal-id", "sa-internal-id"))
            .thenReturn(Mono.just(new UserPermissions().permissions(List.of(
                new UserPermissionItem().functionGroupId("system-group-id-1")
                    .dataGroupIds(Set.of("system-data-group-1", "system-data-group-2"))
            ))));

        when(assignPermissionsServiceApi.putUserPermissions(any(), any(), any())).thenReturn(Mono.empty());

        // When
        BatchProductGroupTask result = subject.assignPermissionsBatch(batchProductGroupTask, usersPermissions)
            .block();

        // Then
        Assertions.assertSame(batchProductGroupTask, result);

        verify(assignPermissionsServiceApi).putUserPermissions("user-internal-id", "sa-internal-id",
            expectedPermissions);
    }

    @Test
    void deleteFunctionGroupsForServiceAgreement_noneTypeConfigured_doesNotInvokeDeletion() {
        String internalSaId = "sa-internal-id";
        String externalSaId = "external-id";

        when(configurationProperties.getFunctionGroupItemType()).thenReturn(
            DeletionProperties.FunctionGroupItemType.NONE);

        subject.deleteFunctionGroupsForServiceAgreement(internalSaId, externalSaId).block();

        verify(functionGroupIntegrationApi, times(0)).batchDeleteFunctionGroups(any());
    }

    @Test
    void deleteFunctionGroupsForServiceAgreement_templateTypeConfigured_deletesOnlyTemplateType() {
        String internalSaId = "sa-internal-id";
        String externalSaId = "external-id";

        FunctionGroupItem systemFunctionGroup = new FunctionGroupItem().id("system-group-id-1")
            .name("SYSTEM_FUNCTION_GROUP")
            .serviceAgreementId(internalSaId)
            .type(TypeEnum.SYSTEM);

        FunctionGroupItem templateFunctionGroup = new FunctionGroupItem().id("template-group-id-2").name("Full access")
            .serviceAgreementId(internalSaId)
            .type(TypeEnum.REFERENCE);

        when(functionGroupServiceApi.getFunctionGroups(internalSaId))
            .thenReturn(Mono.just(new GetFunctionGroups()
                .functionGroups(List.of(systemFunctionGroup, templateFunctionGroup))));

        when(functionGroupIntegrationApi.batchDeleteFunctionGroups(any())).thenReturn(Flux.empty());

        when(configurationProperties.getFunctionGroupItemType()).thenReturn(
            DeletionProperties.FunctionGroupItemType.TEMPLATE);

        subject.deleteFunctionGroupsForServiceAgreement(internalSaId, externalSaId).block();

        ArgumentCaptor<List<FunctionGroupNameIdentifier>> captor = ArgumentCaptor.forClass(List.class);
        verify(functionGroupIntegrationApi).batchDeleteFunctionGroups(captor.capture());

        List<FunctionGroupNameIdentifier> value = captor.getValue();
        assertEquals(templateFunctionGroup.getName(), value.get(0).getName());
        assertEquals(externalSaId, value.get(0).getServiceAgreementExternalId());
    }

    @Test
    void testUpdateServiceAgreementItem() {
        StreamTask streamTask = Mockito.mock(StreamTask.class);

        ServiceAgreement serviceAgreement = new ServiceAgreement();
        serviceAgreement.setExternalId("external-id");
        serviceAgreement.setInternalId("internal-id");
        serviceAgreement.setName("name");

        when(serviceAgreementServiceApi.putServiceAgreement(any(), any())).thenReturn(Mono.empty());

        Mono<ServiceAgreement> resultMono = subject.updateServiceAgreementItem(streamTask, serviceAgreement);

        StepVerifier.create(resultMono)
            .expectNext(serviceAgreement)
            .verifyComplete();

        verify(serviceAgreementServiceApi, times(1))
            .putServiceAgreement("internal-id", new ServiceAgreementUpdateRequest()
                .name("name")
                .externalId("external-id"));

    }

    @Test
    void testUpdateServiceAgreementItemFailed() {
        StreamTask streamTask = Mockito.mock(StreamTask.class);

        ServiceAgreement serviceAgreement = new ServiceAgreement();
        serviceAgreement.setExternalId("external-id");
        serviceAgreement.setInternalId("internal-id");

        when(serviceAgreementServiceApi.putServiceAgreement(any(), any()))
            .thenReturn(Mono.error(new WebClientResponseException(BAD_REQUEST, "Bad request", null, null, null, null)));

        Mono<ServiceAgreement> resultMono = subject.updateServiceAgreementItem(streamTask, serviceAgreement);

        StepVerifier.create(resultMono)
            .verifyError(StreamTaskException.class);

        verify(serviceAgreementServiceApi, times(1))
            .putServiceAgreement(eq("internal-id"), any());
    }

    private void thenRegularUsersUpdateCall(String expectedSaExId, Action expectedAction,
        String... expectedUserIds) {
        ServiceAgreementUsersBatchUpdateRequest expectedRegularUserAddUpdate =
            new ServiceAgreementUsersBatchUpdateRequest().action(expectedAction)
                .users(Stream.of(expectedUserIds).map(userId -> new ServiceAgreementUserExternal()
                    .externalUserId(userId).externalServiceAgreementId(expectedSaExId)).collect(Collectors.toList()));
        verify(serviceAgreementIntegrationApi, times(1))
            .batchUpdateServiceAgreementUsers(eq(expectedRegularUserAddUpdate));
    }

    private void thenUpdateParticipantsCall(InOrder validator, String expectedSaExId, Action expectedAction,
        ExpectedParticipantUpdate... expectedParticipants) {
        List<UpdateParticipantItem> expectedRequest = Stream.of(expectedParticipants)
            .map(ep -> new UpdateParticipantItem()
                .externalServiceAgreementId(expectedSaExId).externalLegalEntityId(ep.exId)
                .sharingAccounts(ep.sharingAccounts).sharingUsers(ep.sharingAccounts).action(expectedAction))
            .collect(Collectors.toList());
        validator.verify(serviceAgreementIntegrationApi).batchUpdateParticipants(eq(expectedRequest));
    }

    private ServiceAgreement buildInputServiceAgreement(String saInternalId, String saExternalId, String description,
        String name) {
        return new ServiceAgreement()
            .internalId(saInternalId)
            .externalId(saExternalId)
            .description(description)
            .status(ENABLED)
            .name(name)
            .validFrom(VALID_FROM)
            .validUntil(VALID_UNTIL);
    }

    private DataGroup buildDataGroupItem(String name, String description, String... items) {
        return new DataGroup()
            .name(name)
            .description(description)
            .items(Set.of(items));
    }

    private BaseProductGroup buildBaseProductGroup(String name, String description,
        BaseProductGroup.ProductGroupTypeEnum productGroupTypeEnum, String... dataGroupItemId) {
        ProductGroup productGroup = new ProductGroup();
        productGroup.setName(name);
        productGroup.setDescription(description);
        productGroup.setProductGroupType(productGroupTypeEnum);
        productGroup.setCustomDataGroupItems(Arrays.stream(dataGroupItemId)
            .map(dgi -> new CustomDataGroupItem().internalId(dgi).externalId(dgi)).toList());
        return productGroup;
    }

    private boolean actionForItemIsPresent(List<DataItemBatchUpdate> actions,
        com.backbase.accesscontrol.datagroup.api.integration.v1.model.Action expectedAction, String expectedItem) {
        return actions.stream().anyMatch(a -> a.getAction().equals(expectedAction)
            && a.getDataItems().stream().anyMatch(expectedItem::equals));
    }

    @AllArgsConstructor
    private static class ExpectedParticipantUpdate {

        String exId;
        boolean sharingAccounts;
        boolean sharingUsers;
    }

    @Test
    void testGetUserContextsByUserId_success() {
        var getContexts = new GetContexts().contextServiceAgreements(
            List.of(new ContextServiceAgreement().id("sa_id"))).totalCount(1L);

        when(userContextApi.getUserContexts(anyString(), any(), any(), any()))
            .thenReturn(Mono.just(getContexts));
        when(accessControlProperties.getUserContextPageSize()).thenReturn(10);

        StepVerifier.create(subject.getUserContextsByUserId(userInternalId))
            .expectNextMatches(serviceAgreements -> serviceAgreements.size() == 1)
            .verifyComplete();

        verify(userContextApi).getUserContexts(userInternalId, null, 0, 10);
        verify(accessControlProperties).getUserContextPageSize();
    }

    @Test
    void testGetUserContextsByUserId_emptyResult() {
        when(userContextApi.getUserContexts(anyString(), any(), any(), any()))
            .thenReturn(Mono.just(getEmptyContext()));
        when(accessControlProperties.getUserContextPageSize()).thenReturn(10);

        StepVerifier.create(subject.getUserContextsByUserId(userInternalId))
            .expectNext(Collections.emptyList())
            .verifyComplete();

        verify(userContextApi).getUserContexts(userInternalId, null, 0, 10);
        verify(accessControlProperties).getUserContextPageSize();
    }

    private GetContexts getEmptyContext() {
        return new GetContexts().contextServiceAgreements(Collections.emptyList()).totalCount(0L);
    }

    @Test
    void testGetUserContextsByUserId_webClientResponseException4xx() {
        when(userContextApi.getUserContexts(anyString(), any(), any(), any()))
            .thenReturn(Mono.error(new WebClientResponseException("Not Found", 404, "Not Found", null, null, null)));
        when(accessControlProperties.getUserContextPageSize()).thenReturn(10);

        StepVerifier.create(subject.getUserContextsByUserId(userInternalId))
            .expectErrorMatches(throwable -> throwable instanceof WebClientResponseException
                && HttpStatus.NOT_FOUND.equals(((WebClientResponseException) throwable).getStatusCode()))
            .verify();
        verify(userContextApi).getUserContexts(userInternalId, null, 0, 10);
        verify(accessControlProperties).getUserContextPageSize();
    }

    @Test
    void testGetUserContextsByUserId_nullFrom() {
        when(userContextApi.getUserContexts(anyString(), any(), any(), any()))
            .thenReturn(Mono.just(getEmptyContext()));
        when(accessControlProperties.getUserContextPageSize()).thenReturn(10);

        StepVerifier.create(subject.getUserContextsByUserId(userInternalId))
            .expectNextMatches(List::isEmpty)
            .verifyComplete();

        verify(userContextApi).getUserContexts(userInternalId, null, 0, 10);
        verify(accessControlProperties).getUserContextPageSize();
    }

    @Test
    void testGetUserContextsByUserId_pagination() {
        var page1 = new GetContexts().contextServiceAgreements(List.of(new ContextServiceAgreement().id("sa_1")))
            .totalCount(2L);
        var page2 = new GetContexts().contextServiceAgreements(List.of(new ContextServiceAgreement().id("sa_2")))
            .totalCount(2L);

        when(userContextApi.getUserContexts(eq(userInternalId), any(), eq(0), any()))
            .thenReturn(Mono.just(page1));
        when(userContextApi.getUserContexts(eq(userInternalId), any(), eq(1), any()))
            .thenReturn(Mono.just(page2));
        when(accessControlProperties.getUserContextPageSize()).thenReturn(1);

        StepVerifier.create(subject.getUserContextsByUserId(userInternalId))
            .expectNextMatches(serviceAgreements -> serviceAgreements.size() == 2
                && serviceAgreements.stream().anyMatch(sa -> sa.getInternalId().equals("sa_1"))
                && serviceAgreements.stream().anyMatch(sa -> sa.getInternalId().equals("sa_2")))
            .verifyComplete();

        verify(userContextApi, times(1)).getUserContexts(eq(userInternalId), any(), eq(0), any());
        verify(userContextApi, times(1)).getUserContexts(eq(userInternalId), any(), eq(1), any());
        verify(accessControlProperties).getUserContextPageSize();
    }

    @Test
    void testGetUserContextsByUserId_paginationLargeNumber() {
        var page1 = new GetContexts().contextServiceAgreements(
            Collections.nCopies(25, new ContextServiceAgreement().id("sa_1"))).totalCount(120L);
        var page2 = new GetContexts().contextServiceAgreements(
            Collections.nCopies(25, new ContextServiceAgreement().id("sa_2"))).totalCount(120L);
        var page3 = new GetContexts().contextServiceAgreements(
            Collections.nCopies(25, new ContextServiceAgreement().id("sa_3"))).totalCount(120L);
        var page4 = new GetContexts().contextServiceAgreements(
            Collections.nCopies(25, new ContextServiceAgreement().id("sa_4"))).totalCount(120L);
        var page5 = new GetContexts().contextServiceAgreements(
            Collections.nCopies(20, new ContextServiceAgreement().id("sa_5"))).totalCount(120L);

        when(userContextApi.getUserContexts(eq(userInternalId), any(), eq(0), any()))
            .thenReturn(Mono.just(page1));
        when(userContextApi.getUserContexts(eq(userInternalId), any(), eq(1), any()))
            .thenReturn(Mono.just(page2));
        when(userContextApi.getUserContexts(eq(userInternalId), any(), eq(2), any()))
            .thenReturn(Mono.just(page3));
        when(userContextApi.getUserContexts(eq(userInternalId), any(), eq(3), any()))
            .thenReturn(Mono.just(page4));
        when(userContextApi.getUserContexts(eq(userInternalId), any(), eq(4), any()))
            .thenReturn(Mono.just(page5));
        when(accessControlProperties.getUserContextPageSize()).thenReturn(25);

        StepVerifier.create(subject.getUserContextsByUserId(userInternalId))
            .expectNextMatches(serviceAgreements -> serviceAgreements.size() == 120)
            .verifyComplete();

        verify(userContextApi, times(1)).getUserContexts(eq(userInternalId), any(), eq(0), any());
        verify(userContextApi, times(1)).getUserContexts(eq(userInternalId), any(), eq(1), any());
        verify(userContextApi, times(1)).getUserContexts(eq(userInternalId), any(), eq(2), any());
        verify(userContextApi, times(1)).getUserContexts(eq(userInternalId), any(), eq(3), any());
        verify(userContextApi, times(1)).getUserContexts(eq(userInternalId), any(), eq(4), any());
        verify(accessControlProperties).getUserContextPageSize();
    }

    @Test
    void testGetUserContextsByUserId_paginationEdgeCases() {
        var page1 = new GetContexts().contextServiceAgreements(
            Collections.nCopies(10, new ContextServiceAgreement().id("sa_1"))).totalCount(11L);
        var page2 = new GetContexts().contextServiceAgreements(
            Collections.nCopies(1, new ContextServiceAgreement().id("sa_2"))).totalCount(11L);

        when(userContextApi.getUserContexts(eq(userInternalId), any(), eq(0), any()))
            .thenReturn(Mono.just(page1));
        when(userContextApi.getUserContexts(eq(userInternalId), any(), eq(1), any()))
            .thenReturn(Mono.just(page2));
        when(accessControlProperties.getUserContextPageSize()).thenReturn(10);

        StepVerifier.create(subject.getUserContextsByUserId(userInternalId))
            .expectNextMatches(serviceAgreements -> serviceAgreements.size() == 11)
            .verifyComplete();

        verify(userContextApi, times(1)).getUserContexts(eq(userInternalId), any(), eq(0), any());
        verify(userContextApi, times(1)).getUserContexts(eq(userInternalId), any(), eq(1), any());
        verify(accessControlProperties).getUserContextPageSize();
    }

    @Test
    void shouldGetAllFunctionGroupPagesWhenLastPageNotFull() {
        String serviceAgreementId = "sa-internal-id";

        var userIdsPage1 = Stream.of("id1", "id2", "id3", "id4", "id5", "id6", "id7", "id8", "id9", "id10", "id11",
                "id12").map(id -> new com.backbase.accesscontrol.serviceagreement.api.service.v1.model.User().userId(id))
            .toList();
        var userIdsPage2 = Stream.of("id13", "id14", "id15", "id16", "id17", "id18", "id19", "id20", "id21",
                "id22", "id23", "id24")
            .map(id -> new com.backbase.accesscontrol.serviceagreement.api.service.v1.model.User().userId(id)).toList();
        var userIdsPage3 = Stream.of("id25", "id26", "id27", "id28", "id29", "id30", "id31", "id32", "id33",
                "id34").map(id -> new com.backbase.accesscontrol.serviceagreement.api.service.v1.model.User().userId(id))
            .toList();

        String page2Cursor = "cursor-2";
        String page3Cursor = "cursor-3";
        String page4Cursor = "cursor-4";

        when(serviceAgreementServiceApi.getServiceAgreementUsers(serviceAgreementId, null, 12))
            .thenReturn(Mono.just(new ServiceAgreementUsers().userIds(userIdsPage1).nextPage(page2Cursor)));
        when(serviceAgreementServiceApi.getServiceAgreementUsers(serviceAgreementId, page2Cursor, 12))
            .thenReturn(Mono.just(new ServiceAgreementUsers().userIds(userIdsPage2).nextPage(page3Cursor)));
        when(serviceAgreementServiceApi.getServiceAgreementUsers(serviceAgreementId, page3Cursor, 12))
            .thenReturn(Mono.just(new ServiceAgreementUsers().userIds(userIdsPage3).nextPage(page4Cursor)));

        StepVerifier.create(subject.fetchAllUsersPages(serviceAgreementId, null, 12))
            .expectNextMatches(userId -> userId.equals("id1"))
            .expectNextMatches(userId -> userId.equals("id2"))
            .expectNextMatches(userId -> userId.equals("id3"))
            .expectNextMatches(userId -> userId.equals("id4"))
            .expectNextMatches(userId -> userId.equals("id5"))
            .expectNextMatches(userId -> userId.equals("id6"))
            .expectNextMatches(userId -> userId.equals("id7"))
            .expectNextMatches(userId -> userId.equals("id8"))
            .expectNextMatches(userId -> userId.equals("id9"))
            .expectNextMatches(userId -> userId.equals("id10"))
            .expectNextMatches(userId -> userId.equals("id11"))
            .expectNextMatches(userId -> userId.equals("id12"))
            .expectNextMatches(userId -> userId.equals("id13"))
            .expectNextMatches(userId -> userId.equals("id14"))
            .expectNextMatches(userId -> userId.equals("id15"))
            .expectNextMatches(userId -> userId.equals("id16"))
            .expectNextMatches(userId -> userId.equals("id17"))
            .expectNextMatches(userId -> userId.equals("id18"))
            .expectNextMatches(userId -> userId.equals("id19"))
            .expectNextMatches(userId -> userId.equals("id20"))
            .expectNextMatches(userId -> userId.equals("id21"))
            .expectNextMatches(userId -> userId.equals("id22"))
            .expectNextMatches(userId -> userId.equals("id23"))
            .expectNextMatches(userId -> userId.equals("id24"))
            .expectNextMatches(userId -> userId.equals("id25"))
            .expectNextMatches(userId -> userId.equals("id26"))
            .expectNextMatches(userId -> userId.equals("id27"))
            .expectNextMatches(userId -> userId.equals("id28"))
            .expectNextMatches(userId -> userId.equals("id29"))
            .expectNextMatches(userId -> userId.equals("id30"))
            .expectNextMatches(userId -> userId.equals("id31"))
            .expectNextMatches(userId -> userId.equals("id32"))
            .expectNextMatches(userId -> userId.equals("id33"))
            .expectNextMatches(userId -> userId.equals("id34"))
            .verifyComplete();

        verify(serviceAgreementServiceApi).getServiceAgreementUsers(serviceAgreementId, null, 12);
        verify(serviceAgreementServiceApi).getServiceAgreementUsers(serviceAgreementId, page2Cursor, 12);
        verify(serviceAgreementServiceApi).getServiceAgreementUsers(serviceAgreementId, page3Cursor, 12);

    }

    @Test
    void shouldGetAllFunctionGroupPagesWhenLastPageFull() {
        String serviceAgreementId = "sa-internal-id";

        var userIdsPage1 = Stream.of("id1", "id2", "id3", "id4", "id5", "id6", "id7", "id8", "id9", "id10", "id11",
                "id12").map(id -> new com.backbase.accesscontrol.serviceagreement.api.service.v1.model.User().userId(id))
            .toList();
        var userIdsPage2 = Stream.of("id13", "id14", "id15", "id16", "id17", "id18", "id19", "id20", "id21",
                "id22", "id23", "id24")
            .map(id -> new com.backbase.accesscontrol.serviceagreement.api.service.v1.model.User().userId(id)).toList();
        var userIdsPage3 = Stream.of("id25", "id26", "id27", "id28", "id29", "id30", "id31", "id32", "id33",
                "id34", "id35", "id36")
            .map(id -> new com.backbase.accesscontrol.serviceagreement.api.service.v1.model.User().userId(id)).toList();

        String page2Cursor = "cursor-2";
        String page3Cursor = "cursor-3";
        String page4Cursor = "cursor-4";

        when(serviceAgreementServiceApi.getServiceAgreementUsers(serviceAgreementId, null, 12))
            .thenReturn(Mono.just(new ServiceAgreementUsers().userIds(userIdsPage1).nextPage(page2Cursor)));
        when(serviceAgreementServiceApi.getServiceAgreementUsers(serviceAgreementId, page2Cursor, 12))
            .thenReturn(Mono.just(new ServiceAgreementUsers().userIds(userIdsPage2).nextPage(page3Cursor)));
        when(serviceAgreementServiceApi.getServiceAgreementUsers(serviceAgreementId, page3Cursor, 12))
            .thenReturn(Mono.just(new ServiceAgreementUsers().userIds(userIdsPage3).nextPage(page4Cursor)));
        when(serviceAgreementServiceApi.getServiceAgreementUsers(serviceAgreementId, page4Cursor, 12))
            .thenReturn(Mono.just(new ServiceAgreementUsers().userIds(List.of()).nextPage(page4Cursor)));

        StepVerifier.create(subject.fetchAllUsersPages(serviceAgreementId, null, 12))
            .expectNextMatches(userId -> userId.equals("id1"))
            .expectNextMatches(userId -> userId.equals("id2"))
            .expectNextMatches(userId -> userId.equals("id3"))
            .expectNextMatches(userId -> userId.equals("id4"))
            .expectNextMatches(userId -> userId.equals("id5"))
            .expectNextMatches(userId -> userId.equals("id6"))
            .expectNextMatches(userId -> userId.equals("id7"))
            .expectNextMatches(userId -> userId.equals("id8"))
            .expectNextMatches(userId -> userId.equals("id9"))
            .expectNextMatches(userId -> userId.equals("id10"))
            .expectNextMatches(userId -> userId.equals("id11"))
            .expectNextMatches(userId -> userId.equals("id12"))
            .expectNextMatches(userId -> userId.equals("id13"))
            .expectNextMatches(userId -> userId.equals("id14"))
            .expectNextMatches(userId -> userId.equals("id15"))
            .expectNextMatches(userId -> userId.equals("id16"))
            .expectNextMatches(userId -> userId.equals("id17"))
            .expectNextMatches(userId -> userId.equals("id18"))
            .expectNextMatches(userId -> userId.equals("id19"))
            .expectNextMatches(userId -> userId.equals("id20"))
            .expectNextMatches(userId -> userId.equals("id21"))
            .expectNextMatches(userId -> userId.equals("id22"))
            .expectNextMatches(userId -> userId.equals("id23"))
            .expectNextMatches(userId -> userId.equals("id24"))
            .expectNextMatches(userId -> userId.equals("id25"))
            .expectNextMatches(userId -> userId.equals("id26"))
            .expectNextMatches(userId -> userId.equals("id27"))
            .expectNextMatches(userId -> userId.equals("id28"))
            .expectNextMatches(userId -> userId.equals("id29"))
            .expectNextMatches(userId -> userId.equals("id30"))
            .expectNextMatches(userId -> userId.equals("id31"))
            .expectNextMatches(userId -> userId.equals("id32"))
            .expectNextMatches(userId -> userId.equals("id33"))
            .expectNextMatches(userId -> userId.equals("id34"))
            .expectNextMatches(userId -> userId.equals("id35"))
            .expectNextMatches(userId -> userId.equals("id36"))
            .verifyComplete();

        verify(serviceAgreementServiceApi).getServiceAgreementUsers(serviceAgreementId, null, 12);
        verify(serviceAgreementServiceApi).getServiceAgreementUsers(serviceAgreementId, page2Cursor, 12);
        verify(serviceAgreementServiceApi).getServiceAgreementUsers(serviceAgreementId, page3Cursor, 12);
        verify(serviceAgreementServiceApi).getServiceAgreementUsers(serviceAgreementId, page4Cursor, 12);

    }

    @Test
    void shouldGetAllDataGroupPagesWhenLastPageNotFull() {
        String serviceAgreementId = "sa-internal-id";
        String dataGroupType = "data-group-type";

        var page1 = Stream.of("id1", "id2", "id3", "id4", "id5", "id6", "id7", "id8", "id9", "id10", "id11",
                "id12").map(id -> new com.backbase.accesscontrol.datagroup.api.service.v1.model.DataGroup().id(id))
            .toList();
        var page2 = Stream.of("id13", "id14", "id15", "id16", "id17", "id18", "id19", "id20", "id21",
                "id22", "id23", "id24")
            .map(id -> new com.backbase.accesscontrol.datagroup.api.service.v1.model.DataGroup().id(id)).toList();
        var page3 = Stream.of("id25", "id26", "id27", "id28", "id29", "id30", "id31", "id32", "id33",
                "id34").map(id -> new com.backbase.accesscontrol.datagroup.api.service.v1.model.DataGroup().id(id))
            .toList();

        String page2Cursor = "cursor-2";
        String page3Cursor = "cursor-3";
        String page4Cursor = "cursor-4";

        when(dataGroupServiceApi.getDataGroups(serviceAgreementId, dataGroupType, true, null, 12))
            .thenReturn(Mono.just(new GetDataGroups().dataGroups(page1).nextPage(page2Cursor)));
        when(dataGroupServiceApi.getDataGroups(serviceAgreementId, dataGroupType, true, page2Cursor, 12))
            .thenReturn(Mono.just(new GetDataGroups().dataGroups(page2).nextPage(page3Cursor)));
        when(dataGroupServiceApi.getDataGroups(serviceAgreementId, dataGroupType, true, page3Cursor, 12))
            .thenReturn(Mono.just(new GetDataGroups().dataGroups(page3).nextPage(page4Cursor)));

        StepVerifier.create(subject.fetchAllDataGroupPages(serviceAgreementId, dataGroupType, null, 12))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id1"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id2"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id3"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id4"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id5"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id6"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id7"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id8"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id9"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id10"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id11"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id12"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id13"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id14"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id15"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id16"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id17"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id18"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id19"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id20"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id21"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id22"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id23"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id24"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id25"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id26"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id27"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id28"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id29"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id30"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id31"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id32"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id33"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id34"))
            .verifyComplete();

        verify(dataGroupServiceApi).getDataGroups(serviceAgreementId, dataGroupType, true, null, 12);
        verify(dataGroupServiceApi).getDataGroups(serviceAgreementId, dataGroupType, true, page2Cursor, 12);
        verify(dataGroupServiceApi).getDataGroups(serviceAgreementId, dataGroupType, true, page3Cursor, 12);

    }

    @Test
    void shouldGetAllDataGroupPagesWhenLastPageFull() {
        String serviceAgreementId = "sa-internal-id";
        String dataGroupType = "data-group-type";

        var page1 = Stream.of("id1", "id2", "id3", "id4", "id5", "id6", "id7", "id8", "id9", "id10", "id11",
                "id12").map(id -> new com.backbase.accesscontrol.datagroup.api.service.v1.model.DataGroup().id(id))
            .toList();
        var page2 = Stream.of("id13", "id14", "id15", "id16", "id17", "id18", "id19", "id20", "id21",
                "id22", "id23", "id24")
            .map(id -> new com.backbase.accesscontrol.datagroup.api.service.v1.model.DataGroup().id(id)).toList();
        var page3 = Stream.of("id25", "id26", "id27", "id28", "id29", "id30", "id31", "id32", "id33",
                "id34", "id35", "id36").map(id -> new com.backbase.accesscontrol.datagroup.api.service.v1.model.DataGroup().id(id))
            .toList();
        String page2Cursor = "cursor-2";
        String page3Cursor = "cursor-3";
        String page4Cursor = "cursor-4";

        when(dataGroupServiceApi.getDataGroups(serviceAgreementId, dataGroupType, true, null, 12))
            .thenReturn(Mono.just(new GetDataGroups().dataGroups(page1).nextPage(page2Cursor)));
        when(dataGroupServiceApi.getDataGroups(serviceAgreementId, dataGroupType, true, page2Cursor, 12))
            .thenReturn(Mono.just(new GetDataGroups().dataGroups(page2).nextPage(page3Cursor)));
        when(dataGroupServiceApi.getDataGroups(serviceAgreementId, dataGroupType, true, page3Cursor, 12))
            .thenReturn(Mono.just(new GetDataGroups().dataGroups(page3).nextPage(page4Cursor)));
        when(dataGroupServiceApi.getDataGroups(serviceAgreementId, dataGroupType, true, page4Cursor, 12))
            .thenReturn(Mono.just(new GetDataGroups().dataGroups(List.of()).nextPage(page4Cursor)));

        StepVerifier.create(subject.fetchAllDataGroupPages(serviceAgreementId, dataGroupType, null, 12))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id1"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id2"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id3"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id4"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id5"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id6"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id7"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id8"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id9"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id10"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id11"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id12"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id13"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id14"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id15"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id16"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id17"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id18"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id19"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id20"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id21"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id22"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id23"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id24"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id25"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id26"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id27"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id28"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id29"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id30"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id31"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id32"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id33"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id34"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id35"))
            .expectNextMatches(dataGroup -> dataGroup.getId().equals("id36"))
            .verifyComplete();

        verify(dataGroupServiceApi).getDataGroups(serviceAgreementId, dataGroupType, true, null, 12);
        verify(dataGroupServiceApi).getDataGroups(serviceAgreementId, dataGroupType, true, page2Cursor, 12);
        verify(dataGroupServiceApi).getDataGroups(serviceAgreementId, dataGroupType, true, page3Cursor, 12);
        verify(dataGroupServiceApi).getDataGroups(serviceAgreementId, dataGroupType, true, page4Cursor, 12);

    }


}