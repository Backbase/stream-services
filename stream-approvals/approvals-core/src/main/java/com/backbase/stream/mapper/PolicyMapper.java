package com.backbase.stream.mapper;

import com.backbase.dbs.approval.api.integration.v2.model.IntegrationPostPolicyAssignmentBulkRequest;
import com.backbase.dbs.approval.api.integration.v2.model.PostPolicyRequest;
import com.backbase.stream.approval.model.Policy;
import com.backbase.stream.approval.model.PolicyAssignment;
import java.util.Collections;
import java.util.Optional;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface PolicyMapper {

    PostPolicyRequest mapPolicy(Policy policy);

    @Mapping(source = "policyAssignmentItems", target = "policyAssignments")
    IntegrationPostPolicyAssignmentBulkRequest mapPolicyAssignments(PolicyAssignment policyAssignment);

    @AfterMapping
    default void afterPolicyAssignmentsMapping(PolicyAssignment policyAssignment,
        @MappingTarget IntegrationPostPolicyAssignmentBulkRequest bulkRequest) {
        Optional.ofNullable(bulkRequest.getPolicyAssignments()).orElse(Collections.emptyList())
            .forEach(pa -> pa.setExternalServiceAgreementId(policyAssignment.getExternalServiceAgreementId()));
    }

}
