package com.backbase.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.backbase.accesscontrol.functiongroup.api.service.v1.model.FunctionGroupItem;
import com.backbase.dbs.approval.api.service.v2.ApprovalTypeAssignmentsApi;
import com.backbase.dbs.approval.api.service.v2.ApprovalTypesApi;
import com.backbase.dbs.approval.api.service.v2.PoliciesApi;
import com.backbase.dbs.approval.api.service.v2.PolicyAssignmentsApi;
import com.backbase.dbs.approval.api.service.v2.model.ApprovalTypeDto;
import com.backbase.dbs.approval.api.service.v2.model.PolicyServiceApiResponseDto;
import com.backbase.dbs.approval.api.service.v2.model.PostApprovalTypeResponse;
import com.backbase.dbs.approval.api.service.v2.model.PostPolicyServiceApiRequest;
import com.backbase.dbs.approval.api.service.v2.model.PostPolicyServiceApiResponse;
import com.backbase.dbs.approval.api.service.v2.model.PostScopedApprovalTypeRequest;
import com.backbase.dbs.approval.api.service.v2.model.PresentationPostBulkApprovalTypeAssignmentResponse;
import com.backbase.stream.approval.model.Approval;
import com.backbase.stream.approval.model.ApprovalTypeAssignmentItem;
import com.backbase.stream.approval.model.PolicyAssignment;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.service.AccessGroupService;
import com.backbase.stream.service.ApprovalsIntegrationService;
import com.backbase.stream.worker.model.TaskHistory;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Mono;

/**
 * Runs the approval saga over approval files without expressions and compares every Service API v2 request it sends
 * with a snapshot recorded before expression-based policy support was added. Run with {@code -Dgolden.update=true} to
 * re-record.
 */
class ApprovalSagaServiceApiV2GoldenTest {

    private static final Path GOLDEN_DIR = Path.of("src/test/resources/golden");

    private static final ObjectMapper MAPPER = JsonMapper.builder()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
        .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
        .enable(SerializationFeature.INDENT_OUTPUT)
        .serializationInclusion(JsonInclude.Include.NON_NULL)
        .build();

    @ParameterizedTest
    @ValueSource(strings = "legacy-approvals")
    void shouldSendTheSameServiceApiV2RequestsForExistingBootstrapFiles(String dataset) throws Exception {
        Approval approval = MAPPER.readValue(GOLDEN_DIR.resolve(dataset + ".json").toFile(), Approval.class);
        Map<String, List<Object>> requests = new LinkedHashMap<>();
        ApprovalSaga saga = sagaRecordingRequestsInto(requests, approval);

        ApprovalTask task = saga.executeTask(new ApprovalTask(approval)).block();

        Map<String, Object> actual = new LinkedHashMap<>(requests);
        actual.put("taskErrors", Objects.requireNonNull(task).getHistory().stream()
            .filter(h -> h.getSeverity() == TaskHistory.Severity.ERROR)
            .map(h -> h.getEntity() + " " + h.getOperation() + ": " + h.getErrorMessage())
            .toList());
        String actualJson = MAPPER.writeValueAsString(actual);

        Path golden = GOLDEN_DIR.resolve(dataset + ".v2-requests.json");
        if (Boolean.getBoolean("golden.update")) {
            Files.writeString(golden, actualJson);
        }
        assertThat(actualJson).isEqualTo(Files.readString(golden));
    }

    private static ApprovalSaga sagaRecordingRequestsInto(Map<String, List<Object>> requests, Approval approval) {
        ApprovalTypesApi approvalTypesApi = mock(ApprovalTypesApi.class);
        ApprovalTypeAssignmentsApi approvalTypeAssignmentsApi = mock(ApprovalTypeAssignmentsApi.class);
        PoliciesApi policiesApi = mock(PoliciesApi.class);
        PolicyAssignmentsApi policyAssignmentsApi = mock(PolicyAssignmentsApi.class);
        AccessGroupService accessGroupService = mock(AccessGroupService.class);

        when(approvalTypesApi.postScopedApprovalType(any())).thenAnswer(invocation -> {
            PostScopedApprovalTypeRequest request = record(requests, "approvalTypes", invocation.getArgument(0));
            return Mono.just(new PostApprovalTypeResponse().approvalType(
                new ApprovalTypeDto().id("approval-type-id-" + request.getName()).name(request.getName())));
        });
        when(policiesApi.postScopedPolicy(any())).thenAnswer(invocation -> {
            PostPolicyServiceApiRequest request = record(requests, "policies", invocation.getArgument(0));
            return Mono.just(new PostPolicyServiceApiResponse().policy(
                new PolicyServiceApiResponseDto().id("policy-id-" + request.getName()).name(request.getName())));
        });
        when(policyAssignmentsApi.postCreatesBulkPolicyAssignments(any())).thenAnswer(invocation -> {
            record(requests, "policyAssignments", invocation.getArgument(0));
            return Mono.empty();
        });
        when(approvalTypeAssignmentsApi.postBulk(any())).thenAnswer(invocation -> {
            record(requests, "approvalTypeAssignments", invocation.getArgument(0));
            return Mono.just(new PresentationPostBulkApprovalTypeAssignmentResponse());
        });
        when(accessGroupService.getServiceAgreementByExternalId(anyString())).thenAnswer(invocation -> {
            String externalId = invocation.getArgument(0);
            return Mono.just(new ServiceAgreement().externalId(externalId).internalId("sa-id-" + externalId));
        });
        List<FunctionGroupItem> functionGroups = Optional.ofNullable(approval.getPolicyAssignments())
            .orElse(List.of()).stream()
            .map(PolicyAssignment::getApprovalTypeAssignments)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .map(ApprovalTypeAssignmentItem::getJobProfileName)
            .filter(Objects::nonNull)
            .distinct()
            .map(name -> new FunctionGroupItem().name(name).id("function-group-id-" + name))
            .toList();
        when(accessGroupService.getFunctionGroupsForServiceAgreement(anyString()))
            .thenReturn(Mono.just(functionGroups));

        ApprovalsIntegrationService integrationService = new ApprovalsIntegrationService(approvalTypesApi,
            approvalTypeAssignmentsApi, policiesApi, policyAssignmentsApi, accessGroupService);
        return new ApprovalSaga(accessGroupService, integrationService);
    }

    private static <T> T record(Map<String, List<Object>> requests, String key, T request) throws Exception {
        requests.computeIfAbsent(key, k -> new ArrayList<>()).add(MAPPER.readTree(MAPPER.writeValueAsString(request)));
        return request;
    }
}
