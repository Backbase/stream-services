package com.backbase.stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.dbs.approval.api.service.v2.ApprovalTypesApi;
import com.backbase.dbs.approval.api.service.v2.PoliciesApi;
import com.backbase.dbs.approval.api.service.v2.model.ApprovalTypeDto;
import com.backbase.dbs.approval.api.service.v2.model.ApprovalTypeScope;
import com.backbase.dbs.approval.api.service.v2.model.PolicyDetailsDto;
import com.backbase.dbs.approval.api.service.v2.model.PolicyScope;
import com.backbase.dbs.approval.api.service.v2.model.PolicyServiceApiResponseDto;
import com.backbase.dbs.approval.api.service.v2.model.PostApprovalTypeRequest;
import com.backbase.dbs.approval.api.service.v2.model.PostApprovalTypeResponse;
import com.backbase.dbs.approval.api.service.v2.model.PostPolicyRequest;
import com.backbase.dbs.approval.api.service.v2.model.PostPolicyResponse;
import com.backbase.dbs.approval.api.service.v2.model.PostPolicyServiceApiRequest;
import com.backbase.dbs.approval.api.service.v2.model.PostPolicyServiceApiResponse;
import com.backbase.dbs.approval.api.service.v2.model.PostScopedApprovalTypeRequest;
import com.backbase.stream.approval.model.ApprovalType;
import com.backbase.stream.approval.model.Policy;
import com.backbase.stream.exceptions.ApprovalTypeException;
import com.backbase.stream.exceptions.PolicyException;
import com.backbase.stream.service.ApprovalsIntegrationService;
import java.math.BigDecimal;
import java.util.UUID;
import org.apache.commons.lang3.ObjectUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ApprovalsIntegrationServiceTest {

    @Mock
    private PoliciesApi policiesApi;

    @Mock
    private ApprovalTypesApi approvalTypesApi;

    @InjectMocks
    private ApprovalsIntegrationService approvalsIntegrationService;

    private static final String TEST_APPROVAL_TYPE_NAME = "TestApprovalType";
    private static final String TEST_POLICY_NAME = "High-Value Payment Policy";
    private static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
    private static final String INTERNAL_SERVER_ERROR_500 = "500 Internal Server Error";
    private static final String BAD_REQUEST = "Bad Request";
    private static final String BAD_REQUEST_400 = "400 Bad Request";

    @Test
    void shouldCreateApprovalType() {
        final ApprovalType inputApprovalType = new ApprovalType()
            .name(TEST_APPROVAL_TYPE_NAME)
            .rank(new BigDecimal(1));

        final PostApprovalTypeRequest mappedApprovalTypeItem = new PostApprovalTypeRequest()
            .name(TEST_APPROVAL_TYPE_NAME)
            .rank(1);

        final PostApprovalTypeResponse apiResponse = new PostApprovalTypeResponse()
            .approvalType(new ApprovalTypeDto().name(TEST_APPROVAL_TYPE_NAME));

        when(approvalTypesApi.postApprovalType(mappedApprovalTypeItem)).thenReturn(Mono.just(apiResponse));

        final Mono<ApprovalType> resultMono = approvalsIntegrationService.createApprovalType(inputApprovalType);

        StepVerifier.create(resultMono)
            .expectNextMatches(createdApprovalType -> {
                assertThat(createdApprovalType.getName()).isEqualTo(TEST_APPROVAL_TYPE_NAME);
                assertThat(ObjectUtils.isEmpty(createdApprovalType.getScope())).isTrue();
                return true;
            })
            .verifyComplete();

        verify(approvalTypesApi, times(1)).postApprovalType(mappedApprovalTypeItem);
        verify(approvalTypesApi, never()).postScopedApprovalType(any());
    }

    @Test
    void shouldCreateScopedApprovalType() {
        final ApprovalType inputApprovalType = new ApprovalType()
            .name(TEST_APPROVAL_TYPE_NAME)
            .rank(new BigDecimal(2))
            .scope(ApprovalTypeScope.LOCAL.getValue());

        final PostScopedApprovalTypeRequest mappedApprovalTypeItem = new PostScopedApprovalTypeRequest()
            .name(TEST_APPROVAL_TYPE_NAME)
            .scope(ApprovalTypeScope.LOCAL)
            .rank(2);

        final PostApprovalTypeResponse apiResponse = new PostApprovalTypeResponse()
            .approvalType(new ApprovalTypeDto().name(TEST_APPROVAL_TYPE_NAME).scope(ApprovalTypeScope.LOCAL));

        when(approvalTypesApi.postScopedApprovalType(mappedApprovalTypeItem)).thenReturn(Mono.just(apiResponse));

        final Mono<ApprovalType> resultMono = approvalsIntegrationService.createApprovalType(inputApprovalType);

        StepVerifier.create(resultMono)
            .expectNextMatches(createdApprovalType -> {
                assertThat(createdApprovalType.getName()).isEqualTo(TEST_APPROVAL_TYPE_NAME);
                assertThat(createdApprovalType.getScope()).isEqualTo(ApprovalTypeScope.LOCAL.getValue());
                return true;
            })
            .verifyComplete();

        verify(approvalTypesApi, times(1)).postScopedApprovalType(mappedApprovalTypeItem);
        verify(approvalTypesApi, never()).postApprovalType(any());
    }

    @Test
    void shouldNotCreateApprovalTypeAndThrowBadRequest() {
        final ApprovalType inputApprovalType = new ApprovalType()
            .name(TEST_APPROVAL_TYPE_NAME);

        final PostApprovalTypeRequest mappedApprovalTypeItem = new PostApprovalTypeRequest()
            .name(TEST_APPROVAL_TYPE_NAME);

        final WebClientResponseException webClientException = new WebClientResponseException(
            HttpStatus.BAD_REQUEST.value(),
            BAD_REQUEST,
            null, null, null);

        when(approvalTypesApi.postApprovalType(mappedApprovalTypeItem)).thenReturn(Mono.error(webClientException));

        final Mono<ApprovalType> resultMono = approvalsIntegrationService.createApprovalType(inputApprovalType);

        StepVerifier.create(resultMono)
            .expectErrorMatches(throwable ->
                throwable instanceof ApprovalTypeException &&
                    throwable.getMessage().contains(BAD_REQUEST_400) &&
                    throwable.getCause() instanceof WebClientResponseException &&
                    ((WebClientResponseException) throwable.getCause()).getStatusCode() == HttpStatus.BAD_REQUEST)
            .verify();

        verify(approvalTypesApi, times(1)).postApprovalType(mappedApprovalTypeItem);
        verify(approvalTypesApi, never()).postScopedApprovalType(any());
    }

    @Test
    void shouldNotCreateScopedApprovalTypeAndThrowInternalServerError() {
        final ApprovalType inputApprovalType = new ApprovalType()
            .name(TEST_APPROVAL_TYPE_NAME)
            .scope(ApprovalTypeScope.LOCAL.getValue());

        final PostScopedApprovalTypeRequest mappedApprovalTypeItem = new
            PostScopedApprovalTypeRequest().name(TEST_APPROVAL_TYPE_NAME).scope(ApprovalTypeScope.LOCAL);

        final WebClientResponseException webClientException = new WebClientResponseException(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            INTERNAL_SERVER_ERROR,
            null, null, null);

        when(approvalTypesApi.postScopedApprovalType(mappedApprovalTypeItem)).thenReturn(
            Mono.error(webClientException));

        final Mono<ApprovalType> resultMono = approvalsIntegrationService.createApprovalType(inputApprovalType);

        StepVerifier.create(resultMono)
            .expectErrorMatches(throwable ->
                throwable instanceof ApprovalTypeException &&
                    throwable.getMessage().contains(INTERNAL_SERVER_ERROR_500) &&
                    throwable.getCause() instanceof WebClientResponseException &&
                    ((WebClientResponseException) throwable.getCause()).getStatusCode()
                        == HttpStatus.INTERNAL_SERVER_ERROR)
            .verify();

        verify(approvalTypesApi, times(1)).postScopedApprovalType(mappedApprovalTypeItem);
        verify(approvalTypesApi, never()).postApprovalType(any());
    }

    @Test
    void shouldCreatePolicy() {
        final Policy inputPolicy = new Policy()
            .name(TEST_POLICY_NAME);

        final PostPolicyRequest expectedMappedRequest = new PostPolicyRequest().name(TEST_POLICY_NAME);

        final PostPolicyResponse apiResponse = new PostPolicyResponse()
            .policy(new PolicyDetailsDto().name(TEST_POLICY_NAME));

        when(policiesApi.postPolicy(expectedMappedRequest)).thenReturn(Mono.just(apiResponse));

        final Mono<Policy> resultMono = approvalsIntegrationService.createPolicy(inputPolicy);

        StepVerifier.create(resultMono)
            .expectNextMatches(createdPolicy -> {
                assertThat(createdPolicy.getName()).isEqualTo(TEST_POLICY_NAME);
                assertThat(ObjectUtils.isEmpty(createdPolicy.getScope())).isTrue();
                return true;
            })
            .verifyComplete();

        verify(policiesApi, times(1)).postPolicy(expectedMappedRequest);
        verify(policiesApi, never()).postScopedPolicy(any());
    }

    @Test
    void shouldCreateScopedPolicy() {
        final String serviceAgreementId = UUID.randomUUID().toString();

        final Policy inputPolicy = new Policy()
            .name(TEST_POLICY_NAME)
            .scope(ApprovalTypeScope.LOCAL.getValue())
            .serviceAgreementId(serviceAgreementId);

        final PostPolicyServiceApiRequest expectedMappedRequest = new PostPolicyServiceApiRequest()
            .name(TEST_POLICY_NAME)
            .scope(PolicyScope.LOCAL)
            .serviceAgreementId(serviceAgreementId);

        final PostPolicyServiceApiResponse apiResponse = new PostPolicyServiceApiResponse()
            .policy(new PolicyServiceApiResponseDto().name(TEST_POLICY_NAME).scope(PolicyScope.LOCAL));

        when(policiesApi.postScopedPolicy(expectedMappedRequest)).thenReturn(Mono.just(apiResponse));

        final Mono<Policy> resultMono = approvalsIntegrationService.createPolicy(inputPolicy);

        StepVerifier.create(resultMono)
            .expectNextMatches(createdPolicy -> {
                assertThat(createdPolicy.getName()).isEqualTo(TEST_POLICY_NAME);
                assertThat(createdPolicy.getScope()).isEqualTo(ApprovalTypeScope.LOCAL.getValue());
                return true;
            })
            .verifyComplete();

        verify(policiesApi, times(1)).postScopedPolicy(expectedMappedRequest);
        verify(policiesApi, never()).postPolicy(any());
    }

    @Test
    void shouldNotCreatePolicyAndThrowBadRequest() {
        final Policy inputPolicy = new Policy().name(TEST_POLICY_NAME);
        final PostPolicyRequest expectedMappedRequest = new PostPolicyRequest().name(TEST_POLICY_NAME);

        final WebClientResponseException webClientException = new WebClientResponseException(
            HttpStatus.BAD_REQUEST.value(),
            BAD_REQUEST,
            null, null, null);

        when(policiesApi.postPolicy(expectedMappedRequest)).thenReturn(Mono.error(webClientException));

        final Mono<Policy> resultMono = approvalsIntegrationService.createPolicy(inputPolicy);

        StepVerifier.create(resultMono)
            .expectErrorMatches(throwable ->
                throwable instanceof PolicyException &&
                    throwable.getMessage().contains(BAD_REQUEST_400) &&
                    throwable.getCause() instanceof WebClientResponseException &&
                    ((WebClientResponseException) throwable.getCause()).getStatusCode() == HttpStatus.BAD_REQUEST)
            .verify();

        verify(policiesApi, times(1)).postPolicy(expectedMappedRequest);
        verify(policiesApi, never()).postScopedPolicy(any());
    }

    @Test
    void shouldNotCreateScopedPolicyAndThrowInternalServerError() {
        final String serviceAgreementId = UUID.randomUUID().toString();

        final Policy inputPolicy = new Policy()
            .name(TEST_POLICY_NAME)
            .scope(ApprovalTypeScope.LOCAL.getValue())
            .serviceAgreementId(serviceAgreementId);

        final PostPolicyServiceApiRequest expectedMappedRequest = new PostPolicyServiceApiRequest()
            .name(TEST_POLICY_NAME)
            .scope(PolicyScope.LOCAL)
            .serviceAgreementId(serviceAgreementId);

        final WebClientResponseException webClientException = new WebClientResponseException(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            INTERNAL_SERVER_ERROR,
            null, null, null);

        when(policiesApi.postScopedPolicy(expectedMappedRequest)).thenReturn(Mono.error(webClientException));

        final Mono<Policy> resultMono = approvalsIntegrationService.createPolicy(inputPolicy);

        StepVerifier.create(resultMono)
            .expectErrorMatches(throwable ->
                throwable instanceof PolicyException &&
                    throwable.getMessage().contains(INTERNAL_SERVER_ERROR_500) &&
                    throwable.getCause() instanceof WebClientResponseException &&
                    ((WebClientResponseException) throwable.getCause()).getStatusCode()
                        == HttpStatus.INTERNAL_SERVER_ERROR)
            .verify();

        verify(policiesApi, times(1)).postScopedPolicy(expectedMappedRequest);
        verify(policiesApi, never()).postPolicy(any());
    }
}
