package com.backbase.stream.mapper;

import com.backbase.dbs.approval.api.service.v2.model.PolicyItemServiceApiRequestDto;
import com.backbase.dbs.approval.api.service.v2.model.PolicyLogicalItemServiceApiRequestDto;
import com.backbase.dbs.approval.api.service.v2.model.PostPolicyServiceApiRequest;
import com.backbase.dbs.approval.api.service.v2.model.PresentationPostPolicyAssignmentBulkRequest;
import com.backbase.stream.approval.model.Policy;
import com.backbase.stream.approval.model.PolicyAssignment;
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

}
