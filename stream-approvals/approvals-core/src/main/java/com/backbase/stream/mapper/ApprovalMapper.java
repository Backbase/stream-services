package com.backbase.stream.mapper;

import com.backbase.dbs.approval.api.service.v2.model.PostScopedApprovalTypeRequest;
import com.backbase.dbs.approval.api.service.v2.model.PresentationPostBulkApprovalTypeAssignmentRequest;
import com.backbase.stream.approval.model.ApprovalType;
import com.backbase.stream.approval.model.PolicyAssignment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ApprovalMapper {

    @Mapping(source = "serviceAgreementId", target = "creatorServiceAgreementId")
    PostScopedApprovalTypeRequest mapScopedApprovalType(ApprovalType approvalType);

    @Mapping(source = "approvalTypeAssignments", target = "approvalTypeAssignments")
    PresentationPostBulkApprovalTypeAssignmentRequest mapApprovalTypeAssignment(PolicyAssignment policyAssignment);

}
