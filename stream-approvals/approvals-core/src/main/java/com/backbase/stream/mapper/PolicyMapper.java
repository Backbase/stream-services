package com.backbase.stream.mapper;

import com.backbase.dbs.approval.api.service.v2.model.PostPolicyRequest;
import com.backbase.dbs.approval.api.service.v2.model.PresentationPostPolicyAssignmentBulkRequest;
import com.backbase.stream.approval.model.Policy;
import com.backbase.stream.approval.model.PolicyAssignment;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface PolicyMapper {

    PostPolicyRequest mapPolicy(Policy policy);

    @Mapping(source = "policyAssignmentItems", target = "policyAssignments")
    PresentationPostPolicyAssignmentBulkRequest mapPolicyAssignments(
        PolicyAssignment policyAssignment);

    @AfterMapping
    default void afterPolicyAssignmentsMapping(
        PolicyAssignment policyAssignment,
        @MappingTarget PresentationPostPolicyAssignmentBulkRequest bulkRequest) {
        Optional.ofNullable(bulkRequest.getPolicyAssignments()).orElse(Collections.emptyList()).stream()
            .filter(pa -> Objects.isNull(pa.getExternalServiceAgreementId()))
            .forEach(
                pa ->
                    pa.setExternalServiceAgreementId(policyAssignment.getExternalServiceAgreementId()));
    }
}
