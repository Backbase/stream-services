package com.backbase.stream.service;

import com.backbase.dbs.approval.api.service.v2.ApprovalTypeAssignmentsApi;
import com.backbase.dbs.approval.api.service.v2.ApprovalTypesApi;
import com.backbase.dbs.approval.api.service.v2.PoliciesApi;
import com.backbase.dbs.approval.api.service.v2.PolicyAssignmentsApi;
import com.backbase.dbs.approval.api.service.v2.model.PostApprovalTypeResponse;
import com.backbase.dbs.approval.api.service.v2.model.PostPolicyServiceApiResponse;
import com.backbase.stream.approval.model.ApprovalType;
import com.backbase.stream.approval.model.Policy;
import com.backbase.stream.approval.model.PolicyAssignment;
import com.backbase.stream.exceptions.ApprovalTypeException;
import com.backbase.stream.exceptions.PolicyAssignmentException;
import com.backbase.stream.exceptions.PolicyException;
import com.backbase.stream.legalentity.model.ServiceAgreement;
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
public class ApprovalsIntegrationService {

    private final ApprovalMapper approvalMapper = Mappers.getMapper(ApprovalMapper.class);
    private final PolicyMapper policyMapper = Mappers.getMapper(PolicyMapper.class);

    private final ApprovalTypesApi approvalTypesApi;
    private final ApprovalTypeAssignmentsApi approvalTypeAssignmentsApi;
    private final PoliciesApi policiesApi;
    private final PolicyAssignmentsApi policyAssignmentsApi;
    private final AccessGroupService accessGroupService;

    public Mono<ApprovalType> createApprovalType(ApprovalType approvalType) {
        return Mono.just(approvalType)
            .flatMap(inputApprovalType -> {
                if (inputApprovalType.getServiceAgreementId() == null) {
                    return approvalTypesApi.postScopedApprovalType(
                            approvalMapper.mapScopedApprovalType(inputApprovalType))
                        .map(createdApprovalType -> mapCreatedApprovalType(inputApprovalType, createdApprovalType));
                } else {
                    return accessGroupService.getServiceAgreementByExternalId(inputApprovalType.getServiceAgreementId())
                        .map(ServiceAgreement::getInternalId)
                        .defaultIfEmpty(inputApprovalType.getServiceAgreementId())
                        .flatMap(internalId -> {
                            inputApprovalType.setServiceAgreementId(internalId);
                            return approvalTypesApi.postScopedApprovalType(
                                    approvalMapper.mapScopedApprovalType(inputApprovalType))
                                .map(createdApprovalType -> mapCreatedApprovalType(inputApprovalType,
                                    createdApprovalType));
                        });
                }
            })
            .doOnError(WebClientResponseException.class, this::handleWebClientResponseException)
            .onErrorResume(WebClientResponseException.class, exception ->
                Mono.error(new ApprovalTypeException(approvalType, "Failed to create Approval Type Level", exception)))
            .onErrorStop();
    }

    private ApprovalType mapCreatedApprovalType(ApprovalType inputApprovalType,
        PostApprovalTypeResponse createdApprovalTypeResponse) {
        String id = createdApprovalTypeResponse.getApprovalType().getId();
        String name = createdApprovalTypeResponse.getApprovalType().getName();
        log.info("Created approval type: {} with identifier: [{}].", name, id);
        inputApprovalType.setInternalId(id);
        return inputApprovalType;
    }

    public Mono<Policy> createPolicy(Policy policy) {
        return Mono.just(policy)
            .flatMap(inputPolicy -> {
                if (inputPolicy.getServiceAgreementId() == null) {
                    // Directly proceed with system policy creation
                    return policiesApi.postScopedPolicy(policyMapper.mapScopedPolicy(inputPolicy))
                        .map(createdPolicyResponse -> mapCreatedPolicy(inputPolicy, createdPolicyResponse));
                } else {
                    // Handle local policy creation
                    return accessGroupService.getServiceAgreementByExternalId(inputPolicy.getServiceAgreementId())
                        .map(ServiceAgreement::getInternalId)
                        .defaultIfEmpty(inputPolicy.getServiceAgreementId())
                        .flatMap(internalId -> {
                            inputPolicy.setServiceAgreementId(internalId);
                            return policiesApi.postScopedPolicy(policyMapper.mapScopedPolicy(inputPolicy))
                                .map(createdPolicyResponse -> mapCreatedPolicy(inputPolicy, createdPolicyResponse));
                        });
                }
            })
            .doOnError(WebClientResponseException.class, this::handleWebClientResponseException)
            .onErrorResume(WebClientResponseException.class, exception ->
                Mono.error(new PolicyException(policy, "Failed to create Policy", exception)))
            .onErrorStop();
    }

    private Policy mapCreatedPolicy(Policy inputPolicy, PostPolicyServiceApiResponse createdPolicyResponse) {
        String id = createdPolicyResponse.getPolicy().getId();
        String name = createdPolicyResponse.getPolicy().getName();
        log.info("Created approval policy: '{}' with identifier: [{}].", name, id);
        inputPolicy.setInternalId(id);
        return inputPolicy;
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
