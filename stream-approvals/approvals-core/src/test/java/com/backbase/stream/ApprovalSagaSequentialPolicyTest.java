package com.backbase.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.dbs.approval.api.management.service.v1.model.PostPolicyServiceRequest;
import com.backbase.dbs.approval.api.management.service.v1.model.PostPolicyServiceResponse;
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
import com.backbase.dbs.approval.api.service.v2.model.PresentationPostPolicyAssignmentBulkRequest;
import com.backbase.stream.approval.model.Approval;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.service.AccessGroupService;
import com.backbase.stream.service.ApprovalsIntegrationService;
import com.backbase.stream.worker.model.TaskHistory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

class ApprovalSagaSequentialPolicyTest {

    private static final Path RESOURCES = Path.of("src/test/resources/sequential");
    private static final String MANAGEMENT_POLICY_ID = "management-policy-id";

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final ApprovalTypesApi approvalTypesApi = mock(ApprovalTypesApi.class);
    private final ApprovalTypeAssignmentsApi approvalTypeAssignmentsApi = mock(ApprovalTypeAssignmentsApi.class);
    private final PoliciesApi policiesApi = mock(PoliciesApi.class);
    private final PolicyAssignmentsApi policyAssignmentsApi = mock(PolicyAssignmentsApi.class);
    private final com.backbase.dbs.approval.api.management.service.v1.PoliciesApi managementPoliciesApi =
        mock(com.backbase.dbs.approval.api.management.service.v1.PoliciesApi.class);
    private final AccessGroupService accessGroupService = mock(AccessGroupService.class);

    private final ApprovalSaga approvalSaga = new ApprovalSaga(accessGroupService,
        new ApprovalsIntegrationService(approvalTypesApi, approvalTypeAssignmentsApi, policiesApi,
            policyAssignmentsApi, accessGroupService, managementPoliciesApi));

    @Test
    void shouldCreateExpressionPolicyThroughManagementApiAndKeepOtherPoliciesOnServiceApiV2() throws Exception {
        Approval approval = MAPPER.readValue(RESOURCES.resolve("sequential-approvals.json").toFile(), Approval.class);
        when(accessGroupService.getServiceAgreementByExternalId(anyString())).thenAnswer(invocation ->
            Mono.just(new ServiceAgreement().internalId("sa-id-" + invocation.getArgument(0))));
        when(approvalTypesApi.postScopedApprovalType(any())).thenAnswer(invocation -> {
            PostScopedApprovalTypeRequest request = invocation.getArgument(0);
            return Mono.just(new PostApprovalTypeResponse().approvalType(
                new ApprovalTypeDto().id("approval-type-id-" + request.getName()).name(request.getName())));
        });
        when(policiesApi.postScopedPolicy(any())).thenAnswer(invocation -> {
            PostPolicyServiceApiRequest request = invocation.getArgument(0);
            return Mono.just(new PostPolicyServiceApiResponse().policy(
                new PolicyServiceApiResponseDto().id("policy-id-" + request.getName()).name(request.getName())));
        });
        when(managementPoliciesApi.postPolicy(any()))
            .thenReturn(Mono.just(new PostPolicyServiceResponse().id(MANAGEMENT_POLICY_ID)));
        when(policyAssignmentsApi.postCreatesBulkPolicyAssignments(any())).thenReturn(Mono.empty());
        when(accessGroupService.getFunctionGroupsForServiceAgreement(anyString())).thenReturn(Mono.just(List.of()));

        ApprovalTask task = approvalSaga.executeTask(new ApprovalTask(approval)).block();

        assertThat(Objects.requireNonNull(task).getHistory())
            .noneMatch(history -> history.getSeverity() == TaskHistory.Severity.ERROR);

        ArgumentCaptor<PostPolicyServiceRequest> managementRequest =
            ArgumentCaptor.forClass(PostPolicyServiceRequest.class);
        verify(managementPoliciesApi).postPolicy(managementRequest.capture());
        assertThat(MAPPER.readTree(MAPPER.writeValueAsString(managementRequest.getValue())))
            .isEqualTo(MAPPER.readTree(RESOURCES.resolve("expected-management-policy-request.json").toFile()));

        ArgumentCaptor<PostPolicyServiceApiRequest> serviceApiV2Request =
            ArgumentCaptor.forClass(PostPolicyServiceApiRequest.class);
        verify(policiesApi).postScopedPolicy(serviceApiV2Request.capture());
        assertThat(serviceApiV2Request.getAllValues())
            .extracting(PostPolicyServiceApiRequest::getName)
            .containsExactly("Single Control");

        ArgumentCaptor<PresentationPostPolicyAssignmentBulkRequest> assignmentRequest =
            ArgumentCaptor.forClass(PresentationPostPolicyAssignmentBulkRequest.class);
        verify(policyAssignmentsApi).postCreatesBulkPolicyAssignments(assignmentRequest.capture());
        assertThat(MAPPER.valueToTree(assignmentRequest.getValue()).findValuesAsText("policyId"))
            .containsExactly(MANAGEMENT_POLICY_ID);
    }
}
