package com.backbase.stream;

import static com.backbase.stream.product.utils.StreamUtils.nullableCollectionToStream;

import com.backbase.dbs.accesscontrol.api.service.v3.model.FunctionGroupItem;
import com.backbase.stream.approval.model.Approval;
import com.backbase.stream.approval.model.ApprovalType;
import com.backbase.stream.approval.model.Policy;
import com.backbase.stream.approval.model.PolicyAssignmentItem;
import com.backbase.stream.approval.model.PolicyItem;
import com.backbase.stream.service.AccessGroupService;
import com.backbase.stream.service.ApprovalsIntegrationService;
import com.backbase.stream.worker.StreamTaskExecutor;
import io.micrometer.tracing.annotation.ContinueSpan;
import io.micrometer.tracing.annotation.SpanTag;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Approval Saga. This Service creates Approvals and their supporting objects from a {@link
 * Approval} aggregate object. For each Approval object it will either create a new one. Next it
 * will assign all defined policies
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ApprovalSaga implements StreamTaskExecutor<ApprovalTask> {

  public static final String APPROVAL_TYPE_ENTITY = "APPROVAL_TYPE_ENTITY";
  public static final String ASSIGN_APPROVAL_TYPE_LEVEL = "ASSIGN_APPROVAL_TYPE_LEVEL";
  public static final String POLICY_ENTITY = "POLICY_ENTITY";
  public static final String UPSERT_APPROVAL_TYPES = "upsert-approval-types";
  public static final String UPSERT_POLICY = "upsert-policy";
  public static final String UPSERT_POLICY_ASSIGNMENT = "upsert-policy-assignments";
  public static final String ASSIGNMENT_APPROVAL_TYPE_LEVELS = "assignment-approval-type-levels";
  public static final String FAILED = "failed";
  public static final String UPSERT = "upsert";
  public static final String UPSERT_APPROVAL_TYPE = "upsert-approval-type";
  public static final String ASSIGN_POLICY = "assign-policy";

  private final AccessGroupService accessGroupService;
  private final ApprovalsIntegrationService approvalsIntegrationService;

  @Override
  public Mono<ApprovalTask> executeTask(@SpanTag(value = "streamTask") ApprovalTask streamTask) {
    return upsertApprovalTypes(streamTask)
        .flatMap(this::upsertPolicy)
        .flatMap(this::setupPolicyAssignment)
        .flatMap(this::assignmentApprovalTypeLevels);
  }

  @Override
  public Mono<ApprovalTask> rollBack(ApprovalTask streamTask) {
    // GET CREATED AND EVENTS AND CALL DELETE ENDPOINTS IN REVERSE
    return Mono.just(streamTask);
  }

  @ContinueSpan(log = UPSERT_APPROVAL_TYPES)
  private Mono<ApprovalTask> upsertApprovalTypes(@SpanTag(value = "streamTask") ApprovalTask task) {
    task.info(APPROVAL_TYPE_ENTITY, UPSERT, null, null, null, "Upsert Approval Types");
    return Flux.fromStream(nullableCollectionToStream(task.getData().getApprovalTypes()))
        .flatMap(approvalsIntegrationService::createApprovalType)
        .onErrorContinue(
            (throwable, o) ->
                task.error(
                    APPROVAL_TYPE_ENTITY,
                    UPSERT_APPROVAL_TYPE,
                    FAILED,
                    null,
                    null,
                    throwable,
                    throwable.getMessage(),
                    throwable.getMessage()))
        .collectList()
        .map(
            approvals -> {
              Map<String, String> approvalTypeIdByName =
                  approvals.stream()
                      .collect(
                          Collectors.toMap(ApprovalType::getName, ApprovalType::getInternalId));
              task.getData().setApprovalTypeIdByName(approvalTypeIdByName);
              return task;
            });
  }

  @ContinueSpan(log = UPSERT_POLICY)
  private Mono<ApprovalTask> upsertPolicy(@SpanTag(value = "streamTask") ApprovalTask task) {
    task.info(POLICY_ENTITY, UPSERT, null, null, null, "Upsert Policies");

    Map<String, String> approvalTypeIdByName = task.getData().getApprovalTypeIdByName();
    log.debug("approvalTypeIdByName: {}", approvalTypeIdByName);

    return Flux.fromStream(nullableCollectionToStream(task.getData().getPolicies()))
        .map(
            p -> {
              propagateCreatedApprovalTypeId(approvalTypeIdByName, p.getItems());
              Optional.ofNullable(p.getLogicalItems())
                  .orElse(Collections.emptyList())
                  .forEach(
                      li -> propagateCreatedApprovalTypeId(approvalTypeIdByName, li.getItems()));
              return p;
            })
        .flatMap(approvalsIntegrationService::createPolicy)
        .onErrorContinue(
            (throwable, o) ->
                task.error(
                    POLICY_ENTITY,
                    UPSERT_POLICY,
                    FAILED,
                    null,
                    null,
                    throwable,
                    throwable.getMessage(),
                    throwable.getMessage()))
        .collectList()
        .map(
            policies -> {
              Map<String, String> policyIdByName =
                  policies.stream()
                      .collect(Collectors.toMap(Policy::getName, Policy::getInternalId));
              task.getData().setPolicyIdByName(policyIdByName);
              return task;
            });
  }

  @ContinueSpan(log = UPSERT_POLICY_ASSIGNMENT)
  private Mono<ApprovalTask> setupPolicyAssignment(
      @SpanTag(value = "streamTask") ApprovalTask task) {
    task.info(ASSIGN_POLICY, UPSERT, null, null, null, "Setup Policy assignments");

    Map<String, String> policyIdByName = task.getData().getPolicyIdByName();

    return Flux.fromStream(nullableCollectionToStream(task.getData().getPolicyAssignments()))
        .map(
            pa -> {
              String externalServiceAgreementId = pa.getExternalServiceAgreementId();
              log.debug("pa.getExternalServiceAgreementId(): {}", externalServiceAgreementId);
              List<PolicyAssignmentItem> policyAssignmentItems = pa.getPolicyAssignmentItems();
              policyAssignmentItems.stream()
                  .map(PolicyAssignmentItem::getBounds)
                  .flatMap(Collection::stream)
                  .forEach(
                      b -> {
                        if (Objects.isNull(b.getPolicyId())) {
                          b.setPolicyId(policyIdByName.get(b.getPolicyName()));
                        }
                      });
              return pa;
            })
        .flatMap(approvalsIntegrationService::assignPolicies)
        .onErrorContinue(
            (throwable, o) ->
                task.error(
                    ASSIGN_POLICY,
                    UPSERT_POLICY_ASSIGNMENT,
                    FAILED,
                    null,
                    null,
                    throwable,
                    throwable.getMessage(),
                    throwable.getMessage()))
        .collectList()
        .map(v -> task);
  }

  @ContinueSpan(log = ASSIGNMENT_APPROVAL_TYPE_LEVELS)
  private Mono<ApprovalTask> assignmentApprovalTypeLevels(
      @SpanTag(value = "streamTask") ApprovalTask task) {
    task.info(ASSIGN_APPROVAL_TYPE_LEVEL, UPSERT, null, null, null, "Assign Approval Type level");
    Approval approval = task.getData();
    return Flux.fromStream(nullableCollectionToStream(task.getData().getPolicyAssignments()))
        .flatMap(
            pa -> {
              String externalServiceAgreementId = pa.getExternalServiceAgreementId();
              return accessGroupService
                  .getServiceAgreementByExternalId(externalServiceAgreementId)
                  .flatMap(
                      sa ->
                          accessGroupService.getFunctionGroupsForServiceAgreement(
                              sa.getInternalId()))
                  .map(
                      fgs ->
                          fgs.stream()
                              .filter(fg -> Objects.nonNull(fg.getId()))
                              .collect(
                                  Collectors.toMap(
                                      FunctionGroupItem::getName, FunctionGroupItem::getId)))
                  .map(
                      functionGroupIdByName -> {
                        approval.setFunctionGroupIdByName(functionGroupIdByName);
                        log.debug("functionGroupIdByName: {}", functionGroupIdByName);
                        Map<String, String> approvalTypeIdByName =
                            approval.getApprovalTypeIdByName();
                        log.debug("approvalTypeIdByName: {}", approvalTypeIdByName);
                        Optional.ofNullable(approval.getPolicyAssignments())
                            .orElse(Collections.emptyList())
                            .stream()
                            .map(
                                p ->
                                    Optional.ofNullable(p.getApprovalTypeAssignments())
                                        .orElse(Collections.emptyList()))
                            .flatMap(Collection::stream)
                            .forEach(
                                ata -> {
                                  String approvalTypeId =
                                      approvalTypeIdByName.get(ata.getApprovalTypeName());
                                  if (Objects.nonNull(approvalTypeId)) {
                                    ata.setApprovalTypeId(approvalTypeId);
                                  }
                                  String functionGroupId =
                                      functionGroupIdByName.get(ata.getJobProfileName());
                                  if (Objects.nonNull(functionGroupId)) {
                                    ata.setJobProfileId(functionGroupId);
                                  }
                                });
                        return pa;
                      });
            })
        .flatMap(approvalsIntegrationService::assignApprovalTypeLevels)
        .onErrorContinue((throwable, o) -> Mono.just(task))
        .collectList()
        .map(v -> task);
  }

  private void propagateCreatedApprovalTypeId(
      Map<String, String> approvalTypeIdByName, List<PolicyItem> items) {
    Optional.ofNullable(items)
        .orElse(Collections.emptyList())
        .forEach(
            i -> {
              String newId = approvalTypeIdByName.get(i.getApprovalTypeName());
              if (Objects.nonNull(newId)) {
                i.setApprovalTypeId(newId);
              }
            });
  }
}
