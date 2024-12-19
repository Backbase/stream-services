package com.backbase.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.backbase.dbs.accesscontrol.api.service.v3.model.FunctionGroupItem;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ApprovalSagaTest {

  @Mock private AccessGroupService accessGroupService;

  @Mock private ApprovalsIntegrationService approvalsIntegrationService;

  @InjectMocks ApprovalSaga approvalSaga;

  @Test
  void executeTask() {
    String approvalName = "high level approvals";
    Approval approval =
        new Approval()
            .name(approvalName)
            .addPoliciesItem(
                new Policy(
                    List.of(
                        new PolicyLogicalItem()
                            .rank(BigDecimal.ONE)
                            .addItemsItem(
                                new PolicyItem()
                                    .approvalTypeName("Level A")
                                    .numberOfApprovals(BigDecimal.ONE))),
                    List.of()))
            .addApprovalTypesItem(
                new ApprovalType()
                    .rank(BigDecimal.ONE)
                    .name("Level A")
                    .description("Level A Approvals"))
            .addPolicyAssignmentsItem(
                new PolicyAssignment()
                    .externalServiceAgreementId("externalId")
                    .addApprovalTypeAssignmentsItem(
                        new ApprovalTypeAssignmentItem()
                            .approvalTypeName("Level A")
                            .jobProfileName("All"))
                    .addPolicyAssignmentItemsItem(
                        new PolicyAssignmentItem()
                            .externalServiceAgreementId("externalId")
                            .addFunctionsItem("Assign Users")
                            .addBoundsItem(
                                new IntegrationPolicyAssignmentRequestBounds("id")
                                    .policyName("Dual Control"))
                            .addBoundsItem(
                                new IntegrationPolicyAssignmentRequestBounds(null)
                                    .policyName("Control"))));
    ApprovalTask approvalTask = new ApprovalTask(approval);

    when(accessGroupService.getFunctionGroupsForServiceAgreement(any()))
        .thenReturn(Mono.just(List.of(new FunctionGroupItem().name("name").id("id"))));
    when(accessGroupService.getServiceAgreementByExternalId(any()))
        .thenReturn(Mono.just(new ServiceAgreement().internalId("id")));
    var value = approvalSaga.executeTask(approvalTask).block();
    assertEquals(approvalName, value.getApproval().getName());
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
