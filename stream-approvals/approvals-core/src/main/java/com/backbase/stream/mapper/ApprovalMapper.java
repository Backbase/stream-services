package com.backbase.stream.mapper;

import com.backbase.dbs.approval.api.integration.v2.model.IntegrationPostBulkApprovalTypeAssignmentRequest;
import com.backbase.dbs.approval.api.integration.v2.model.PostApprovalTypeRequest;
import com.backbase.stream.approval.model.ApprovalType;
import com.backbase.stream.approval.model.PolicyAssignment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ApprovalMapper {

    PostApprovalTypeRequest mapApprovalType(ApprovalType approvalType);

    @Mapping(source = "approvalTypeAssignments", target = "approvalTypeAssignments")
    IntegrationPostBulkApprovalTypeAssignmentRequest mapApprovalTypeAssignment(PolicyAssignment policyAssignment);

}
