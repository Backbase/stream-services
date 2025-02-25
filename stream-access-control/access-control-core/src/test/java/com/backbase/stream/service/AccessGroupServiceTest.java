package com.backbase.stream.service;

import static com.backbase.dbs.accesscontrol.api.service.v3.model.BatchResponseItemExtended.StatusEnum.HTTP_STATUS_INTERNAL_SERVER_ERROR;
import static com.backbase.dbs.accesscontrol.api.service.v3.model.BatchResponseItemExtended.StatusEnum.HTTP_STATUS_OK;
import static com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationAction.ADD;
import static com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationAction.REMOVE;
import static com.backbase.stream.LambdaAssertions.assertEqualsTo;
import static com.backbase.stream.WebClientTestUtils.buildWebResponseExceptionMono;
import static com.backbase.stream.legalentity.model.LegalEntityStatus.ENABLED;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import com.backbase.dbs.accesscontrol.api.service.v3.DataGroupsApi;
import com.backbase.dbs.accesscontrol.api.service.v3.FunctionGroupsApi;
import com.backbase.dbs.accesscontrol.api.service.v3.ServiceAgreementsApi;
import com.backbase.dbs.accesscontrol.api.service.v3.UserContextApi;
import com.backbase.dbs.accesscontrol.api.service.v3.UsersApi;
import com.backbase.dbs.accesscontrol.api.service.v3.model.BatchResponseItemExtended;
import com.backbase.dbs.accesscontrol.api.service.v3.model.DataGroupItem;
import com.backbase.dbs.accesscontrol.api.service.v3.model.DataGroupItemSystemBase;
import com.backbase.dbs.accesscontrol.api.service.v3.model.FunctionGroupItem;
import com.backbase.dbs.accesscontrol.api.service.v3.model.FunctionGroupItem.TypeEnum;
import com.backbase.dbs.accesscontrol.api.service.v3.model.GetContexts;
import com.backbase.dbs.accesscontrol.api.service.v3.model.IdItem;
import com.backbase.dbs.accesscontrol.api.service.v3.model.ParticipantIngest;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PersistenceApprovalPermissions;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PersistenceApprovalPermissionsGetResponseBody;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationAction;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationAssignUserPermissions;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationDataGroupIdentifier;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationDataGroupItemPutRequestBody;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationDataGroupUpdate;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationFunctionGroupDataGroup;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationIdentifier;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationItemIdentifier;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationParticipantBatchUpdate;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationParticipantPutBody;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationServiceAgreementUserPair;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationServiceAgreementUsersBatchUpdate;
import com.backbase.dbs.accesscontrol.api.service.v3.model.ServiceAgreementItem;
import com.backbase.dbs.accesscontrol.api.service.v3.model.ServiceAgreementParticipantsGetResponseBody;
import com.backbase.dbs.accesscontrol.api.service.v3.model.ServiceAgreementUsersQuery;
import com.backbase.dbs.accesscontrol.api.service.v3.model.ServicesAgreementIngest;
import com.backbase.dbs.accesscontrol.api.service.v3.model.UserContextItem;
import com.backbase.dbs.user.api.service.v2.UserManagementApi;
import com.backbase.stream.configuration.DeletionProperties;
import com.backbase.stream.legalentity.model.BaseProductGroup;
import com.backbase.stream.legalentity.model.BaseProductGroup.ProductGroupTypeEnum;
import com.backbase.stream.legalentity.model.BatchProductGroup;
import com.backbase.stream.legalentity.model.BusinessFunctionGroup;
import com.backbase.stream.legalentity.model.CurrentAccount;
import com.backbase.stream.legalentity.model.JobProfileUser;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.LegalEntityParticipant;
import com.backbase.stream.legalentity.model.LegalEntityType;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.legalentity.model.ServiceAgreementUserAction;
import com.backbase.stream.legalentity.model.User;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.legalentity.model.CustomDataGroupItem;
import com.backbase.stream.product.task.BatchProductGroupTask;
import com.backbase.stream.product.task.BatchProductIngestionMode;
import com.backbase.stream.product.task.ProductGroupTask;
import com.backbase.stream.product.utils.StreamUtils;
import com.backbase.stream.utils.BatchResponseUtils;
import com.backbase.stream.worker.exception.StreamTaskException;
import com.backbase.stream.worker.model.StreamTask;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
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
    private UsersApi accessControlUsersApi;

    @Mock
    private DataGroupsApi dataGroupsApi;

    @Mock
    private FunctionGroupsApi functionGroupsApi;

    @Mock
    private ServiceAgreementsApi serviceAgreementsApi;

    @Spy
    private DeletionProperties configurationProperties;

    @Spy
    private BatchResponseUtils batchResponseUtils;

    @Mock
    private UserContextApi userContextApi;

    @Captor
    private ArgumentCaptor<List<PresentationDataGroupItemPutRequestBody>> presentationDataGroupItemPutRequestBodyCaptor;

    String userInternalId = "userInternalId";

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
            .participants(List.of(new LegalEntityParticipant().externalId(participantExId)));

        when(serviceAgreementsApi.postServiceAgreementIngest(any())).thenReturn(Mono.just(new IdItem().id(saInId)));

        ServiceAgreement actual = subject.createServiceAgreement(streamTask, serviceAgreement).block();

        assertEquals(saInId, actual.getInternalId());

        ServicesAgreementIngest expectedSA = new ServicesAgreementIngest()
            .status(null).isMaster(null)
            .externalId(saExId).name(saName).description(saDesc)
            .participantsToIngest(List.of(new ParticipantIngest().externalId(participantExId)));
        verify(serviceAgreementsApi).postServiceAgreementIngest(expectedSA);
    }

    @Test
    void shouldHandleErrorOnCreateServiceAgreement() {
        String saExId = "someSaExId";
        String saName = "someSaName";
        String saDesc = "someSaDesc";
        String participantExId = "someParticipantExId";
        String saInId = "someSaInId";

        StreamTask streamTask = Mockito.mock(StreamTask.class);
        ServiceAgreement serviceAgreement = new ServiceAgreement()
            .externalId(saExId).name(saName).description(saDesc)
            .participants(List.of(new LegalEntityParticipant().externalId(participantExId)));

        String errorMessage = "some error";
        WebClientResponseException.InternalServerError error =
            Mockito.mock(WebClientResponseException.InternalServerError.class);
        when(error.getResponseBodyAsString()).thenReturn(errorMessage);
        when(serviceAgreementsApi.postServiceAgreementIngest(any()))
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

        when(dataGroupsApi.getDataGroups(saInId, ProductGroupTypeEnum.CUSTOM.name(), true))
            .thenReturn(Flux.just(new DataGroupItem().id(existingDgItemInId1).name(productGroupName)));

        when(dataGroupsApi.putDataGroups(any())).thenReturn(Mono.empty());

        subject.setupProductGroups(streamTask).block();

        PresentationDataGroupUpdate expectedDGUpdate1 = new PresentationDataGroupUpdate();
        expectedDGUpdate1.setDataGroupIdentifier(
            new PresentationDataGroupIdentifier().idIdentifier(existingDgItemInId1));
        expectedDGUpdate1.setDataItems(List.of(new PresentationItemIdentifier().id(existingDgItemInId1)));
        expectedDGUpdate1.setName(productGroupName);

        verify(dataGroupsApi).putDataGroups(expectedDGUpdate1);
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

        when(serviceAgreementsApi.putPresentationServiceAgreementAdminsBatchUpdate(any()))
            .thenReturn(Flux.just(new BatchResponseItemExtended()));

        subject.setAdministrators(legalEntity).block();

        PresentationServiceAgreementUsersBatchUpdate expected = new PresentationServiceAgreementUsersBatchUpdate()
            .action(ADD).addUsersItem(new PresentationServiceAgreementUserPair()
                .externalServiceAgreementId(saExId).externalUserId(userExId));
        verify(serviceAgreementsApi).putPresentationServiceAgreementAdminsBatchUpdate(expected);
    }

    @Test
    void getServiceAgreementByExternalIdRetrievesServiceAgreementByExternalId() {
        final String externalId = "someExternalId";
        final Mono<ServiceAgreementItem> dbsSa = Mono.just(new ServiceAgreementItem().externalId(externalId));

        when(serviceAgreementsApi.getServiceAgreementExternalId(eq(externalId))).thenReturn(dbsSa);

        Mono<ServiceAgreement> result = subject.getServiceAgreementByExternalId(externalId);

        ServiceAgreement expected = new ServiceAgreement().externalId(externalId);

        StepVerifier.create(result)
                .assertNext(serviceAgreement -> assertEquals(serviceAgreement,expected))
                .verifyComplete();

    }

    @Test
    void getServiceAgreementByExternalIdReturnsEmptyOnServiceAgreementNotFound() {
        final String externalId = "someExternalId";

        Mono<ServiceAgreementItem> response =
            buildWebResponseExceptionMono(WebClientResponseException.NotFound.class, HttpMethod.GET);
        when(serviceAgreementsApi.getServiceAgreementExternalId(eq(externalId)))
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
        final String validFromDate = "2021-03-08";
        final String validFromTime = "00:00:00";
        final String validUntilDate = "2022-03-08";
        final String validUntilTime = "23:59:59";
        final List<ServiceAgreementUserAction> regularUsers = asList("userId1", "userId2").stream()
            .map(u -> new ServiceAgreementUserAction().userProfile(new JobProfileUser().user(new User()
                .externalId("ex_" + u).internalId("in_" + u)))
                .action(ServiceAgreementUserAction.ActionEnum.ADD))
            .collect(Collectors.toList());

        StreamTask streamTask = Mockito.mock(StreamTask.class);

        ServiceAgreement serviceAgreement = buildInputServiceAgreement(saInternalId, saExternalId, description, name,
            LocalDate.parse(validFromDate), validFromTime, LocalDate.parse(validUntilDate), validUntilTime);

        serviceAgreement
            .addParticipantsItem(new LegalEntityParticipant().externalId("p1").sharingAccounts(true)
                .sharingUsers(true).action(LegalEntityParticipant.ActionEnum.ADD))
            .addParticipantsItem(new LegalEntityParticipant().externalId("p2").sharingAccounts(false)
                .sharingUsers(false).action(LegalEntityParticipant.ActionEnum.ADD));

        when(serviceAgreementsApi.putPresentationIngestServiceAgreementParticipants(any()))
            .thenReturn(Flux.concat(
                Mono.just(new BatchResponseItemExtended().action(ADD).resourceId("p1").status(HTTP_STATUS_OK)),
                Mono.just(new BatchResponseItemExtended().action(ADD).resourceId("p2").status(HTTP_STATUS_OK))
            ));

        Flux<BatchResponseItemExtended> usersResponse = Flux.fromIterable(regularUsers.stream()
            .map(u -> new BatchResponseItemExtended().status(HTTP_STATUS_OK)
                .resourceId(u.getUserProfile().getUser().getExternalId()))
            .collect(Collectors.toList()));
        when(serviceAgreementsApi.putPresentationServiceAgreementUsersBatchUpdate(any())).thenReturn(usersResponse);

        when(serviceAgreementsApi.getServiceAgreementParticipants(eq(saInternalId), eq(false)))
            .thenReturn(Flux.fromIterable(Collections.emptyList()));

        Mono<ServiceAgreementUsersQuery> emptyExistingUsersList = Mono.just(new ServiceAgreementUsersQuery());
        when(serviceAgreementsApi.getServiceAgreementUsers(eq(saInternalId))).thenReturn(emptyExistingUsersList);


        Mono<ServiceAgreement> result = subject.updateServiceAgreementAssociations(streamTask, serviceAgreement, regularUsers);
        result.block();


        InOrder inOrderValidator = inOrder(serviceAgreementsApi);
        thenUpdateParticipantsCall(inOrderValidator, saExternalId, ADD,
            new ExpectedParticipantUpdate("p1", true, true),
            new ExpectedParticipantUpdate("p2", false, false));

        thenRegularUsersUpdateCall(saExternalId, ADD, "ex_userId1", "ex_userId2");
    }

    @Test
    void updateServiceAgreementWithExistingParticipants() {
        final String saInternalId = "someSaInternalId";
        final String saExternalId = "someSaExternalId";
        final String description = "someDescription";
        final String name = "someName";
        final String validFromDate = "2021-03-08";
        final String validFromTime = "00:00:00";
        final String validUntilDate = "2022-03-08";
        final String validUntilTime = "23:59:59";
        final List<ServiceAgreementUserAction> regularUsersToAdd = asList("userId1", "userId2").stream()
            .map(u -> new ServiceAgreementUserAction().userProfile(new JobProfileUser().user(new User()
                .externalId("ex_" + u).internalId("in_" + u)))
                .action(ServiceAgreementUserAction.ActionEnum.ADD))
            .collect(Collectors.toList());
        final List<ServiceAgreementUserAction> regularUsersToRemove = asList("userId3", "userId4").stream()
            .map(u -> new ServiceAgreementUserAction().userProfile(new JobProfileUser().user(new User()
                .externalId("ex_" + u).internalId("in_" + u)))
                .action(ServiceAgreementUserAction.ActionEnum.REMOVE))
            .collect(Collectors.toList());
        final List<ServiceAgreementUserAction> regularUsers =
            Stream.concat(regularUsersToAdd.stream(), regularUsersToRemove.stream()).collect(Collectors.toList());

        StreamTask streamTask = Mockito.mock(StreamTask.class);

        ServiceAgreement serviceAgreement = buildInputServiceAgreement(saInternalId, saExternalId, description, name,
            LocalDate.parse(validFromDate), validFromTime, LocalDate.parse(validUntilDate), validUntilTime);

        // participants
        serviceAgreement
            .addParticipantsItem(new LegalEntityParticipant().externalId("p1").sharingAccounts(true)
                .sharingUsers(true).action(LegalEntityParticipant.ActionEnum.ADD))
            .addParticipantsItem(new LegalEntityParticipant().externalId("p2").sharingAccounts(false)
                .sharingUsers(false).action(LegalEntityParticipant.ActionEnum.REMOVE))
            .addParticipantsItem(new LegalEntityParticipant().externalId("p3").sharingAccounts(false)
                .sharingUsers(false).action(LegalEntityParticipant.ActionEnum.ADD));

        when(serviceAgreementsApi.putPresentationIngestServiceAgreementParticipants(any()))
            .thenReturn(Flux.concat(Mono.just(new BatchResponseItemExtended().status(HTTP_STATUS_OK))));

        ServiceAgreementParticipantsGetResponseBody existingPar1 =
            new ServiceAgreementParticipantsGetResponseBody().externalId("p1");
        ServiceAgreementParticipantsGetResponseBody existingPar2 =
            new ServiceAgreementParticipantsGetResponseBody().externalId("p2");
        when(serviceAgreementsApi.getServiceAgreementParticipants(eq(saInternalId), eq(false)))
            .thenReturn(Flux.fromIterable(asList(existingPar1, existingPar2)));

        // users
        Flux<BatchResponseItemExtended> usersResponse = Flux.fromIterable(regularUsers.stream()
            .map(u -> new BatchResponseItemExtended().status(HTTP_STATUS_OK)
                .resourceId(u.getUserProfile().getUser().getExternalId()))
            .collect(Collectors.toList()));
        when(serviceAgreementsApi.putPresentationServiceAgreementUsersBatchUpdate(any())).thenReturn(usersResponse);

        Mono<ServiceAgreementUsersQuery> existingUsersList =
            Mono.just(new ServiceAgreementUsersQuery().addUserIdsItem("in_userId1").addUserIdsItem("in_userId3"));
        when(serviceAgreementsApi.getServiceAgreementUsers(eq(saInternalId))).thenReturn(existingUsersList);


        Mono<ServiceAgreement> result = subject.updateServiceAgreementAssociations(streamTask, serviceAgreement, regularUsers);
        result.block();


        InOrder inOrderValidator = inOrder(serviceAgreementsApi);
        thenUpdateParticipantsCall(inOrderValidator, saExternalId, ADD,
            new ExpectedParticipantUpdate("p3", false, false));
        thenUpdateParticipantsCall(inOrderValidator, saExternalId, REMOVE,
            new ExpectedParticipantUpdate("p2", false, false));

        thenRegularUsersUpdateCall(saExternalId, REMOVE, "ex_userId3");
        thenRegularUsersUpdateCall(saExternalId, ADD, "ex_userId2");
    }

    @Test
    void updateParticipantsLogsAllErrors() {
        final String saExternalId = "someSaExternalId";
        final String saInternalId = "someSaInternalId";

        StreamTask streamTask = Mockito.spy(StreamTask.class);

        ServiceAgreement serviceAgreement = buildInputServiceAgreement(saInternalId, saExternalId,
            "", "", null, null, null, null);

        serviceAgreement
            .addParticipantsItem(new LegalEntityParticipant().externalId("p1").sharingAccounts(true)
                .sharingUsers(true).action(LegalEntityParticipant.ActionEnum.ADD))
            .addParticipantsItem(new LegalEntityParticipant().externalId("p2").sharingAccounts(false)
                .sharingUsers(false).action(LegalEntityParticipant.ActionEnum.ADD))
            .addParticipantsItem(new LegalEntityParticipant().externalId("p3").sharingAccounts(false)
                .sharingUsers(false).action(LegalEntityParticipant.ActionEnum.ADD))
            .addParticipantsItem(new LegalEntityParticipant().externalId("p4").sharingAccounts(false)
                .sharingUsers(false).action(LegalEntityParticipant.ActionEnum.ADD));

        when(serviceAgreementsApi.putPresentationIngestServiceAgreementParticipants(any()))
            .thenReturn(Flux.concat(
                Mono.just(new BatchResponseItemExtended().action(ADD).resourceId("p1")
                    .status(HTTP_STATUS_OK)),
                Mono.just(new BatchResponseItemExtended().action(ADD).resourceId("p2")
                    .status(HTTP_STATUS_INTERNAL_SERVER_ERROR)),
                Mono.just(new BatchResponseItemExtended().action(ADD).resourceId("p3")
                    .status(HTTP_STATUS_INTERNAL_SERVER_ERROR)),
                Mono.just(new BatchResponseItemExtended().action(ADD).resourceId("p4")
                    .status(HTTP_STATUS_OK))
            ));

        when(serviceAgreementsApi.getServiceAgreementParticipants(eq(saInternalId), eq(false)))
            .thenReturn(Flux.fromIterable(Collections.emptyList()));


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
            new BatchProductGroup().serviceAgreement(new ServiceAgreement().externalId("sa_benedict").internalId("sa-internal-id"))
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

        List<PresentationAssignUserPermissions> expectedPermissions = Collections.singletonList(
            new PresentationAssignUserPermissions()
                .externalUserId("benedict")
                .externalServiceAgreementId("sa_benedict")
                .functionGroupDataGroups(Arrays.asList(
                    new PresentationFunctionGroupDataGroup().functionGroupIdentifier(
                        new PresentationIdentifier().idIdentifier("system-group-id-2")
                    ).dataGroupIdentifiers(Collections.emptyList()),
                    new PresentationFunctionGroupDataGroup().functionGroupIdentifier(
                        new PresentationIdentifier().idIdentifier("system-group-id-3")
                    ).dataGroupIdentifiers(Collections.emptyList()),
                    new PresentationFunctionGroupDataGroup().functionGroupIdentifier(
                        new PresentationIdentifier().idIdentifier("business-function-group-id-1")
                    ).dataGroupIdentifiers(List.of(new PresentationDataGroupIdentifier().idIdentifier("data-group-id")))
                ))
        );

        when(functionGroupsApi.getFunctionGroups("sa-internal-id"))
            .thenReturn(Flux.just(
                new FunctionGroupItem().id("system-group-id-1").name("SYSTEM_FUNCTION_GROUP").type(FunctionGroupItem.TypeEnum.SYSTEM),
                new FunctionGroupItem().id("system-group-id-2").name("Full access").type(FunctionGroupItem.TypeEnum.TEMPLATE)
            ));

        when(accessControlUsersApi.getPersistenceApprovalPermissions("user-internal-id", "sa-internal-id"))
            .thenReturn(Mono.just(new PersistenceApprovalPermissions().items(Arrays.asList(
                new PersistenceApprovalPermissionsGetResponseBody().functionGroupId("system-group-id-1").dataGroupIds(Collections.emptyList()),
                new PersistenceApprovalPermissionsGetResponseBody().functionGroupId("system-group-id-2").dataGroupIds(Collections.emptyList()),
                new PersistenceApprovalPermissionsGetResponseBody().functionGroupId("system-group-id-3").dataGroupIds(Collections.emptyList()),
                new PersistenceApprovalPermissionsGetResponseBody().functionGroupId("business-function-group-id-1").dataGroupIds(List.of("data-group-id"))
            ))));

        when(accessControlUsersApi.putAssignUserPermissions(expectedPermissions))
            .thenReturn(Flux.just(
                new BatchResponseItemExtended().resourceId("resource-id").status(HTTP_STATUS_OK).errors(Collections.emptyList())
            ));

        // When
        BatchProductGroupTask result = subject.assignPermissionsBatch(batchProductGroupTask, usersPermissions)
            .block();

        // Then
        Assertions.assertSame(batchProductGroupTask, result);

        verify(accessControlUsersApi).putAssignUserPermissions(expectedPermissions);
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
            new BatchProductGroup().serviceAgreement(new ServiceAgreement().externalId("sa_benedict").internalId("sa-internal-id"))
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

        List<PresentationAssignUserPermissions> expectedPermissions = Collections.singletonList(
            new PresentationAssignUserPermissions()
                .externalUserId("benedict")
                .externalServiceAgreementId("sa_benedict")
                .functionGroupDataGroups(Arrays.asList(
                    new PresentationFunctionGroupDataGroup().functionGroupIdentifier(
                        new PresentationIdentifier().idIdentifier("function-group-id-1")
                    ).dataGroupIdentifiers(Collections.emptyList()),
                    new PresentationFunctionGroupDataGroup().functionGroupIdentifier(
                        new PresentationIdentifier().idIdentifier("business-function-group-id-1")
                    ).dataGroupIdentifiers(Arrays.asList(
                        new PresentationDataGroupIdentifier().idIdentifier("data-group-1"),
                        new PresentationDataGroupIdentifier().idIdentifier("data-group-0"))),
                    new PresentationFunctionGroupDataGroup().functionGroupIdentifier(
                        new PresentationIdentifier().idIdentifier("business-function-group-id-2")
                    ).dataGroupIdentifiers(Arrays.asList(
                        new PresentationDataGroupIdentifier().idIdentifier("data-group-2")
                    )
                ))
        ));

        when(functionGroupsApi.getFunctionGroups("sa-internal-id"))
            .thenReturn(Flux.just(
                new FunctionGroupItem().id("system-group-id-1").name("SFG").type(FunctionGroupItem.TypeEnum.SYSTEM),
                new FunctionGroupItem().id("function-group-id-1").name("Full access").type(FunctionGroupItem.TypeEnum.TEMPLATE)
            ));

        when(accessControlUsersApi.getPersistenceApprovalPermissions("user-internal-id", "sa-internal-id"))
            .thenReturn(Mono.just(new PersistenceApprovalPermissions().items(Arrays.asList(
                new PersistenceApprovalPermissionsGetResponseBody().functionGroupId("function-group-id-1").dataGroupIds(Collections.emptyList()),
                new PersistenceApprovalPermissionsGetResponseBody().functionGroupId("business-function-group-id-1").dataGroupIds(Collections.singletonList("data-group-1"))
            ))));

        when(accessControlUsersApi.putAssignUserPermissions(expectedPermissions))
            .thenReturn(Flux.just(
                new BatchResponseItemExtended().resourceId("resource-id").status(HTTP_STATUS_OK).errors(Collections.emptyList())
            ));

        // When
        BatchProductGroupTask result = subject.assignPermissionsBatch(batchProductGroupTask, usersPermissions)
            .block();

        // Then
        Assertions.assertSame(batchProductGroupTask, result);

        verify(accessControlUsersApi).putAssignUserPermissions(expectedPermissions);
    }

    @Test
    void assignPermissionsBatchIngestionModeReplace() {
        // Given
        BatchProductGroupTask batchProductGroupTask = new BatchProductGroupTask().data(
            new BatchProductGroup().serviceAgreement(new ServiceAgreement().externalId("sa_benedict").internalId("sa-internal-id"))
        );
        batchProductGroupTask.setIngestionMode(BatchProductIngestionMode.REPLACE);

        Map<BusinessFunctionGroup, List<BaseProductGroup>> baseProductGroupMap = new HashMap<>();
        baseProductGroupMap.put(new BusinessFunctionGroup().id("business-function-group-id-1"), Collections.emptyList());

        Map<User, Map<BusinessFunctionGroup, List<BaseProductGroup>>> usersPermissions = new HashMap<>();
        usersPermissions.put(
            new User().internalId("user-internal-id").externalId("benedict"),
            baseProductGroupMap
        );

        List<PresentationAssignUserPermissions> expectedPermissions = Collections.singletonList(
            new PresentationAssignUserPermissions()
                .externalUserId("benedict")
                .externalServiceAgreementId("sa_benedict")
                .functionGroupDataGroups(Collections.singletonList(
                    new PresentationFunctionGroupDataGroup().functionGroupIdentifier(
                        new PresentationIdentifier().idIdentifier("business-function-group-id-1")
                    ).dataGroupIdentifiers(Collections.emptyList())
                ))
        );

        when(accessControlUsersApi.putAssignUserPermissions(expectedPermissions))
            .thenReturn(Flux.just(
                new BatchResponseItemExtended().resourceId("resource-id").status(HTTP_STATUS_OK).errors(Collections.emptyList())
            ));

        // When
        BatchProductGroupTask result = subject.assignPermissionsBatch(batchProductGroupTask, usersPermissions)
            .block();

        // Then
        Assertions.assertSame(batchProductGroupTask, result);

        verify(accessControlUsersApi).putAssignUserPermissions(expectedPermissions);
    }

    @Test
    void updateExistingDataGroupsBatchWithSameInDbsIngestionModeReplace() {
        // Given
        BatchProductGroupTask batchProductGroupTask = new BatchProductGroupTask();
        batchProductGroupTask.setIngestionMode(BatchProductIngestionMode.REPLACE);
        batchProductGroupTask.setBatchProductGroup(new BatchProductGroup().productGroups(
            List.of(new BaseProductGroup().name("Test product group"))));

        DataGroupItem dataGroupItemTemplateCustom = buildDataGroupItem("Repository Group Template Custom",
            "Repository Group Template Custom", "template-custom");
        DataGroupItem dataGroupItemEngagementTemplateCustom = buildDataGroupItem(
            "Repository Group Engagement Template Custom",
            "Repository Group Engagement Template Custom", "engagement-template-custom");
        DataGroupItem dataGroupItemEngagementTemplateNotification = buildDataGroupItem(
            "Repository Group Engagement Template Notification",
            "Repository Group Engagement Template Notification", "engagement-template-notification");

        BaseProductGroup baseProductGroupTemplateCustom = buildBaseProductGroup("Repository Group Template Custom",
            "Repository Group Template Custom", BaseProductGroup.ProductGroupTypeEnum.REPOSITORIES,
            "template-custom");
        BaseProductGroup baseProductGroupEngagementTemplateCustom = buildBaseProductGroup("Repository Group Engagement Template Custom",
            "Repository Group Engagement Template Custom", BaseProductGroup.ProductGroupTypeEnum.REPOSITORIES,
            "engagement-template-custom");
        BaseProductGroup baseProductGroupEngagementTemplateNotification = buildBaseProductGroup("Repository Group Engagement Template Notification",
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
        verify(dataGroupsApi, times(0)).putDataGroupItemsUpdate(any());
    }

    @Test
    void updateExistingDataGroupsBatchWithMissingInDbsIngestionModeReplace() {
        // Given
        BatchProductGroupTask batchProductGroupTask = new BatchProductGroupTask();
        batchProductGroupTask.setIngestionMode(BatchProductIngestionMode.REPLACE);
        batchProductGroupTask.setBatchProductGroup(new BatchProductGroup().productGroups(
            List.of(new BaseProductGroup().name("Test product group"))));

        DataGroupItem dataGroupItemTemplateCustom = buildDataGroupItem("Repository Group Template Custom",
            "Repository Group Template Custom");
        DataGroupItem dataGroupItemEngagementTemplateCustom = buildDataGroupItem("Repository Group Engagement Template Custom",
            "Repository Group Engagement Template Custom");
        DataGroupItem dataGroupItemEngagementTemplateNotification = buildDataGroupItem("Repository Group Engagement Template Notification",
            "Repository Group Engagement Template Notification");

        BaseProductGroup baseProductGroupTemplateCustom = buildBaseProductGroup("Repository Group Template Custom",
            "Repository Group Template Custom", BaseProductGroup.ProductGroupTypeEnum.REPOSITORIES,
            "template-custom");
        BaseProductGroup baseProductGroupEngagementTemplateCustom = buildBaseProductGroup("Repository Group Engagement Template Custom",
            "Repository Group Engagement Template Custom", BaseProductGroup.ProductGroupTypeEnum.REPOSITORIES,
            "engagement-template-custom");
        BaseProductGroup baseProductGroupEngagementTemplateNotification = buildBaseProductGroup("Repository Group Engagement Template Notification",
            "Repository Group Engagement Template Notification", BaseProductGroup.ProductGroupTypeEnum.REPOSITORIES,
            "engagement-template-notification");
        when(dataGroupsApi.putDataGroupItemsUpdate(any())).thenReturn(Flux.just(new BatchResponseItemExtended()
            .status(HTTP_STATUS_OK)
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
        verify(dataGroupsApi).putDataGroupItemsUpdate(presentationDataGroupItemPutRequestBodyCaptor.capture());
        assertEquals(3, presentationDataGroupItemPutRequestBodyCaptor.getValue().stream()
            .map(PresentationDataGroupItemPutRequestBody::getAction)
            .filter(ADD::equals)
            .toList()
            .size());
    }

    @Test
    void updateExistingDataGroupsBatchWithIncorrectInDbsIngestionModeReplace() {
        // Given
        BatchProductGroupTask batchProductGroupTask = new BatchProductGroupTask();
        batchProductGroupTask.setIngestionMode(BatchProductIngestionMode.REPLACE);
        batchProductGroupTask.setBatchProductGroup(new BatchProductGroup().productGroups(
            List.of(new BaseProductGroup().name("Test product group"))));

        DataGroupItem dataGroupItemTemplateCustom = buildDataGroupItem("Repository Group Template Custom",
            "Repository Group Template Custom", "template-custom-test");
        DataGroupItem dataGroupItemEngagementTemplateCustom = buildDataGroupItem("Repository Group Engagement Template Custom",
            "Repository Group Engagement Template Custom", "engagement-template-custom-test");
        DataGroupItem dataGroupItemEngagementTemplateNotification = buildDataGroupItem("Repository Group Engagement Template Notification",
            "Repository Group Engagement Template Notification", "engagement-template-notification-test");

        BaseProductGroup baseProductGroupTemplateCustom = buildBaseProductGroup("Repository Group Template Custom",
            "Repository Group Template Custom", BaseProductGroup.ProductGroupTypeEnum.REPOSITORIES,
            "template-custom");
        BaseProductGroup baseProductGroupEngagementTemplateCustom = buildBaseProductGroup("Repository Group Engagement Template Custom",
            "Repository Group Engagement Template Custom", BaseProductGroup.ProductGroupTypeEnum.REPOSITORIES,
            "engagement-template-custom");
        BaseProductGroup baseProductGroupEngagementTemplateNotification = buildBaseProductGroup("Repository Group Engagement Template Notification",
            "Repository Group Engagement Template Notification", BaseProductGroup.ProductGroupTypeEnum.REPOSITORIES,
            "engagement-template-notification");
        when(dataGroupsApi.putDataGroupItemsUpdate(any())).thenReturn(Flux.just(new BatchResponseItemExtended()
            .status(HTTP_STATUS_OK)
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
        verify(dataGroupsApi).putDataGroupItemsUpdate(presentationDataGroupItemPutRequestBodyCaptor.capture());
        List<PresentationDataGroupItemPutRequestBody> actions = presentationDataGroupItemPutRequestBodyCaptor.getValue();
        assertEquals(6, actions.size());
        assertTrue(actionForItemIsPresent(actions, REMOVE, "template-custom-test"));
        assertTrue(actionForItemIsPresent(actions, REMOVE, "engagement-template-custom-test"));
        assertTrue(actionForItemIsPresent(actions, REMOVE, "engagement-template-notification-test"));
        assertTrue(actionForItemIsPresent(actions, ADD, "template-custom"));
        assertTrue(actionForItemIsPresent(actions, ADD, "engagement-template-custom"));
        assertTrue(actionForItemIsPresent(actions, ADD, "engagement-template-notification"));
    }

    @Test
    void updateExistingDataGroupsDoesNotRemoveCustomDataGroups() {
        // Given
        BatchProductGroupTask batchProductGroupTask = new BatchProductGroupTask();
        batchProductGroupTask.setIngestionMode(BatchProductIngestionMode.REPLACE);
        batchProductGroupTask.setBatchProductGroup(new BatchProductGroup().productGroups(
            List.of(new BaseProductGroup().name("Test product group"))));

        DataGroupItem existingDGroupItemCustom = buildDataGroupItem("Custom data group item",
            "custom desc", "custom-dg-item1", "custom-dg-item2");
        DataGroupItem existingDGroupItemRepository = buildDataGroupItem("Repository data group item",
            "rep desc", "repository-dg-item1");

        BaseProductGroup upsertProductGroupCustom = buildBaseProductGroup("Custom data group item",
            "custom desc", ProductGroupTypeEnum.CUSTOM,
            "custom-dg-item1", "custom-dg-item2");
        BaseProductGroup upsertProductGroupRepository = buildBaseProductGroup("Repository data group item",
            "rep desc", BaseProductGroup.ProductGroupTypeEnum.REPOSITORIES,
            "repository-dg-item2");
        when(dataGroupsApi.putDataGroupItemsUpdate(any())).thenReturn(Flux.just(new BatchResponseItemExtended()
            .status(HTTP_STATUS_OK)
            .resourceId("test-resource-id")));

        // When
        subject.updateExistingDataGroupsBatch(batchProductGroupTask,
                List.of(existingDGroupItemCustom, existingDGroupItemRepository),
                List.of(upsertProductGroupCustom, upsertProductGroupRepository))
            .block();

        // Then
        verify(dataGroupsApi).putDataGroupItemsUpdate(presentationDataGroupItemPutRequestBodyCaptor.capture());

        List<PresentationDataGroupItemPutRequestBody> actions = presentationDataGroupItemPutRequestBodyCaptor.getValue();
        assertEquals(2, actions.size());
        assertTrue(actionForItemIsPresent(actions, ADD, "repository-dg-item2"));
        assertTrue(actionForItemIsPresent(actions, REMOVE, "repository-dg-item1"));
        // the following assertions is to test if for some reason "custom-dg-item1" and "custom-dg-item2" ended up paired with "repository-dg-item*" ;)
        assertFalse(actionForItemIsPresent(actions, REMOVE, "custom-dg-item1"));
        assertFalse(actionForItemIsPresent(actions, REMOVE, "custom-dg-item2"));
        assertFalse(actionForItemIsPresent(actions, ADD, "custom-dg-item1"));
        assertFalse(actionForItemIsPresent(actions, ADD, "custom-dg-item2"));
    }

    @Test
    void updateArrangementDataGroupsWhenArrangementAlreadyExists() {
        BatchProductGroupTask batchProductGroupTask = new BatchProductGroupTask();
        batchProductGroupTask.setIngestionMode(BatchProductIngestionMode.REPLACE);
        batchProductGroupTask.setBatchProductGroup(new BatchProductGroup().productGroups(
            List.of(new BaseProductGroup().name("Test product group"))));

        DataGroupItem existingDGroup = new DataGroupItem().id("dgId1").name("arrangement1")
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

        verify(dataGroupsApi, times(0)).putDataGroupItemsUpdate(any());

    }

    @Test
    void updateArrangementDataGroupsWhenArrangementItemDoesNotExist() {
        BatchProductGroupTask batchProductGroupTask = new BatchProductGroupTask();
        batchProductGroupTask.setIngestionMode(BatchProductIngestionMode.REPLACE);
        batchProductGroupTask.setBatchProductGroup(new BatchProductGroup().productGroups(
            List.of(new BaseProductGroup().name("Test product group"))));

        DataGroupItem existingDGroup = new DataGroupItem().id("debitAccountInId1").name("arrangement1")
            .addItemsItem("debitAccountExId1").serviceAgreementId("saInId");

        when(dataGroupsApi.putDataGroupItemsUpdate(any()))
            .thenReturn(
                Flux.just(new BatchResponseItemExtended().status(HTTP_STATUS_OK).resourceId("test-resource-id")));

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

        verify(dataGroupsApi).putDataGroupItemsUpdate(presentationDataGroupItemPutRequestBodyCaptor.capture());

        List<PresentationDataGroupItemPutRequestBody> actions = presentationDataGroupItemPutRequestBodyCaptor.getValue();
        assertEquals(2, actions.size());
        assertTrue(actionForItemIsPresent(actions, ADD, "debitAccountInId2"));
        assertTrue(actionForItemIsPresent(actions, REMOVE, "debitAccountExId1"));
    }

    @Test
    void updateExistingDataGroupsHandleError() {

        BatchProductGroupTask batchProductGroupTask = new BatchProductGroupTask();
        batchProductGroupTask.setIngestionMode(BatchProductIngestionMode.REPLACE);
        batchProductGroupTask.setBatchProductGroup(new BatchProductGroup()
            .serviceAgreement(new ServiceAgreement())
            .productGroups(List.of(new BaseProductGroup().name("Test product group"))));

        DataGroupItem existingDGroupItemCustom = buildDataGroupItem("Custom data group item",
            "custom desc", "custom-dg-item1");

        BaseProductGroup upsertProductGroupCustom = buildBaseProductGroup("Custom data group item",
            "custom desc", ProductGroupTypeEnum.CUSTOM,
            "custom-dg-item2");
        when(dataGroupsApi.putDataGroupItemsUpdate(any())).thenReturn(
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
        BaseProductGroup baseProductGroupEngagementTemplateCustom = buildBaseProductGroup("Repository Group Engagement Template Custom",
            "Repository Group Engagement Template Custom", BaseProductGroup.ProductGroupTypeEnum.REPOSITORIES,
            "engagement-template-custom");
        BaseProductGroup baseProductGroupEngagementTemplateNotification = buildBaseProductGroup("Repository Group Engagement Template Notification",
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
        verify(dataGroupsApi, times(0)).putDataGroupItemsUpdate(any());
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
            new BatchProductGroup().serviceAgreement(new ServiceAgreement().externalId("sa_benedict").internalId("sa-internal-id"))
        );
        batchProductGroupTask.setIngestionMode(BatchProductIngestionMode.UPSERT);

        Map<BusinessFunctionGroup, List<BaseProductGroup>> baseProductGroupMap = new HashMap<>();
        baseProductGroupMap.put(new BusinessFunctionGroup().id("business-function-group-id-1"), Collections.emptyList());

        Map<User, Map<BusinessFunctionGroup, List<BaseProductGroup>>> usersPermissions = new HashMap<>();
        usersPermissions.put(
            new User().internalId("user-internal-id").externalId("benedict"),
            baseProductGroupMap
        );

        List<PresentationAssignUserPermissions> expectedPermissions = Collections.singletonList(
            new PresentationAssignUserPermissions()
                .externalUserId("benedict")
                .externalServiceAgreementId("sa_benedict")
                .functionGroupDataGroups(Collections.singletonList(
                    new PresentationFunctionGroupDataGroup().functionGroupIdentifier(
                        new PresentationIdentifier().idIdentifier("business-function-group-id-1")
                    ).dataGroupIdentifiers(Collections.emptyList())
                ))
        );

        when(functionGroupsApi.getFunctionGroups("sa-internal-id"))
            .thenReturn(Flux.just(
                new FunctionGroupItem().id("system-group-id-1").name("SYSTEM_FUNCTION_GROUP").type(FunctionGroupItem.TypeEnum.SYSTEM),
                new FunctionGroupItem().id("system-group-id-2").name("Full access").type(FunctionGroupItem.TypeEnum.TEMPLATE)
            ));

        when(accessControlUsersApi.getPersistenceApprovalPermissions("user-internal-id", "sa-internal-id"))
            .thenReturn(Mono.just(new PersistenceApprovalPermissions().items(Collections.emptyList())));

        when(accessControlUsersApi.putAssignUserPermissions(expectedPermissions))
            .thenReturn(Flux.just(
                new BatchResponseItemExtended().resourceId("resource-id").status(HTTP_STATUS_OK).errors(Collections.emptyList())
            ));

        // When
        BatchProductGroupTask result = subject.assignPermissionsBatch(batchProductGroupTask, usersPermissions)
            .block();

        // Then
        Assertions.assertSame(batchProductGroupTask, result);

        verify(accessControlUsersApi).putAssignUserPermissions(expectedPermissions);
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
            new BatchProductGroup().serviceAgreement(new ServiceAgreement().externalId("sa_benedict").internalId("sa-internal-id"))
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

        List<PresentationAssignUserPermissions> expectedPermissions = Collections.singletonList(
            new PresentationAssignUserPermissions()
                .externalUserId("benedict")
                .externalServiceAgreementId("sa_benedict")
                .functionGroupDataGroups(Collections.singletonList(
                    new PresentationFunctionGroupDataGroup().functionGroupIdentifier(
                        new PresentationIdentifier().idIdentifier("business-function-group-id-1")
                    ).dataGroupIdentifiers(Collections.singletonList(new PresentationDataGroupIdentifier().idIdentifier("data-group-0")))
                ))
        );

        when(functionGroupsApi.getFunctionGroups("sa-internal-id"))
            .thenReturn(Flux.just(
                new FunctionGroupItem().id("system-group-id-1").name("SYSTEM_FUNCTION_GROUP").type(FunctionGroupItem.TypeEnum.SYSTEM)
            ));

        when(accessControlUsersApi.getPersistenceApprovalPermissions("user-internal-id", "sa-internal-id"))
            .thenReturn(Mono.just(new PersistenceApprovalPermissions().items(Collections.singletonList(
                new PersistenceApprovalPermissionsGetResponseBody()
                    .functionGroupId("system-group-id-1")
                    .dataGroupIds(Arrays.asList("system-data-group-1", "system-data-group-2"))
            ))));

        when(accessControlUsersApi.putAssignUserPermissions(expectedPermissions))
            .thenReturn(Flux.just(
                new BatchResponseItemExtended().resourceId("resource-id").status(HTTP_STATUS_OK).errors(Collections.emptyList())
            ));

        // When
        BatchProductGroupTask result = subject.assignPermissionsBatch(batchProductGroupTask, usersPermissions)
            .block();

        // Then
        Assertions.assertSame(batchProductGroupTask, result);

        verify(accessControlUsersApi).putAssignUserPermissions(expectedPermissions);
    }

    @Test
    void deleteFunctionGroupsForServiceAgreement_noneTypeConfigured_doesNotInvokeDeletion() {
        String internalSaId = "sa-internal-id";

        when(configurationProperties.getFunctionGroupItemType()).thenReturn(DeletionProperties.FunctionGroupItemType.NONE);

        subject.deleteFunctionGroupsForServiceAgreement(internalSaId).block();

        verify(functionGroupsApi, times(0)).postFunctionGroupsDelete(any());
    }

    @Test
    void deleteFunctionGroupsForServiceAgreement_templateTypeConfigured_deletesOnlyTemplateType() {
        String internalSaId = "sa-internal-id";

        FunctionGroupItem systemFunctionGroup = new FunctionGroupItem().id("system-group-id-1")
            .name("SYSTEM_FUNCTION_GROUP")
            .type(TypeEnum.SYSTEM);

        FunctionGroupItem templateFunctionGroup = new FunctionGroupItem().id("template-group-id-2").name("Full access")
            .type(TypeEnum.TEMPLATE);

        when(functionGroupsApi.getFunctionGroups(internalSaId))
            .thenReturn(Flux.just(
                systemFunctionGroup,
                templateFunctionGroup
            ));

        when(functionGroupsApi.postFunctionGroupsDelete(any())).thenReturn(Flux.empty());

        when(configurationProperties.getFunctionGroupItemType()).thenReturn(DeletionProperties.FunctionGroupItemType.TEMPLATE);

        subject.deleteFunctionGroupsForServiceAgreement(internalSaId).block();

        ArgumentCaptor<List<PresentationIdentifier>> captor = ArgumentCaptor.forClass(
            List.class);
        verify(functionGroupsApi).postFunctionGroupsDelete(captor.capture());

        List<PresentationIdentifier> value = captor.getValue();
        assertEquals(templateFunctionGroup.getId(), value.get(0).getIdIdentifier());
    }
    @Test
    void testUpdateServiceAgreementItem() {
        StreamTask streamTask = Mockito.mock(StreamTask.class);

        ServiceAgreement serviceAgreement = new ServiceAgreement();
        serviceAgreement.setExternalId("external-id");
        serviceAgreement.setInternalId("internal-id");
        serviceAgreement.setName("name");

        when(serviceAgreementsApi.putServiceAgreementItem(any(), any())).thenReturn(Mono.empty());

        Mono<ServiceAgreement> resultMono = subject.updateServiceAgreementItem(streamTask, serviceAgreement);

        StepVerifier.create(resultMono)
                .expectNext(serviceAgreement)
                .verifyComplete();

        verify(serviceAgreementsApi, times(1))
                .putServiceAgreementItem(eq("internal-id"), any());

    }

    @Test
    void testUpdateServiceAgreementItemFailed() {
        StreamTask streamTask = Mockito.mock(StreamTask.class);

        ServiceAgreement serviceAgreement = new ServiceAgreement();
        serviceAgreement.setExternalId("external-id");
        serviceAgreement.setInternalId("internal-id");

        when(serviceAgreementsApi.putServiceAgreementItem(any(), any()))
                .thenReturn(Mono.error(new WebClientResponseException(BAD_REQUEST, "Bad request", null, null, null, null)));

        Mono<ServiceAgreement> resultMono = subject.updateServiceAgreementItem(streamTask, serviceAgreement);

        StepVerifier.create(resultMono)
                .verifyError(StreamTaskException.class);

        verify(serviceAgreementsApi, times(1))
                .putServiceAgreementItem(eq("internal-id"), any());
    }

    private void thenRegularUsersUpdateCall(String expectedSaExId, PresentationAction expectedAction,
                                            String... expectedUserIds) {
        PresentationServiceAgreementUsersBatchUpdate expectedRegularUserAddUpdate =
            new PresentationServiceAgreementUsersBatchUpdate().action(expectedAction)
                .users(Stream.of(expectedUserIds).map(userId -> new PresentationServiceAgreementUserPair()
                    .externalUserId(userId).externalServiceAgreementId(expectedSaExId)).collect(Collectors.toList()));
        verify(serviceAgreementsApi, times(1))
            .putPresentationServiceAgreementUsersBatchUpdate(eq(expectedRegularUserAddUpdate));
    }

    private void thenUpdateParticipantsCall(InOrder validator, String expectedSaExId, PresentationAction expectedAction,
                                            ExpectedParticipantUpdate... expectedParticipants) {
        PresentationParticipantBatchUpdate expectedRequest = new PresentationParticipantBatchUpdate()
            .participants(Stream.of(expectedParticipants).map(ep -> new PresentationParticipantPutBody()
                .externalServiceAgreementId(expectedSaExId).externalParticipantId(ep.exId)
                .sharingAccounts(ep.sharingAccounts).sharingUsers(ep.sharingAccounts).action(expectedAction))
                .collect(Collectors.toList()));
        validator.verify(serviceAgreementsApi).putPresentationIngestServiceAgreementParticipants(eq(expectedRequest));
    }

    private ServiceAgreement buildInputServiceAgreement(String saInternalId, String saExternalId, String description,
                                                        String name, LocalDate validFromDate, String validFromTime,
                                                        LocalDate validUntilDate, String validUntilTime) {
        return new ServiceAgreement()
            .internalId(saInternalId)
            .externalId(saExternalId)
            .description(description)
            .status(ENABLED)
            .name(name)
            .validFromDate(validFromDate)
            .validFromTime(validFromTime)
            .validUntilDate(validUntilDate)
            .validUntilTime(validUntilTime);
    }

    private DataGroupItem buildDataGroupItem(String name, String description, String... items) {
        return new DataGroupItem()
            .name(name)
            .description(description)
            .items(List.of(items));
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

    private boolean actionForItemIsPresent(List<PresentationDataGroupItemPutRequestBody> actions,
        PresentationAction expectedAction, String expectedItem) {
        return actions.stream().anyMatch(a -> a.getAction().equals(expectedAction)
            && a.getDataItems().stream().map(PresentationItemIdentifier::getId).anyMatch(expectedItem::equals));
    }

    @AllArgsConstructor
    private static class ExpectedParticipantUpdate {
        String exId;
        boolean sharingAccounts;
        boolean sharingUsers;
    }

    @Test
    void testGetUserContextsByUserId_success() {
        var getContexts = new GetContexts().elements(
            Collections.singletonList(new UserContextItem().serviceAgreementId("sa_id"))).totalElements(1L);

        when(userContextApi.getUserContexts(anyString(), any(), any(), any()))
            .thenReturn(Mono.just(getContexts));

        StepVerifier.create(subject.getUserContextsByUserId(userInternalId, null, 0, 10))
            .expectNextMatches(serviceAgreements -> serviceAgreements.size() == 1)
            .verifyComplete();

        verify(userContextApi, times(1)).getUserContexts(userInternalId, null, 0, 10);
    }

    @Test
    void testGetUserContextsByUserId_emptyResult() {
        when(userContextApi.getUserContexts(anyString(), any(), any(), any()))
            .thenReturn(Mono.just(getEmptyContext()));

        StepVerifier.create(subject.getUserContextsByUserId(userInternalId, null, 0, 10))
            .expectNext(Collections.emptyList())
            .verifyComplete();
    }

    private GetContexts getEmptyContext() {
        return new GetContexts().elements(Collections.emptyList()).totalElements(0L);
    }

    @Test
    void testGetUserContextsByUserId_webClientResponseException4xx() {
        when(userContextApi.getUserContexts(anyString(), any(), any(), any()))
            .thenReturn(Mono.error(new WebClientResponseException("Not Found", 404, "Not Found", null, null, null)));

        StepVerifier.create(subject.getUserContextsByUserId(userInternalId, null, 0, 10))
            .expectErrorMatches(throwable -> throwable instanceof WebClientResponseException
                && HttpStatus.NOT_FOUND.equals(((WebClientResponseException) throwable).getStatusCode()))
            .verify();
    }

    @Test
    void testGetUserContextsByUserId_nullFrom() {
        when(userContextApi.getUserContexts(anyString(), any(), any(), any()))
            .thenReturn(Mono.just(getEmptyContext()));

        StepVerifier.create(subject.getUserContextsByUserId(userInternalId, null, null, 10))
            .expectNextMatches(List::isEmpty)
            .verifyComplete();
    }

    @Test
    void testGetUserContextsByUserId_pagination() {
        // Mock responses for multiple pages
        var page1 = new GetContexts().elements(List.of(new UserContextItem().serviceAgreementId("sa_1")))
            .totalElements(2L);
        var page2 = new GetContexts().elements(List.of(new UserContextItem().serviceAgreementId("sa_2")))
            .totalElements(2L);

        when(userContextApi.getUserContexts(eq(userInternalId), any(), eq(0), any()))
            .thenReturn(Mono.just(page1));
        when(userContextApi.getUserContexts(eq(userInternalId), any(), eq(1), any()))
            .thenReturn(Mono.just(page2));

        StepVerifier.create(subject.getUserContextsByUserId(userInternalId, null, 0, 1))
            .expectNextMatches(serviceAgreements -> serviceAgreements.size() == 2
                && serviceAgreements.stream().anyMatch(sa -> sa.getInternalId().equals("sa_1"))
                && serviceAgreements.stream().anyMatch(sa -> sa.getInternalId().equals("sa_2")))
            .verifyComplete();

        verify(userContextApi, times(1)).getUserContexts(eq(userInternalId), any(), eq(0), any());
        verify(userContextApi, times(1)).getUserContexts(eq(userInternalId), any(), eq(1), any());
    }

    @Test
    void testGetUserContextsByUserId_paginationLargeNumber() {
        var page1 = new GetContexts().elements(
            Collections.nCopies(25, new UserContextItem().serviceAgreementId("sa_1"))).totalElements(120L);
        var page2 = new GetContexts().elements(
            Collections.nCopies(25, new UserContextItem().serviceAgreementId("sa_2"))).totalElements(120L);
        var page3 = new GetContexts().elements(
            Collections.nCopies(25, new UserContextItem().serviceAgreementId("sa_3"))).totalElements(120L);
        var page4 = new GetContexts().elements(
            Collections.nCopies(25, new UserContextItem().serviceAgreementId("sa_4"))).totalElements(120L);
        var page5 = new GetContexts().elements(
            Collections.nCopies(20, new UserContextItem().serviceAgreementId("sa_5"))).totalElements(120L);

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

        StepVerifier.create(subject.getUserContextsByUserId(userInternalId, null, 0, 25))
            .expectNextMatches(serviceAgreements -> serviceAgreements.size() == 120)
            .verifyComplete();

        verify(userContextApi, times(1)).getUserContexts(eq(userInternalId), any(), eq(0), any());
        verify(userContextApi, times(1)).getUserContexts(eq(userInternalId), any(), eq(1), any());
        verify(userContextApi, times(1)).getUserContexts(eq(userInternalId), any(), eq(2), any());
        verify(userContextApi, times(1)).getUserContexts(eq(userInternalId), any(), eq(3), any());
        verify(userContextApi, times(1)).getUserContexts(eq(userInternalId), any(), eq(4), any());
    }

    @Test
    void testGetUserContextsByUserId_paginationEdgeCases() {
        var page1 = new GetContexts().elements(
            Collections.nCopies(10, new UserContextItem().serviceAgreementId("sa_1"))).totalElements(11L);
        var page2 = new GetContexts().elements(
            Collections.nCopies(1, new UserContextItem().serviceAgreementId("sa_2"))).totalElements(11L);


        when(userContextApi.getUserContexts(eq(userInternalId), any(), eq(0), any()))
            .thenReturn(Mono.just(page1));
        when(userContextApi.getUserContexts(eq(userInternalId), any(), eq(1), any()))
            .thenReturn(Mono.just(page2));


        StepVerifier.create(subject.getUserContextsByUserId(userInternalId, null, 0, 10))
            .expectNextMatches(serviceAgreements -> serviceAgreements.size() == 11)
            .verifyComplete();

        verify(userContextApi, times(1)).getUserContexts(eq(userInternalId), any(), eq(0), any());
        verify(userContextApi, times(1)).getUserContexts(eq(userInternalId), any(), eq(1), any());
    }

}