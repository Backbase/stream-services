package com.backbase.stream.service;

import static com.backbase.stream.legalentity.model.LegalEntityStatus.ENABLED;
import static org.mockito.ArgumentMatchers.any;

import com.backbase.accesscontrol.assignpermissions.api.service.v1.AssignPermissionsApi;
import com.backbase.accesscontrol.datagroup.api.service.v1.DataGroupApi;
import com.backbase.accesscontrol.functiongroup.api.integration.v1.model.BatchResponseItemExtended;
import com.backbase.accesscontrol.functiongroup.api.integration.v1.model.FunctionGroupBatchPutItem;
import com.backbase.accesscontrol.functiongroup.api.integration.v1.model.FunctionGroupIngest;
import com.backbase.accesscontrol.functiongroup.api.integration.v1.model.FunctionGroupNameIdentifier;
import com.backbase.accesscontrol.functiongroup.api.integration.v1.model.FunctionGroupUpdate;
import com.backbase.accesscontrol.functiongroup.api.service.v1.FunctionGroupApi;
import com.backbase.accesscontrol.functiongroup.api.service.v1.model.FunctionGroupCreateRequest;
import com.backbase.accesscontrol.functiongroup.api.service.v1.model.FunctionGroupItem;
import com.backbase.accesscontrol.functiongroup.api.service.v1.model.GetFunctionGroups;
import com.backbase.accesscontrol.functiongroup.api.service.v1.model.Permission;
import com.backbase.accesscontrol.functiongroup.api.service.v1.model.ResultId;
import com.backbase.accesscontrol.permissioncheck.api.service.v1.PermissionCheckApi;
import com.backbase.accesscontrol.serviceagreement.api.service.v1.ServiceAgreementApi;
import com.backbase.accesscontrol.usercontext.api.service.v1.UserContextApi;
import com.backbase.dbs.arrangement.api.service.v3.ArrangementsApi;
import com.backbase.dbs.user.api.service.v2.UserManagementApi;
import com.backbase.stream.configuration.AccessControlConfigurationProperties;
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
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
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
    private ArrangementsApi arrangementsApi;
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

    private static final OffsetDateTime VALID_FROM = OffsetDateTime.of(LocalDateTime.now().minusYears(1L),
        ZoneOffset.UTC);
    private static final OffsetDateTime VALID_UNTIL = OffsetDateTime.of(LocalDateTime.now().plusMonths(10L),
        ZoneOffset.UTC);

    @Test
    void shouldCreateNewJobRoleForSA() {
        final String saInternalId = "someSaInternalId";
        final String saExternalId = "someSaExternalId";
        final String description = "someDescription";
        final String name = "someName";

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

        serviceAgreement.setIsMaster(true);

        Mockito.when(functionGroupServiceApi.getFunctionGroups(saInternalId))
            .thenReturn(Mono.just(new GetFunctionGroups()
                .functionGroups(List.of(new FunctionGroupItem()
                    .name("jobRoleOld").id("2")
                    .addPermissionsItem(new Permission().businessFunctionName("name2")
                        .privileges(Set.of("view", "edit"))
                    )))));

        JobRole jobRole = new JobRole()
            .name("jobRoleNew")
            .validFrom(VALID_FROM)
            .validUntil(VALID_UNTIL)
            .apsName("apsName")
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
            .metadata(Map.of("key1", "value1"));

        Mockito.when(functionGroupIntegrationApi.ingestFunctionGroup(any()))
            .thenReturn(Mono.just(new com.backbase.accesscontrol.functiongroup.api.integration.v1.model.ResultId().id("1")));
        Mockito.when(accessControlProperties.getConcurrency())
            .thenReturn(1);

        Mono<List<JobRole>> listMono = subject.setupJobRoleForSa(streamTask, serviceAgreement, Stream.of(jobRole));
        List<JobRole> setupJobRole = listMono.block();

        Assertions.assertNotNull(setupJobRole);

        Mockito.verify(functionGroupIntegrationApi)
            .ingestFunctionGroup(new FunctionGroupIngest()
                .externalServiceAgreementId(saExternalId)
                .validFrom(VALID_FROM)
                .validUntil(VALID_UNTIL)
                .name("jobRoleNew")
                .description("jobRoleNew")
                .type(FunctionGroupIngest.TypeEnum.CUSTOM)
                .metadata(Map.of("key1", "value1"))
                .apsName("apsName")
                .addPermissionsItem(new com.backbase.accesscontrol.functiongroup.api.integration.v1.model.Permission()
                    .businessFunctionName("name1")
                    .addPrivilegesItem("view")
                )
                .addPermissionsItem(new com.backbase.accesscontrol.functiongroup.api.integration.v1.model.Permission()
                    .businessFunctionName("name2")
                    .addPrivilegesItem("view")
                    .addPrivilegesItem("edit")
                ));
    }

    @Test
    void shouldUpdateOldJobRoleForSA() {
        final String saInternalId = "someSaInternalId";
        final String saExternalId = "someSaExternalId";
        final String description = "someDescription";
        final String name = "someName";

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

        Mockito.when(functionGroupServiceApi.getFunctionGroups(saInternalId))
            .thenReturn(
                Mono.just(new GetFunctionGroups().functionGroups(Collections.singletonList(new FunctionGroupItem()
                    .name("jobRole").id("1")
                    .addPermissionsItem(new Permission().businessFunctionName("name1")
                        .addPrivilegesItem("view")
                        .addPrivilegesItem("edit")
                    )))));

        JobRole jobRole = new JobRole()
            .name("jobRole")
            .validFrom(VALID_FROM)
            .validUntil(VALID_UNTIL)
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
            .metadata(Map.of("key1", "value1"));

        Mockito.when(functionGroupIntegrationApi.batchUpdateFunctionGroups(any()))
            .thenReturn(Flux.fromIterable(Collections.emptyList()));
        Mockito.when(accessControlProperties.getConcurrency())
            .thenReturn(1);

        Mono<List<JobRole>> listMono = subject.setupJobRoleForSa(streamTask, serviceAgreement, Stream.of(jobRole));
        List<JobRole> setupJobRole = listMono.block();

        Assertions.assertNotNull(setupJobRole);

        Mockito.verify(functionGroupIntegrationApi)
            .batchUpdateFunctionGroups(Collections.singletonList(new FunctionGroupBatchPutItem()
                .identifier(new FunctionGroupNameIdentifier().name("jobRole").serviceAgreementExternalId(saExternalId))
                .newValues(new FunctionGroupUpdate()
                    .name("jobRole")
                    .validFrom(VALID_FROM)
                    .validUntil(VALID_UNTIL)
                    .description("jobRole")
                    .metadata(Map.of("key1", "value1"))
                    .addPermissionsItem(
                        new com.backbase.accesscontrol.functiongroup.api.integration.v1.model.Permission()
                            .businessFunctionName("name1")
                            .addPrivilegesItem("view"))
                    .addPermissionsItem(
                        new com.backbase.accesscontrol.functiongroup.api.integration.v1.model.Permission()
                            .businessFunctionName("name2")
                            .addPrivilegesItem("view")
                            .addPrivilegesItem("edit")))
            ));
    }

    @Test
    void setupJobRoleNoType() {
        final String saInternalId = "someSaInternalId";
        final String saExternalId = "someSaExternalId";
        final String description = "someDescription";
        final String name = "someName";

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

        serviceAgreement.setIsMaster(true);

        Mockito.when(functionGroupServiceApi.getFunctionGroups(saInternalId))
            .thenReturn(
                Mono.just(new GetFunctionGroups().functionGroups(Collections.singletonList(new FunctionGroupItem()
                    .name("jobRoleOld").id("2")
                    .addPermissionsItem(new Permission().businessFunctionName("name2")
                        .addPrivilegesItem("view")
                        .addPrivilegesItem("edit")
                    )))));

        JobRole jobRole = new JobRole()
            .name("jobRoleNew")
            .validFrom(VALID_FROM)
            .validUntil(VALID_UNTIL)
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
            .metadata(Map.of("key1", "value1"));

        Mockito.when(functionGroupIntegrationApi.ingestFunctionGroup(any()))
            .thenReturn(Mono.just(new com.backbase.accesscontrol.functiongroup.api.integration.v1.model.ResultId().id("1")));

        Mono<JobRole> listMono = subject.setupJobRole(streamTask, serviceAgreement, jobRole);
        JobRole setupJobRole = listMono.block();

        Assertions.assertNotNull(setupJobRole);

        Mockito.verify(functionGroupIntegrationApi)
            .ingestFunctionGroup(new FunctionGroupIngest()
                .externalServiceAgreementId(saExternalId)
                .name("jobRoleNew")
                .validFrom(VALID_FROM)
                .validUntil(VALID_UNTIL)
                .description("jobRoleNew")
                .type(FunctionGroupIngest.TypeEnum.CUSTOM)
                .metadata(Map.of("key1", "value1"))
                .addPermissionsItem(new com.backbase.accesscontrol.functiongroup.api.integration.v1.model.Permission()
                    .businessFunctionName("name1")
                    .addPrivilegesItem("view")
                )
                .addPermissionsItem(new com.backbase.accesscontrol.functiongroup.api.integration.v1.model.Permission()
                    .businessFunctionName("name2")
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

        serviceAgreement.setIsMaster(true);

        Mockito.when(functionGroupServiceApi.getFunctionGroups(saInternalId))
            .thenReturn(
                Mono.just(new GetFunctionGroups().functionGroups(Collections.singletonList(new FunctionGroupItem()
                    .name("jobRoleOld").id("2")
                    .addPermissionsItem(new Permission().businessFunctionName("name2")
                        .addPrivilegesItem("view")
                        .addPrivilegesItem("edit")
                    )))));

        JobRole jobRole = new JobRole()
            .name("jobRoleNew")
            .validFrom(VALID_FROM)
            .validUntil(VALID_UNTIL)
            .apsName("apsName")
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
            .metadata(Map.of("key1", "value1"));

        Mockito.when(functionGroupIntegrationApi.ingestFunctionGroup(any()))
            .thenReturn(Mono.just(new com.backbase.accesscontrol.functiongroup.api.integration.v1.model.ResultId().id("1")));

        Mono<JobRole> listMono = subject.setupJobRole(streamTask, serviceAgreement, jobRole);
        JobRole setupJobRole = listMono.block();

        Assertions.assertNotNull(setupJobRole);

        Mockito.verify(functionGroupIntegrationApi)
            .ingestFunctionGroup(new FunctionGroupIngest()
                .externalServiceAgreementId(saExternalId)
                .name("jobRoleNew")
                .validFrom(VALID_FROM)
                .validUntil(VALID_UNTIL)
                .description("jobRoleNew")
                .type(FunctionGroupIngest.TypeEnum.REFERENCE)
                .metadata(Map.of("key1", "value1"))
                .apsName("apsName")
                .addPermissionsItem(new com.backbase.accesscontrol.functiongroup.api.integration.v1.model.Permission()
                    .businessFunctionName("name1")
                    .addPrivilegesItem("view")
                )
                .addPermissionsItem(new com.backbase.accesscontrol.functiongroup.api.integration.v1.model.Permission()
                    .businessFunctionName("name2")
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

        serviceAgreement.setIsMaster(true);

        Mockito.when(functionGroupServiceApi.getFunctionGroups(saInternalId))
            .thenReturn(
                Mono.just(new GetFunctionGroups().functionGroups(Collections.singletonList(new FunctionGroupItem()
                    .name("jobRoleOld").id("2")
                    .addPermissionsItem(new Permission().businessFunctionName("name2")
                        .addPrivilegesItem("view")
                        .addPrivilegesItem("edit")
                    )))));

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
            .metadata(Map.of("key1", "value1"));

        Mono<JobRole> listMono = subject.setupJobRole(streamTask, serviceAgreement, jobRole);

        Assertions.assertThrows(IllegalArgumentException.class, listMono::block);

    }

    @Test
    void setupJobRoleNoFunctionGroups() {
        final String saInternalId = "someSaInternalId";
        final String saExternalId = "someSaExternalId";
        final String description = "someDescription";
        final String name = "someName";

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

        serviceAgreement.setIsMaster(true);

        Mockito.when(functionGroupServiceApi.getFunctionGroups(saInternalId))
            .thenReturn(
                Mono.just(new GetFunctionGroups().functionGroups(Collections.singletonList(new FunctionGroupItem()
                    .name("jobRoleOld").id("2")
                    .addPermissionsItem(new Permission().businessFunctionName("name2")
                        .addPrivilegesItem("view")
                        .addPrivilegesItem("edit")
                    )))));

        JobRole jobRole = new JobRole()
            .name("jobRoleNew")
            .metadata(Map.of("key1", "value1"));

        Mono<JobRole> listMono = subject.setupJobRole(streamTask, serviceAgreement, jobRole);

        Assertions.assertThrows(IllegalArgumentException.class, listMono::block);

    }

    @Test
    void updateJobRole() {
        final String saInternalId = "someSaInternalId";
        final String saExternalId = "someSaExternalId";
        final String description = "someDescription";
        final String name = "someName";

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

        Mockito.when(functionGroupServiceApi.getFunctionGroups(saInternalId))
            .thenReturn(
                Mono.just(new GetFunctionGroups().functionGroups(Collections.singletonList(new FunctionGroupItem()
                    .name("jobRole").id("1")
                    .addPermissionsItem(new Permission().businessFunctionName("name1")
                        .addPrivilegesItem("view")
                        .addPrivilegesItem("edit")
                    )))));

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
            .metadata(Map.of("key1", "value1"));

        Mockito.when(functionGroupIntegrationApi.batchUpdateFunctionGroups(any()))
            .thenReturn(Flux.fromIterable(Collections.emptyList()));

        Mono<JobRole> listMono = subject.setupJobRole(streamTask, serviceAgreement, jobRole);
        JobRole setupJobRole = listMono.block();

        Assertions.assertNotNull(setupJobRole);

        Mockito.verify(functionGroupIntegrationApi)
            .batchUpdateFunctionGroups(Collections.singletonList(new FunctionGroupBatchPutItem()
                .identifier(new FunctionGroupNameIdentifier().name("jobRole").serviceAgreementExternalId(saExternalId))
                .newValues(new FunctionGroupUpdate()
                    .name("jobRole")
                    .description("jobRole")
                    .metadata(Map.of("key1", "value1"))
                    .addPermissionsItem(
                        new com.backbase.accesscontrol.functiongroup.api.integration.v1.model.Permission()
                            .businessFunctionName("name1")
                            .addPrivilegesItem("view"))
                    .addPermissionsItem(
                        new com.backbase.accesscontrol.functiongroup.api.integration.v1.model.Permission()
                            .businessFunctionName("name2")
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

        Mockito.when(functionGroupServiceApi.getFunctionGroups(saInternalId))
            .thenReturn(
                Mono.just(new GetFunctionGroups().functionGroups(Collections.singletonList(new FunctionGroupItem()
                    .name("jobRole").id("1")
                    .addPermissionsItem(new Permission().businessFunctionName("name1")
                        .addPrivilegesItem("view")
                        .addPrivilegesItem("edit")
                    )))));

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
            .putMetadataItem("key1", "value1");

        Mockito.when(functionGroupIntegrationApi.batchUpdateFunctionGroups(any()))
            .thenReturn(Flux.just(new BatchResponseItemExtended()
                .resourceId("4028db307522bfbb017523171c9d0007")
                .status(BatchResponseItemExtended.StatusEnum.HTTP_STATUS_BAD_REQUEST)
                .addErrorsItem(
                    "You cannot manage this entity, while the referenced service agreement has a pending change.")
            ));

        Mono<JobRole> listMono = subject.setupJobRole(streamTask, serviceAgreement, jobRole);

        Assertions.assertThrows(StreamTaskException.class, listMono::block);

        Mockito.verify(functionGroupIntegrationApi)
            .batchUpdateFunctionGroups(Collections.singletonList(new FunctionGroupBatchPutItem()
                .identifier(new FunctionGroupNameIdentifier().name("jobRole").serviceAgreementExternalId(saExternalId))
                .newValues(new FunctionGroupUpdate()
                    .name("jobRole")
                    .description("jobRole")
                    .metadata(Map.of("key1", "value1"))
                    .addPermissionsItem(
                        new com.backbase.accesscontrol.functiongroup.api.integration.v1.model.Permission()
                            .businessFunctionName("name1")
                            .addPrivilegesItem("view"))
                    .addPermissionsItem(
                        new com.backbase.accesscontrol.functiongroup.api.integration.v1.model.Permission()
                            .businessFunctionName("name2")
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

        Mockito.when(functionGroupServiceApi.getFunctionGroups(saInternalId))
            .thenReturn(
                Mono.just(new GetFunctionGroups().functionGroups(Collections.singletonList(new FunctionGroupItem()
                    .name("fg1").id("1")
                    .addPermissionsItem(new Permission().businessFunctionName("name1")
                        .addPrivilegesItem("view")
                        .addPrivilegesItem("edit")
                    )))));

        List<BusinessFunctionGroup> newFunctionGroup = List.of(new BusinessFunctionGroup()
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

        Mockito.when(functionGroupIntegrationApi.batchUpdateFunctionGroups(any()))
            .thenReturn(Flux.fromIterable(Collections.emptyList()));
        Mockito.when(functionGroupServiceApi.createFunctionGroup(any()))
            .thenReturn(Mono.just(new ResultId().id("2")));

        Mono<List<BusinessFunctionGroup>> listMono =
            subject.setupFunctionGroups(streamTask, serviceAgreement, newFunctionGroup);
        List<BusinessFunctionGroup> businessFunctionGroups = listMono.block();

        Assertions.assertNotNull(businessFunctionGroups);
        Assertions.assertEquals(2, businessFunctionGroups.size());

        Mockito.verify(functionGroupIntegrationApi)
            .batchUpdateFunctionGroups(Collections.singletonList(new FunctionGroupBatchPutItem()
                .identifier(new FunctionGroupNameIdentifier().name("fg1").serviceAgreementExternalId(saExternalId))
                .newValues(new FunctionGroupUpdate()
                    .name("fg1")
                    .description("fg1")
                    .addPermissionsItem(
                        new com.backbase.accesscontrol.functiongroup.api.integration.v1.model.Permission()
                            .businessFunctionName("name1")
                            .addPrivilegesItem("view")))
            ));
        Mockito.verify(functionGroupServiceApi)
            .createFunctionGroup(new FunctionGroupCreateRequest()
                .serviceAgreementId(saInternalId)
                .name("fg2")
                .description("fg2")
                .type(FunctionGroupCreateRequest.TypeEnum.CUSTOM)
                .addPermissionsItem(new Permission()
                    .businessFunctionName("name2")
                    .addPrivilegesItem("view")
                    .addPrivilegesItem("edit")
                ));
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

    @Test
    void testPartitionList() {
        List<Integer> input = List.of(1, 2, 3, 4, 5, 6, 7);
        int partitionSize = 3;

        List<List<Integer>> result = AccessGroupService.partitionList(input, partitionSize);

        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(List.of(1, 2, 3), result.get(0));
        Assertions.assertEquals(List.of(4, 5, 6), result.get(1));
        Assertions.assertEquals(List.of(7), result.get(2));
    }
}
