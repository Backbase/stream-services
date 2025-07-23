package com.backbase.stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.dbs.approval.api.service.v2.ApprovalTypesApi;
import com.backbase.dbs.approval.api.service.v2.PoliciesApi;
import com.backbase.dbs.approval.api.service.v2.model.ApprovalTypeDto;
import com.backbase.dbs.approval.api.service.v2.model.ApprovalTypeScope;
import com.backbase.dbs.approval.api.service.v2.model.PolicyScope;
import com.backbase.dbs.approval.api.service.v2.model.PolicyServiceApiResponseDto;
import com.backbase.dbs.approval.api.service.v2.model.PostApprovalTypeResponse;
import com.backbase.dbs.approval.api.service.v2.model.PostPolicyServiceApiRequest;
import com.backbase.dbs.approval.api.service.v2.model.PostPolicyServiceApiResponse;
import com.backbase.dbs.approval.api.service.v2.model.PostScopedApprovalTypeRequest;
import com.backbase.stream.approval.model.ApprovalType;
import com.backbase.stream.approval.model.Policy;
import com.backbase.stream.exceptions.ApprovalTypeException;
import com.backbase.stream.exceptions.PolicyException;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.service.AccessGroupService;
import com.backbase.stream.service.ApprovalsIntegrationService;
import java.math.BigDecimal;
import java.util.UUID;
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

    @Mock
    private AccessGroupService accessGroupService;

    @InjectMocks
    private ApprovalsIntegrationService approvalsIntegrationService;

    private static final String TEST_APPROVAL_TYPE_NAME = "TestApprovalType";
    private static final String TEST_POLICY_NAME = "High-Value Payment Policy";
    private static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
    private static final String INTERNAL_SERVER_ERROR_500 = "500 Internal Server Error";
    private static final String BAD_REQUEST = "Bad Request";
    private static final String BAD_REQUEST_400 = "400 Bad Request";

    @Test
    void shouldCreateSystemApprovalType() {
        final ApprovalType inputApprovalType = new ApprovalType()
            .name(TEST_APPROVAL_TYPE_NAME)
            .scope(ApprovalTypeScope.SYSTEM.getValue())
            .rank(new BigDecimal(1));

        final PostScopedApprovalTypeRequest mappedApprovalTypeItem = new PostScopedApprovalTypeRequest()
            .name(TEST_APPROVAL_TYPE_NAME)
            .scope(ApprovalTypeScope.SYSTEM)
            .rank(1);

        final PostApprovalTypeResponse apiResponse = new PostApprovalTypeResponse()
            .approvalType(new ApprovalTypeDto()
                .name(TEST_APPROVAL_TYPE_NAME));

        when(approvalTypesApi.postScopedApprovalType(mappedApprovalTypeItem)).thenReturn(Mono.just(apiResponse));

        final Mono<ApprovalType> resultMono = approvalsIntegrationService.createApprovalType(inputApprovalType);

        StepVerifier.create(resultMono)
            .expectNextMatches(createdApprovalType -> {
                assertThat(createdApprovalType.getName()).isEqualTo(TEST_APPROVAL_TYPE_NAME);
                assertThat(createdApprovalType.getScope()).isEqualTo(ApprovalTypeScope.SYSTEM.getValue());
                return true;
            })
            .verifyComplete();

        verify(approvalTypesApi, times(1)).postScopedApprovalType(mappedApprovalTypeItem);
    }

    @Test
    void shouldCreateLocalApprovalType() {
        final String serviceAgreementInternalId = UUID.randomUUID().toString();
        final String serviceAgreementExternalId = "External identifier";

        final ApprovalType inputApprovalType = new ApprovalType()
            .name(TEST_APPROVAL_TYPE_NAME)
            .rank(new BigDecimal(2))
            .scope(ApprovalTypeScope.LOCAL.getValue())
            .serviceAgreementId(serviceAgreementExternalId);

        final PostScopedApprovalTypeRequest mappedApprovalTypeItem = new PostScopedApprovalTypeRequest()
            .name(TEST_APPROVAL_TYPE_NAME)
            .scope(ApprovalTypeScope.LOCAL)
            .creatorServiceAgreementId(serviceAgreementInternalId)
            .rank(2);

        final PostApprovalTypeResponse apiResponse = new PostApprovalTypeResponse()
            .approvalType(new ApprovalTypeDto()
                .name(TEST_APPROVAL_TYPE_NAME)
                .scope(ApprovalTypeScope.LOCAL)
                .creatorServiceAgreementId(serviceAgreementInternalId));

        final ServiceAgreement serviceAgreement = new ServiceAgreement()
            .internalId(serviceAgreementInternalId)
            .externalId(serviceAgreementExternalId);

        when(accessGroupService.getServiceAgreementByExternalId(anyString()))
            .thenReturn(Mono.just(serviceAgreement));
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
    }

    @Test
    void shouldNotCreateSystemApprovalTypeAndThrowBadRequest() {
        final ApprovalType inputApprovalType = new ApprovalType()
            .name(TEST_APPROVAL_TYPE_NAME);

        final PostScopedApprovalTypeRequest mappedApprovalTypeItem = new PostScopedApprovalTypeRequest()
            .name(TEST_APPROVAL_TYPE_NAME);

        final WebClientResponseException webClientException = new WebClientResponseException(
            HttpStatus.BAD_REQUEST.value(),
            BAD_REQUEST,
            null, null, null);

        when(approvalTypesApi.postScopedApprovalType(mappedApprovalTypeItem)).thenReturn(
            Mono.error(webClientException));

        final Mono<ApprovalType> resultMono = approvalsIntegrationService.createApprovalType(inputApprovalType);

        StepVerifier.create(resultMono)
            .expectErrorMatches(throwable ->
                throwable instanceof ApprovalTypeException &&
                    throwable.getMessage().contains(BAD_REQUEST_400) &&
                    throwable.getCause() instanceof WebClientResponseException &&
                    ((WebClientResponseException) throwable.getCause()).getStatusCode() == HttpStatus.BAD_REQUEST)
            .verify();

        verify(approvalTypesApi, times(1)).postScopedApprovalType(mappedApprovalTypeItem);
    }

    @Test
    void shouldNotCreateLocalApprovalTypeAndThrowInternalServerError() {
        final String serviceAgreementInternalId = UUID.randomUUID().toString();
        final String serviceAgreementExternalId = "External identifier";

        final ApprovalType inputApprovalType = new ApprovalType()
            .name(TEST_APPROVAL_TYPE_NAME)
            .scope(ApprovalTypeScope.LOCAL.getValue())
            .serviceAgreementId(serviceAgreementExternalId);

        final PostScopedApprovalTypeRequest mappedApprovalTypeItem = new PostScopedApprovalTypeRequest()
            .name(TEST_APPROVAL_TYPE_NAME)
            .scope(ApprovalTypeScope.LOCAL)
            .creatorServiceAgreementId(serviceAgreementInternalId);

        final WebClientResponseException webClientException = new WebClientResponseException(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            INTERNAL_SERVER_ERROR,
            null, null, null);

        final ServiceAgreement serviceAgreement = new ServiceAgreement()
            .internalId(serviceAgreementInternalId)
            .externalId(serviceAgreementExternalId);

        when(accessGroupService.getServiceAgreementByExternalId(anyString()))
            .thenReturn(Mono.just(serviceAgreement));
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
    }

    @Test
    void shouldCreateSystemPolicy() {
        final Policy inputPolicy = new Policy()
            .name(TEST_POLICY_NAME)
            .scope(ApprovalTypeScope.SYSTEM.getValue());

        final PostPolicyServiceApiRequest expectedMappedRequest = new PostPolicyServiceApiRequest()
            .name(TEST_POLICY_NAME)
            .scope(PolicyScope.SYSTEM);

        final PostPolicyServiceApiResponse apiResponse = new PostPolicyServiceApiResponse()
            .policy(new PolicyServiceApiResponseDto()
                .name(TEST_POLICY_NAME)
                .scope(PolicyScope.SYSTEM));

        when(policiesApi.postScopedPolicy(expectedMappedRequest)).thenReturn(Mono.just(apiResponse));

        final Mono<Policy> resultMono = approvalsIntegrationService.createPolicy(inputPolicy);

        StepVerifier.create(resultMono)
            .expectNextMatches(createdPolicy -> {
                assertThat(createdPolicy.getName()).isEqualTo(TEST_POLICY_NAME);
                assertThat(createdPolicy.getScope()).isEqualTo(ApprovalTypeScope.SYSTEM.getValue());
                return true;
            })
            .verifyComplete();

        verify(policiesApi, times(1)).postScopedPolicy(expectedMappedRequest);
    }

    @Test
    void shouldCreateLocalPolicy() {
        final String serviceAgreementInternalId = UUID.randomUUID().toString();
        final String serviceAgreementExternalId = "External identifier";

        final Policy inputPolicy = new Policy()
            .name(TEST_POLICY_NAME)
            .scope(ApprovalTypeScope.LOCAL.getValue())
            .serviceAgreementId(serviceAgreementExternalId);

        final ServiceAgreement serviceAgreement = new ServiceAgreement()
            .internalId(serviceAgreementInternalId)
            .externalId(serviceAgreementExternalId);

        when(accessGroupService.getServiceAgreementByExternalId(serviceAgreementExternalId))
            .thenReturn(Mono.just(serviceAgreement));

        final PostPolicyServiceApiRequest expectedMappedRequest = new PostPolicyServiceApiRequest()
            .name(TEST_POLICY_NAME)
            .scope(PolicyScope.LOCAL)
            .serviceAgreementId(serviceAgreementInternalId);

        final PostPolicyServiceApiResponse apiResponse = new PostPolicyServiceApiResponse()
            .policy(new PolicyServiceApiResponseDto()
                .name(TEST_POLICY_NAME)
                .scope(PolicyScope.LOCAL)
                .serviceAgreementId(serviceAgreementInternalId));

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
    }

    @Test
    void shouldNotCreateSystemPolicyAndThrowBadRequest() {
        final Policy inputPolicy = new Policy()
            .name(TEST_POLICY_NAME);

        final PostPolicyServiceApiRequest expectedMappedRequest = new PostPolicyServiceApiRequest()
            .name(TEST_POLICY_NAME);

        final WebClientResponseException webClientException = new WebClientResponseException(
            HttpStatus.BAD_REQUEST.value(),
            BAD_REQUEST,
            null, null, null);

        when(policiesApi.postScopedPolicy(expectedMappedRequest)).thenReturn(Mono.error(webClientException));

        final Mono<Policy> resultMono = approvalsIntegrationService.createPolicy(inputPolicy);

        StepVerifier.create(resultMono)
            .expectErrorMatches(throwable ->
                throwable instanceof PolicyException &&
                    throwable.getMessage().contains(BAD_REQUEST_400) &&
                    throwable.getCause() instanceof WebClientResponseException &&
                    ((WebClientResponseException) throwable.getCause()).getStatusCode() == HttpStatus.BAD_REQUEST)
            .verify();

        verify(policiesApi, times(1)).postScopedPolicy(expectedMappedRequest);
    }

    @Test
    void shouldNotCreateLocalPolicyAndThrowInternalServerError() {
        final String serviceAgreementInternalId = UUID.randomUUID().toString();
        final String serviceAgreementExternalId = "External identifier";

        final Policy inputPolicy = new Policy()
            .name(TEST_POLICY_NAME)
            .scope(ApprovalTypeScope.LOCAL.getValue())
            .serviceAgreementId(serviceAgreementExternalId);

        final ServiceAgreement serviceAgreement = new ServiceAgreement()
            .internalId(serviceAgreementInternalId)
            .externalId(serviceAgreementExternalId);

        when(accessGroupService.getServiceAgreementByExternalId(serviceAgreementExternalId))
            .thenReturn(Mono.just(serviceAgreement));

        final PostPolicyServiceApiRequest expectedMappedRequest = new PostPolicyServiceApiRequest()
            .name(TEST_POLICY_NAME)
            .scope(PolicyScope.LOCAL)
            .serviceAgreementId(serviceAgreementInternalId);

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
    }
}
