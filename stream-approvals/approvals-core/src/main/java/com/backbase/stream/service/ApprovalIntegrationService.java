package com.backbase.stream.service;

import com.backbase.dbs.approval.api.service.v2.ApprovalTypeAssignmentsApi;
import com.backbase.dbs.approval.api.service.v2.ApprovalTypesApi;
import com.backbase.dbs.approval.api.service.v2.PoliciesApi;
import com.backbase.dbs.approval.api.service.v2.PolicyAssignmentsApi;
import com.backbase.dbs.approval.api.service.v2.model.PostApprovalTypeResponse;
import com.backbase.dbs.approval.api.service.v2.model.PostPolicyResponse;
import com.backbase.stream.approval.model.ApprovalType;
import com.backbase.stream.approval.model.Policy;
import com.backbase.stream.approval.model.PolicyAssignment;
import com.backbase.stream.exceptions.ApprovalTypeException;
import com.backbase.stream.exceptions.PolicyAssignmentException;
import com.backbase.stream.exceptions.PolicyException;
import com.backbase.stream.mapper.ApprovalMapper;
import com.backbase.stream.mapper.PolicyMapper;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class ApprovalIntegrationService {

    private final ApprovalMapper approvalMapper = Mappers.getMapper(ApprovalMapper.class);
    private final PolicyMapper policyMapper = Mappers.getMapper(PolicyMapper.class);

    private final ApprovalTypesApi approvalTypesApi;
    private final ApprovalTypeAssignmentsApi approvalTypeAssignmentsApi;
    private final PoliciesApi policiesApi;
    private final PolicyAssignmentsApi policyAssignmentsApi;

    public Mono<ApprovalType> createApprovalType(ApprovalType approvalType) {
        return approvalTypesApi.postApprovalType(approvalMapper.mapApprovalType(approvalType))
            .map(PostApprovalTypeResponse::getApprovalType)
            .map(at -> {
                String id = at.getId();
                log.info("Created Approval Type: {} with ID: {}", at.getName(), id);
                approvalType.setInternalId(id);
                return approvalType;
            })
            .doOnError(WebClientResponseException.class, this::handleWebClientResponseException)
            .onErrorResume(WebClientResponseException.class, exception ->
                Mono.error(new ApprovalTypeException(approvalType, "Failed to create Approval Type Level", exception)))
            .onErrorStop();
    }

    public Mono<Policy> createPolicy(Policy policy) {
        return policiesApi.postPolicy(policyMapper.mapPolicy(policy))
            .map(PostPolicyResponse::getPolicy)
            .map(at -> {
                String id = at.getId();
                log.info("Created Approval Type: {} with ID: {}", at.getName(), id);
                policy.setInternalId(id);
                return policy;
            })
            .doOnError(WebClientResponseException.class, this::handleWebClientResponseException)
            .onErrorResume(WebClientResponseException.class, exception ->
                Mono.error(new PolicyException(policy, "Failed to create Policy", exception)))
            .onErrorStop();
    }

    public Mono<PolicyAssignment> assignPolicies(PolicyAssignment policyAssignment) {
        return policyAssignmentsApi
            .postCreatesBulkPolicyAssignments(policyMapper.mapPolicyAssignments(policyAssignment))
            .map(o -> policyAssignment)
            .doOnError(WebClientResponseException.class, this::handleWebClientResponseException)
            .onErrorResume(WebClientResponseException.class, exception ->
                Mono.error(new PolicyAssignmentException(policyAssignment, "Failed to assign policy", exception)))
            .onErrorStop();
    }

    public Mono<PolicyAssignment> assignApprovalTypeLevels(PolicyAssignment policyAssignment) {
        if (CollectionUtils.isEmpty(policyAssignment.getApprovalTypeAssignments())) {
            return Mono.just(policyAssignment);
        }
        return approvalTypeAssignmentsApi
            .postBulk(approvalMapper.mapApprovalTypeAssignment(policyAssignment))
            .map(o -> policyAssignment)
            .doOnError(WebClientResponseException.class, this::handleWebClientResponseException)
            .onErrorResume(WebClientResponseException.class, exception ->
                Mono.error(
                    new PolicyAssignmentException(policyAssignment, "Failed to assign approval type", exception)))
            .onErrorStop();
    }

    private void handleWebClientResponseException(WebClientResponseException webclientResponseException) {
        Objects.requireNonNull(webclientResponseException);
        Optional<HttpRequest> responseExceptionRequest = Optional.ofNullable(webclientResponseException.getRequest());
        log.error("Bad Request: \n[{}]: {}\nResponse: {}",
            responseExceptionRequest.map(HttpRequest::getMethod).orElse(null),
            responseExceptionRequest.map(HttpRequest::getURI).orElse(null),
            webclientResponseException.getResponseBodyAsString());
    }

}
