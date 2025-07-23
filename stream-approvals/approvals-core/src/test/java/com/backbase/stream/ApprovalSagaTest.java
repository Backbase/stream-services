package com.backbase.stream;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.backbase.dbs.accesscontrol.api.service.v3.model.FunctionGroupItem;
import com.backbase.dbs.approval.api.service.v2.model.ApprovalTypeScope;
import com.backbase.dbs.approval.api.service.v2.model.PolicyScope;
import com.backbase.stream.approval.model.Approval;
import com.backbase.stream.approval.model.ApprovalType;
import com.backbase.stream.approval.model.ApprovalTypeAssignmentItem;
import com.backbase.stream.approval.model.IntegrationPolicyAssignmentRequestBounds;
import com.backbase.stream.approval.model.Policy;
import com.backbase.stream.approval.model.PolicyAssignment;
import com.backbase.stream.approval.model.PolicyAssignmentItem;
import com.backbase.stream.approval.model.PolicyItem;
import com.backbase.stream.approval.model.PolicyLogicalItem;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.service.AccessGroupService;
import com.backbase.stream.service.ApprovalsIntegrationService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ApprovalSagaTest {

    @Mock
    private AccessGroupService accessGroupService;

    @Mock
    private ApprovalsIntegrationService approvalsIntegrationService;

    @InjectMocks
    ApprovalSaga approvalSaga;

    private static final String APPROVAL_NAME = "high level approvals";

    @ParameterizedTest
    @MethodSource("approvals")
    void executeTask(Approval approval) {
        ApprovalTask approvalTask = new ApprovalTask(approval);

        when(accessGroupService.getFunctionGroupsForServiceAgreement(any())).thenReturn(
            Mono.just(List.of(new FunctionGroupItem().name("name").id("id"))));
        when(accessGroupService.getServiceAgreementByExternalId(any())).thenReturn(
            Mono.just(new ServiceAgreement().internalId("id")));
        var value = approvalSaga.executeTask(approvalTask).block();
        assertEquals(
            APPROVAL_NAME,
            Objects.requireNonNull(value, "Approval task should not be null.").getApproval().getName());
    }

    public static Stream<Arguments> approvals() {
        return Stream.of(
            Arguments.of(new Approval()
                .name(APPROVAL_NAME)
                .addPoliciesItem(new Policy(List.of(new PolicyLogicalItem().rank(BigDecimal.ONE)
                    .addItemsItem(new PolicyItem().approvalTypeName("Level A").numberOfApprovals(BigDecimal.ONE))),
                    List.of()).scope(PolicyScope.LOCAL.getValue()).serviceAgreementId("internalId"))
                .addApprovalTypesItem(
                    new ApprovalType().rank(BigDecimal.ONE).name("Level A").description("Level A Approvals")
                        .scope(ApprovalTypeScope.LOCAL.getValue()))
                .addPolicyAssignmentsItem(new PolicyAssignment().externalServiceAgreementId("externalId")
                    .addApprovalTypeAssignmentsItem(
                        new ApprovalTypeAssignmentItem().approvalTypeName("Level A").jobProfileName("All"))
                    .addPolicyAssignmentItemsItem(new PolicyAssignmentItem().externalServiceAgreementId("externalId")
                        .addFunctionsItem("Assign Users")
                        .addBoundsItem(new IntegrationPolicyAssignmentRequestBounds("id").policyName("Dual Control"))
                        .addBoundsItem(new IntegrationPolicyAssignmentRequestBounds(null).policyName("Control"))))),
            Arguments.of(new Approval()
                .name(APPROVAL_NAME)
                .addPoliciesItem(new Policy(List.of(new PolicyLogicalItem().rank(BigDecimal.ONE)
                    .addItemsItem(new PolicyItem().approvalTypeName("Level A").numberOfApprovals(BigDecimal.ONE))),
                    List.of()).scope(PolicyScope.SYSTEM.getValue()))
                .addApprovalTypesItem(
                    new ApprovalType().rank(BigDecimal.ONE).name("Level A").description("Level A Approvals").scope(
                        ApprovalTypeScope.SYSTEM.getValue()))
                .addPolicyAssignmentsItem(new PolicyAssignment().externalServiceAgreementId("externalId")
                    .addApprovalTypeAssignmentsItem(
                        new ApprovalTypeAssignmentItem().approvalTypeName("Level A").jobProfileName("All"))
                    .addPolicyAssignmentItemsItem(new PolicyAssignmentItem().externalServiceAgreementId("externalId")
                        .addFunctionsItem("Assign Users")
                        .addBoundsItem(new IntegrationPolicyAssignmentRequestBounds("id").policyName("Dual Control"))
                        .addBoundsItem(new IntegrationPolicyAssignmentRequestBounds(null).policyName("Control"))))),
            Arguments.of(new Approval()
                    .name(APPROVAL_NAME)
                    .addPoliciesItem(new Policy(List.of(new PolicyLogicalItem().rank(BigDecimal.ONE)
                        .addItemsItem(new PolicyItem().approvalTypeName("Level A").numberOfApprovals(BigDecimal.ONE))),
                        List.of()))
                    .addApprovalTypesItem(
                        new ApprovalType().rank(BigDecimal.ONE).name("Level A").description("Level A Approvals"))
                    .addPolicyAssignmentsItem(new PolicyAssignment().externalServiceAgreementId("externalId")
                        .addApprovalTypeAssignmentsItem(
                            new ApprovalTypeAssignmentItem().approvalTypeName("Level A").jobProfileName("All"))
                        .addPolicyAssignmentItemsItem(new PolicyAssignmentItem().externalServiceAgreementId("externalId")
                            .addFunctionsItem("Assign Users")
                            .addBoundsItem(new IntegrationPolicyAssignmentRequestBounds("id").policyName("Dual Control"))
                            .addBoundsItem(new IntegrationPolicyAssignmentRequestBounds(null).policyName("Control")))))
        );
    }

    @Test
    void rollBack() {
        ApprovalTask streamTask = Mockito.spy(new ApprovalTask());
        Mono<ApprovalTask> approvalTaskMono = approvalSaga.rollBack(streamTask);

        assertNotNull(approvalTaskMono);

        approvalTaskMono.block();

        Mockito.verify(streamTask, Mockito.never()).getData();
    }

}