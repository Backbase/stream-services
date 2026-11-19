package com.backbase.stream.mapper;

import com.backbase.dbs.approval.api.management.service.v1.model.PolicyExpressionDto;
import com.backbase.dbs.approval.api.management.service.v1.model.PolicyExpressionOperator;
import com.backbase.dbs.approval.api.management.service.v1.model.PolicyExpressionOperatorType;
import com.backbase.dbs.approval.api.management.service.v1.model.PolicyExpressionUnit;
import com.backbase.dbs.approval.api.management.service.v1.model.PolicyLocalScope;
import com.backbase.dbs.approval.api.management.service.v1.model.PolicyScope;
import com.backbase.dbs.approval.api.management.service.v1.model.PolicySystemScope;
import com.backbase.dbs.approval.api.management.service.v1.model.PostPolicyServiceRequest;
import com.backbase.dbs.approval.api.service.v2.model.PolicyItemServiceApiRequestDto;
import com.backbase.dbs.approval.api.service.v2.model.PolicyLogicalItemServiceApiRequestDto;
import com.backbase.dbs.approval.api.service.v2.model.PostPolicyServiceApiRequest;
import com.backbase.dbs.approval.api.service.v2.model.PresentationPostPolicyAssignmentBulkRequest;
import com.backbase.stream.approval.model.Policy;
import com.backbase.stream.approval.model.PolicyAssignment;
import com.backbase.stream.approval.model.PolicyExpression;
import com.backbase.stream.approval.model.PolicyItem;
import com.backbase.stream.approval.model.PolicyLogicalItem;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface PolicyMapper {

    String LOCAL_SCOPE = "LOCAL";

    @Mapping(source = "name", target = "name")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "scope", target = "scope")
    @Mapping(source = "serviceAgreementId", target = "serviceAgreementId")
    @Mapping(source = "items", target = "items")
    @Mapping(source = "logicalItems", target = "logicalItems")
    PostPolicyServiceApiRequest mapScopedPolicy(Policy policy);

    List<PolicyItemServiceApiRequestDto> mapPolicyItems(List<PolicyItem> items);

    @Mapping(source = "approvalTypeId", target = "approvalTypeId")
    @Mapping(source = "numberOfApprovals", target = "numberOfApprovals")
    PolicyItemServiceApiRequestDto mapPolicyItem(PolicyItem item);

    List<PolicyLogicalItemServiceApiRequestDto> mapPolicyLogicalItems(List<PolicyLogicalItem> logicalItems);

    @Mapping(source = "items", target = "items")
    PolicyLogicalItemServiceApiRequestDto mapPolicyLogicalItem(PolicyLogicalItem logicalItem);

    @Mapping(source = "policyAssignmentItems", target = "policyAssignments")
    PresentationPostPolicyAssignmentBulkRequest mapPolicyAssignments(PolicyAssignment policyAssignment);

    @AfterMapping
    default void afterPolicyAssignmentsMapping(PolicyAssignment policyAssignment,
        @MappingTarget PresentationPostPolicyAssignmentBulkRequest bulkRequest) {
        Optional.ofNullable(bulkRequest.getPolicyAssignments()).orElse(Collections.emptyList())
            .stream()
            .filter(pa -> Objects.isNull(pa.getExternalServiceAgreementId()))
            .forEach(pa -> pa.setExternalServiceAgreementId(policyAssignment.getExternalServiceAgreementId()));
    }

    /**
     * Maps an expression-based policy to an Approval Management Service API request.
     *
     * @throws IllegalArgumentException when the expression tree is incomplete or an approval type is unresolved
     */
    default PostPolicyServiceRequest mapExpressionPolicy(Policy policy) {
        return new PostPolicyServiceRequest()
            .name(policy.getName())
            .description(policy.getDescription())
            .scope(mapPolicyScope(policy))
            .expression(mapPolicyExpression(policy.getExpression()));
    }

    /**
     * Maps the bootstrap scope string to the scope object. Without a scope, a policy with a service agreement is
     * local, matching how the Service API v2 path decides.
     */
    default PolicyScope mapPolicyScope(Policy policy) {
        boolean local = Optional.ofNullable(policy.getScope())
            .map(LOCAL_SCOPE::equalsIgnoreCase)
            .orElse(policy.getServiceAgreementId() != null);
        return local
            ? new PolicyLocalScope().serviceAgreementId(policy.getServiceAgreementId())
            : new PolicySystemScope();
    }

    /**
     * Maps a policy expression tree recursively. A node with an operator is an operator node; a node with an
     * approval type is a unit node.
     *
     * @throws IllegalArgumentException when a node is both or neither, a unit has operands or misses its count, or
     *                                  an approval type is unresolved
     */
    default PolicyExpressionDto mapPolicyExpression(PolicyExpression expression) {
        boolean operator = expression.getOperator() != null;
        boolean unit = expression.getApprovalTypeName() != null || expression.getApprovalTypeId() != null;
        if (operator == unit) {
            throw new IllegalArgumentException(operator
                ? "Policy expression node has both 'operator' and an approval type"
                : "Policy expression node has neither 'operator' nor an approval type");
        }
        return operator ? mapOperator(expression) : mapUnit(expression);
    }

    private PolicyExpressionOperator mapOperator(PolicyExpression expression) {
        return new PolicyExpressionOperator()
            .operatorType(PolicyExpressionOperatorType.fromValue(expression.getOperator().getValue()))
            .operands(Optional.ofNullable(expression.getOperands()).orElse(Collections.emptyList())
                .stream()
                .map(this::mapPolicyExpression)
                .toList());
    }

    private PolicyExpressionUnit mapUnit(PolicyExpression expression) {
        if (expression.getApprovalTypeId() == null) {
            throw new IllegalArgumentException(
                "Approval type '" + expression.getApprovalTypeName() + "' could not be resolved to an id");
        }
        if (expression.getOperands() != null && !expression.getOperands().isEmpty()) {
            throw new IllegalArgumentException(
                "Policy expression unit '" + expression.getApprovalTypeName() + "' cannot have 'operands'");
        }
        if (expression.getCount() == null) {
            throw new IllegalArgumentException(
                "Policy expression unit '" + expression.getApprovalTypeName() + "' is missing 'count'");
        }
        return new PolicyExpressionUnit()
            .approvalTypeId(expression.getApprovalTypeId())
            .count(expression.getCount());
    }

}
