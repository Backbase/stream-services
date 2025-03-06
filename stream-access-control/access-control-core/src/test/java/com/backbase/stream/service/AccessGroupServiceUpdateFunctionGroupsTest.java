package com.backbase.stream.service;

import static com.backbase.stream.legalentity.model.LegalEntityStatus.ENABLED;
import static org.mockito.ArgumentMatchers.any;

import com.backbase.dbs.accesscontrol.api.service.v3.DataGroupsApi;
import com.backbase.dbs.accesscontrol.api.service.v3.FunctionGroupsApi;
import com.backbase.dbs.accesscontrol.api.service.v3.ServiceAgreementsApi;
import com.backbase.dbs.accesscontrol.api.service.v3.UsersApi;
import com.backbase.dbs.accesscontrol.api.service.v3.model.BatchResponseItemExtended;
import com.backbase.dbs.accesscontrol.api.service.v3.model.FunctionGroupItem;
import com.backbase.dbs.accesscontrol.api.service.v3.model.FunctionGroupUpdate;
import com.backbase.dbs.accesscontrol.api.service.v3.model.IdItem;
import com.backbase.dbs.accesscontrol.api.service.v3.model.Permission;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationFunctionGroupPutRequestBody;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationIdentifier;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationIngestFunctionGroup;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationPermission;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationPermissionFunctionGroupUpdate;
import com.backbase.dbs.accesscontrol.api.service.v3.model.Privilege;
import com.backbase.dbs.user.api.service.v2.UserManagementApi;
import com.backbase.stream.configuration.DeletionProperties;
import com.backbase.stream.legalentity.model.BusinessFunction;
import com.backbase.stream.legalentity.model.BusinessFunctionGroup;
import com.backbase.stream.legalentity.model.BusinessFunctionGroup.TypeEnum;
import com.backbase.stream.legalentity.model.JobRole;
import com.backbase.stream.legalentity.model.LegalEntityParticipant;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.utils.BatchResponseUtils;
import com.backbase.stream.worker.exception.StreamTaskException;
import com.backbase.stream.worker.model.StreamTask;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class AccessGroupServiceUpdateFunctionGroupsTest {

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

    @Mock
    private DeletionProperties deletionProperties;

    @Spy
    private BatchResponseUtils batchResponseUtils;

    @Test
    void setupJobRoleNoType() {
        final String saInternalId = "someSaInternalId";
        final String saExternalId = "someSaExternalId";
        final String description = "someDescription";
        final String name = "someName";
        final String validFromDate = "2021-03-08";
        final String validFromTime = "00:00:00";
        final String validUntilDate = "2022-03-08";
        final String validUntilTime = "23:59:59";

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

        serviceAgreement.setIsMaster(true);

        Mockito.when(functionGroupsApi.getFunctionGroups(saInternalId))
            .thenReturn(Flux.fromIterable(Collections.singletonList(new FunctionGroupItem()
                .name("jobRoleOld").id("2")
                .addPermissionsItem(new Permission().functionId("102")
                    .addAssignedPrivilegesItem(new Privilege().privilege("view"))
                    .addAssignedPrivilegesItem(new Privilege().privilege("edit")))
            )));

        JobRole jobRole = new JobRole()
            .name("jobRoleNew")
            .addFunctionGroupsItem(new BusinessFunctionGroup()
                .name("fg1")
                .addFunctionsItem(new BusinessFunction()
                    .name("name1")
                    .functionId("101")
                    .addPrivilegesItem(new com.backbase.stream.legalentity.model.Privilege().privilege("view"))
                )
            )
            .addFunctionGroupsItem(new BusinessFunctionGroup().name("fg2")
                .addFunctionsItem(new BusinessFunction()
                    .name("name2")
                    .functionId("102")
                    .addPrivilegesItem(new com.backbase.stream.legalentity.model.Privilege().privilege("view"))
                    .addPrivilegesItem(new com.backbase.stream.legalentity.model.Privilege().privilege("edit"))
                ))
            .metadata(Map.of("key1","value1"));

        Mockito.when(functionGroupsApi.postPresentationIngestFunctionGroup(any()))
            .thenReturn(Mono.just(new IdItem().id("1")));

        Mono<JobRole> listMono = subject.setupJobRole(streamTask, serviceAgreement, jobRole);
        JobRole setupJobRole = listMono.block();

        Assertions.assertNotNull(setupJobRole);

        Mockito.verify(functionGroupsApi)
            .postPresentationIngestFunctionGroup(new PresentationIngestFunctionGroup()
                .externalServiceAgreementId(saExternalId)
                .name("jobRoleNew")
                .description("jobRoleNew")
                .type(PresentationIngestFunctionGroup.TypeEnum.REGULAR)
                .metadata(Map.of("key1","value1"))
                .addPermissionsItem(new PresentationPermission()
                    .functionId("101")
                    .addPrivilegesItem("view")
                )
                .addPermissionsItem(new PresentationPermission()
                    .functionId("102")
                    .addPrivilegesItem("view")
                    .addPrivilegesItem("edit")
                ));

    }

    @Test
    void setupJobRoleReferenceType() {
        final String saInternalId = "someSaInternalId";
        final String saExternalId = "someSaExternalId";
        final String description = "someDescription";
        final String name = "someName";
        final String validFromDate = "2021-03-08";
        final String validFromTime = "00:00:00";
        final String validUntilDate = "2022-03-08";
        final String validUntilTime = "23:59:59";

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

        serviceAgreement.setIsMaster(true);

        Mockito.when(functionGroupsApi.getFunctionGroups(saInternalId))
            .thenReturn(Flux.fromIterable(Collections.singletonList(new FunctionGroupItem()
                .name("jobRoleOld").id("2")
                .addPermissionsItem(new Permission().functionId("102")
                    .addAssignedPrivilegesItem(new Privilege().privilege("view"))
                    .addAssignedPrivilegesItem(new Privilege().privilege("edit")))
            )));

        JobRole jobRole = new JobRole()
            .name("jobRoleNew")
            .addFunctionGroupsItem(new BusinessFunctionGroup()
                .name("fg1")
                .type(TypeEnum.TEMPLATE)
                .addFunctionsItem(new BusinessFunction()
                    .name("name1")
                    .functionId("101")
                    .addPrivilegesItem(new com.backbase.stream.legalentity.model.Privilege().privilege("view"))
                )
            )
            .addFunctionGroupsItem(new BusinessFunctionGroup()
                .name("fg2")
                .type(TypeEnum.TEMPLATE)
                .addFunctionsItem(new BusinessFunction()
                    .name("name2")
                    .functionId("102")
                    .addPrivilegesItem(new com.backbase.stream.legalentity.model.Privilege().privilege("view"))
                    .addPrivilegesItem(new com.backbase.stream.legalentity.model.Privilege().privilege("edit"))
                ))
            .metadata(Map.of("key1","value1"));

        Mockito.when(functionGroupsApi.postPresentationIngestFunctionGroup(any()))
            .thenReturn(Mono.just(new IdItem().id("1")));

        Mono<JobRole> listMono = subject.setupJobRole(streamTask, serviceAgreement, jobRole);
        JobRole setupJobRole = listMono.block();

        Assertions.assertNotNull(setupJobRole);

        Mockito.verify(functionGroupsApi)
            .postPresentationIngestFunctionGroup(new PresentationIngestFunctionGroup()
                .externalServiceAgreementId(saExternalId)
                .apsId(1L)
                .name("jobRoleNew")
                .description("jobRoleNew")
                .type(PresentationIngestFunctionGroup.TypeEnum.TEMPLATE)
                .metadata(Map.of("key1","value1"))
                .addPermissionsItem(new PresentationPermission()
                    .functionId("101")
                    .addPrivilegesItem("view")
                )
                .addPermissionsItem(new PresentationPermission()
                    .functionId("102")
                    .addPrivilegesItem("view")
                    .addPrivilegesItem("edit")
                ));

    }

    @Test
    void setupJobRoleInvalidType() {
        final String saInternalId = "someSaInternalId";
        final String saExternalId = "someSaExternalId";
        final String description = "someDescription";
        final String name = "someName";
        final String validFromDate = "2021-03-08";
        final String validFromTime = "00:00:00";
        final String validUntilDate = "2022-03-08";
        final String validUntilTime = "23:59:59";

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

        serviceAgreement.setIsMaster(true);

        Mockito.when(functionGroupsApi.getFunctionGroups(saInternalId))
            .thenReturn(Flux.fromIterable(Collections.singletonList(new FunctionGroupItem()
                .name("jobRoleOld").id("2")
                .addPermissionsItem(new Permission().functionId("102")
                    .addAssignedPrivilegesItem(new Privilege().privilege("view"))
                    .addAssignedPrivilegesItem(new Privilege().privilege("edit")))
            )));

        JobRole jobRole = new JobRole()
            .name("jobRoleNew")
            .addFunctionGroupsItem(new BusinessFunctionGroup()
                .name("fg1")
                .type(TypeEnum.SYSTEM)
                .addFunctionsItem(new BusinessFunction()
                    .name("name1")
                    .functionId("101")
                    .addPrivilegesItem(new com.backbase.stream.legalentity.model.Privilege().privilege("view"))
                )
            )
            .addFunctionGroupsItem(new BusinessFunctionGroup()
                .name("fg2")
                .type(TypeEnum.SYSTEM)
                .addFunctionsItem(new BusinessFunction()
                    .name("name2")
                    .functionId("102")
                    .addPrivilegesItem(new com.backbase.stream.legalentity.model.Privilege().privilege("view"))
                    .addPrivilegesItem(new com.backbase.stream.legalentity.model.Privilege().privilege("edit"))
                ))
            .metadata(Map.of("key1","value1"));

        Mono<JobRole> listMono = subject.setupJobRole(streamTask, serviceAgreement, jobRole);

        Assertions.assertThrows(IllegalArgumentException.class, listMono::block);

    }

    @Test
    void setupJobRoleNoFunctionGroups() {
        final String saInternalId = "someSaInternalId";
        final String saExternalId = "someSaExternalId";
        final String description = "someDescription";
        final String name = "someName";
        final String validFromDate = "2021-03-08";
        final String validFromTime = "00:00:00";
        final String validUntilDate = "2022-03-08";
        final String validUntilTime = "23:59:59";

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

        serviceAgreement.setIsMaster(true);

        Mockito.when(functionGroupsApi.getFunctionGroups(saInternalId))
            .thenReturn(Flux.fromIterable(Collections.singletonList(new FunctionGroupItem()
                .name("jobRoleOld").id("2")
                .addPermissionsItem(new Permission().functionId("102")
                    .addAssignedPrivilegesItem(new Privilege().privilege("view"))
                    .addAssignedPrivilegesItem(new Privilege().privilege("edit")))
            )));

        JobRole jobRole = new JobRole()
            .name("jobRoleNew")
            .metadata(Map.of("key1","value1"));

        Mono<JobRole> listMono = subject.setupJobRole(streamTask, serviceAgreement, jobRole);

        Assertions.assertThrows(IllegalArgumentException.class, listMono::block);

    }

    @Test
    void updateJobRole() {
        final String saInternalId = "someSaInternalId";
        final String saExternalId = "someSaExternalId";
        final String description = "someDescription";
        final String name = "someName";
        final String validFromDate = "2021-03-08";
        final String validFromTime = "00:00:00";
        final String validUntilDate = "2022-03-08";
        final String validUntilTime = "23:59:59";

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

        Mockito.when(functionGroupsApi.getFunctionGroups(saInternalId))
            .thenReturn(Flux.fromIterable(Collections.singletonList(new FunctionGroupItem()
                .name("jobRole").id("1")
                .addPermissionsItem(new Permission().functionId("101")
                    .addAssignedPrivilegesItem(new Privilege().privilege("view"))
                    .addAssignedPrivilegesItem(new Privilege().privilege("edit")))
            )));

        JobRole jobRole = new JobRole()
            .name("jobRole")
            .addFunctionGroupsItem(new BusinessFunctionGroup()
                .name("fg1")
                .addFunctionsItem(new BusinessFunction()
                    .name("name1")
                    .functionId("101")
                    .addPrivilegesItem(new com.backbase.stream.legalentity.model.Privilege().privilege("view"))
                )
            )
            .addFunctionGroupsItem(new BusinessFunctionGroup().name("fg2")
                .addFunctionsItem(new BusinessFunction()
                    .name("name2")
                    .functionId("102")
                    .addPrivilegesItem(new com.backbase.stream.legalentity.model.Privilege().privilege("view"))
                    .addPrivilegesItem(new com.backbase.stream.legalentity.model.Privilege().privilege("edit"))
                ))
            .metadata(Map.of("key1","value1"));

        Mockito.when(functionGroupsApi.putFunctionGroupsUpdate(any()))
            .thenReturn(Flux.fromIterable(Collections.emptyList()));

        Mono<JobRole> listMono = subject.setupJobRole(streamTask, serviceAgreement, jobRole);
        JobRole setupJobRole = listMono.block();

        Assertions.assertNotNull(setupJobRole);

        Mockito.verify(functionGroupsApi)
            .putFunctionGroupsUpdate(Collections.singletonList(new PresentationFunctionGroupPutRequestBody()
                .identifier(new PresentationIdentifier().idIdentifier("1"))
                .functionGroup(new FunctionGroupUpdate()
                    .name("jobRole")
                    .description("jobRole")
                        .metadata(Map.of("key1","value1"))
                    .addPermissionsItem(new PresentationPermissionFunctionGroupUpdate()
                        .functionName("name1")
                        .addPrivilegesItem("view"))
                    .addPermissionsItem(new PresentationPermissionFunctionGroupUpdate()
                        .functionName("name2")
                        .addPrivilegesItem("view")
                        .addPrivilegesItem("edit")))
            ));
    }

    @Test
    void updateJobRoleItemStatusIs400() {
        final String saInternalId = "someSaInternalId";
        final String saExternalId = "someSaExternalId";
        final String description = "someDescription";
        final String name = "someName";
        final String validFromDate = "2021-03-08";
        final String validFromTime = "00:00:00";
        final String validUntilDate = "2022-03-08";
        final String validUntilTime = "23:59:59";

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

        Mockito.when(functionGroupsApi.getFunctionGroups(saInternalId))
                .thenReturn(Flux.fromIterable(Collections.singletonList(new FunctionGroupItem()
                        .name("jobRole").id("1")
                        .addPermissionsItem(new Permission().functionId("101")
                                .addAssignedPrivilegesItem(new Privilege().privilege("view"))
                                .addAssignedPrivilegesItem(new Privilege().privilege("edit")))
                )));

        JobRole jobRole = new JobRole()
                .name("jobRole")
                .addFunctionGroupsItem(new BusinessFunctionGroup()
                        .name("fg1")
                        .addFunctionsItem(new BusinessFunction()
                                .name("name1")
                                .functionId("101")
                                .addPrivilegesItem(new com.backbase.stream.legalentity.model.Privilege().privilege("view"))
                        )
                )
                .addFunctionGroupsItem(new BusinessFunctionGroup().name("fg2")
                        .addFunctionsItem(new BusinessFunction()
                                .name("name2")
                                .functionId("102")
                                .addPrivilegesItem(new com.backbase.stream.legalentity.model.Privilege().privilege("view"))
                                .addPrivilegesItem(new com.backbase.stream.legalentity.model.Privilege().privilege("edit"))
                        ));

        Mockito.when(functionGroupsApi.putFunctionGroupsUpdate(any()))
                .thenReturn(Flux.just(new BatchResponseItemExtended()
                        .resourceId("4028db307522bfbb017523171c9d0007")
                        .status(BatchResponseItemExtended.StatusEnum.HTTP_STATUS_BAD_REQUEST)
                        .addErrorsItem("You cannot manage this entity, while the referenced service agreement has a pending change.")
                ));

        Mono<JobRole> listMono = subject.setupJobRole(streamTask, serviceAgreement, jobRole);

        Assertions.assertThrows(StreamTaskException.class, listMono::block);

        Mockito.verify(functionGroupsApi)
                .putFunctionGroupsUpdate(Collections.singletonList(new PresentationFunctionGroupPutRequestBody()
                        .identifier(new PresentationIdentifier().idIdentifier("1"))
                        .functionGroup(new FunctionGroupUpdate()
                                .name("jobRole")
                                .description("jobRole")
                                .addPermissionsItem(new PresentationPermissionFunctionGroupUpdate()
                                        .functionName("name1")
                                        .addPrivilegesItem("view"))
                                .addPermissionsItem(new PresentationPermissionFunctionGroupUpdate()
                                        .functionName("name2")
                                        .addPrivilegesItem("view")
                                        .addPrivilegesItem("edit")))
                ));
    }

    @Test
    void setupFunctionGroups() {
        final String saInternalId = "someSaInternalId";
        final String saExternalId = "someSaExternalId";
        final String description = "someDescription";
        final String name = "someName";
        final String validFromDate = "2021-03-08";
        final String validFromTime = "00:00:00";
        final String validUntilDate = "2022-03-08";
        final String validUntilTime = "23:59:59";

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

        Mockito.when(functionGroupsApi.getFunctionGroups(saInternalId))
            .thenReturn(Flux.fromIterable(Collections.singletonList(new FunctionGroupItem()
                .name("fg1").id("1")
                .addPermissionsItem(new Permission().functionId("101")
                    .addAssignedPrivilegesItem(new Privilege().privilege("view"))
                    .addAssignedPrivilegesItem(new Privilege().privilege("edit")))
            )));

        List<BusinessFunctionGroup> newFunctionGroup = Arrays.asList(new BusinessFunctionGroup()
                .name("fg1")
                .id("1")
                .addFunctionsItem(new BusinessFunction()
                    .name("name1")
                    .functionId("101")
                    .addPrivilegesItem(new com.backbase.stream.legalentity.model.Privilege().privilege("view"))
                ),
            new BusinessFunctionGroup().name("fg2")
                .addFunctionsItem(new BusinessFunction()
                    .name("name2")
                    .functionId("102")
                    .addPrivilegesItem(new com.backbase.stream.legalentity.model.Privilege().privilege("view"))
                    .addPrivilegesItem(new com.backbase.stream.legalentity.model.Privilege().privilege("edit"))
                )
        );

        Mockito.when(functionGroupsApi.putFunctionGroupsUpdate(any()))
            .thenReturn(Flux.fromIterable(Collections.emptyList()));
        Mockito.when(functionGroupsApi.postPresentationIngestFunctionGroup(any()))
            .thenReturn(Mono.just(new IdItem().id("2")));

        Mono<List<BusinessFunctionGroup>> listMono =
            subject.setupFunctionGroups(streamTask, serviceAgreement, newFunctionGroup);
        List<BusinessFunctionGroup> businessFunctionGroups = listMono.block();

        Assertions.assertNotNull(businessFunctionGroups);
        Assertions.assertEquals(2, businessFunctionGroups.size());

        Mockito.verify(functionGroupsApi)
            .putFunctionGroupsUpdate(Collections.singletonList(new PresentationFunctionGroupPutRequestBody()
                .identifier(new PresentationIdentifier().idIdentifier("1"))
                .functionGroup(new FunctionGroupUpdate()
                    .name("fg1")
                    .description("fg1")
                    .addPermissionsItem(new PresentationPermissionFunctionGroupUpdate()
                        .functionName("name1")
                        .addPrivilegesItem("view")))
            ));

        Mockito.verify(functionGroupsApi)
            .postPresentationIngestFunctionGroup(new PresentationIngestFunctionGroup()
                .externalServiceAgreementId(saExternalId)
                .name("fg2")
                .description("fg2")
                .type(PresentationIngestFunctionGroup.TypeEnum.REGULAR)
                .addPermissionsItem(new PresentationPermission()
                    .functionId("102")
                    .addPrivilegesItem("view")
                    .addPrivilegesItem("edit")
                ));
    }

    private ServiceAgreement buildInputServiceAgreement(String saInternalId, String saExternalId, String description,
        String name, LocalDate validFromDate, String validFromTime, LocalDate validUntilDate, String validUntilTime) {

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

}
