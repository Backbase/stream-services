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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import com.backbase.dbs.accesscontrol.api.service.v3.DataGroupsApi;
import com.backbase.dbs.accesscontrol.api.service.v3.FunctionGroupsApi;
import com.backbase.dbs.accesscontrol.api.service.v3.ServiceAgreementsApi;
import com.backbase.dbs.accesscontrol.api.service.v3.UsersApi;
import com.backbase.dbs.accesscontrol.api.service.v3.model.BatchResponseItemExtended;
import com.backbase.dbs.accesscontrol.api.service.v3.model.DataGroupItem;
import com.backbase.dbs.accesscontrol.api.service.v3.model.FunctionGroupItem;
import com.backbase.dbs.accesscontrol.api.service.v3.model.FunctionGroupItem.TypeEnum;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PersistenceApprovalPermissions;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PersistenceApprovalPermissionsGetResponseBody;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationAction;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationAssignUserPermissions;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationDataGroupIdentifier;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationDataGroupItemPutRequestBody;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationFunctionGroupDataGroup;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationIdentifier;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationParticipantBatchUpdate;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationParticipantPutBody;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationServiceAgreementUserPair;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationServiceAgreementUsersBatchUpdate;
import com.backbase.dbs.accesscontrol.api.service.v3.model.ServiceAgreementItem;
import com.backbase.dbs.accesscontrol.api.service.v3.model.ServiceAgreementParticipantsGetResponseBody;
import com.backbase.dbs.accesscontrol.api.service.v3.model.ServiceAgreementUsersQuery;
import com.backbase.dbs.user.api.service.v2.UserManagementApi;
import com.backbase.stream.configuration.DeletionProperties;
import com.backbase.stream.legalentity.model.BaseProductGroup;
import com.backbase.stream.legalentity.model.BatchProductGroup;
import com.backbase.stream.legalentity.model.BusinessFunctionGroup;
import com.backbase.stream.legalentity.model.JobProfileUser;
import com.backbase.stream.legalentity.model.LegalEntityParticipant;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.legalentity.model.ServiceAgreementUserAction;
import com.backbase.stream.legalentity.model.User;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.legalentity.model.CustomDataGroupItem;
import com.backbase.stream.product.task.BatchProductGroupTask;
import com.backbase.stream.product.task.BatchProductIngestionMode;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.client.HttpClientErrorException;
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

    @Captor
    private ArgumentCaptor<List<PresentationDataGroupItemPutRequestBody>> presentationDataGroupItemPutRequestBodyCaptor;

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

        when(serviceAgreementsApi.getServiceAgreementParticipants(eq(saInternalId)))
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
        when(serviceAgreementsApi.getServiceAgreementParticipants(eq(saInternalId)))
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

        when(serviceAgreementsApi.getServiceAgreementParticipants(eq(saInternalId)))
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
        assertEquals(3, presentationDataGroupItemPutRequestBodyCaptor.getValue().stream()
                .map(PresentationDataGroupItemPutRequestBody::getAction)
                .filter(REMOVE::equals)
                .toList()
                .size());
        assertEquals(3, presentationDataGroupItemPutRequestBodyCaptor.getValue().stream()
                .map(PresentationDataGroupItemPutRequestBody::getAction)
                .filter(ADD::equals)
                .toList()
                .size());
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
                .thenReturn(Mono.error(new HttpClientErrorException(BAD_REQUEST, "Bad request", null, null, null)));

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
                                                   BaseProductGroup.ProductGroupTypeEnum productGroupTypeEnum,
                                                   String dataGroupItemId) {
        ProductGroup productGroup = new ProductGroup();
        productGroup.setName(name);
        productGroup.setDescription(description);
        productGroup.setProductGroupType(productGroupTypeEnum);
        productGroup.addCustomDataGroupItemsItem(
                new CustomDataGroupItem()
                        .internalId(dataGroupItemId)
                        .externalId(dataGroupItemId));
        return productGroup;
    }

    @AllArgsConstructor
    private static class ExpectedParticipantUpdate {
        String exId;
        boolean sharingAccounts;
        boolean sharingUsers;
    }

}