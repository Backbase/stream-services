package com.backbase.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.backbase.dbs.approval.api.management.service.v1.model.PolicyExpressionOperator;
import com.backbase.dbs.approval.api.management.service.v1.model.PolicyExpressionOperatorType;
import com.backbase.dbs.approval.api.management.service.v1.model.PolicyExpressionUnit;
import com.backbase.dbs.approval.api.management.service.v1.model.PolicyLocalScope;
import com.backbase.dbs.approval.api.management.service.v1.model.PolicySystemScope;
import com.backbase.dbs.approval.api.management.service.v1.model.PostPolicyServiceRequest;
import com.backbase.dbs.approval.api.management.service.v1.model.PostPolicyServiceResponse;
import com.backbase.dbs.approval.api.service.v2.ApprovalTypeAssignmentsApi;
import com.backbase.dbs.approval.api.service.v2.ApprovalTypesApi;
import com.backbase.dbs.approval.api.service.v2.PoliciesApi;
import com.backbase.dbs.approval.api.service.v2.PolicyAssignmentsApi;
import com.backbase.dbs.approval.api.service.v2.model.PolicyServiceApiResponseDto;
import com.backbase.dbs.approval.api.service.v2.model.PostPolicyServiceApiResponse;
import com.backbase.stream.approval.model.Policy;
import com.backbase.stream.approval.model.PolicyExpression;
import com.backbase.stream.approval.model.PolicyExpression.OperatorEnum;
import com.backbase.stream.approval.model.PolicyItem;
import com.backbase.stream.exceptions.PolicyException;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.service.AccessGroupService;
import com.backbase.stream.service.ApprovalsIntegrationService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ApprovalsIntegrationServiceExpressionPolicyTest {

    private static final String POLICY_NAME = "Level A or Level B, then Level C";
    private static final String CREATED_POLICY_ID = "created-policy-id";
    private static final String SERVICE_AGREEMENT_EXTERNAL_ID = "example-corporate-sa";
    private static final String SERVICE_AGREEMENT_INTERNAL_ID = "service-agreement-internal-id";

    @Mock
    private ApprovalTypesApi approvalTypesApi;
    @Mock
    private ApprovalTypeAssignmentsApi approvalTypeAssignmentsApi;
    @Mock
    private PoliciesApi policiesApi;
    @Mock
    private PolicyAssignmentsApi policyAssignmentsApi;
    @Mock
    private com.backbase.dbs.approval.api.management.service.v1.PoliciesApi managementPoliciesApi;
    @Mock
    private AccessGroupService accessGroupService;

    private ApprovalsIntegrationService approvalsIntegrationService;

    @BeforeEach
    void setUp() {
        approvalsIntegrationService = new ApprovalsIntegrationService(approvalTypesApi, approvalTypeAssignmentsApi,
            policiesApi, policyAssignmentsApi, accessGroupService, managementPoliciesApi);
    }

    @Test
    void shouldCreateLocalExpressionPolicyThroughManagementApi() {
        Policy policy = sequentialPolicy().scope("LOCAL").serviceAgreementId(SERVICE_AGREEMENT_EXTERNAL_ID);
        when(accessGroupService.getServiceAgreementByExternalId(SERVICE_AGREEMENT_EXTERNAL_ID))
            .thenReturn(Mono.just(new ServiceAgreement().internalId(SERVICE_AGREEMENT_INTERNAL_ID)));
        PostPolicyServiceRequest expectedRequest = expectedRequest()
            .scope(new PolicyLocalScope().serviceAgreementId(SERVICE_AGREEMENT_INTERNAL_ID));
        when(managementPoliciesApi.postPolicy(expectedRequest))
            .thenReturn(Mono.just(new PostPolicyServiceResponse().id(CREATED_POLICY_ID)));

        StepVerifier.create(approvalsIntegrationService.createPolicy(policy))
            .assertNext(created -> assertThat(created.getInternalId()).isEqualTo(CREATED_POLICY_ID))
            .verifyComplete();

        verify(managementPoliciesApi).postPolicy(expectedRequest);
        verifyNoInteractions(policiesApi);
    }

    @Test
    void shouldCreateSystemExpressionPolicyThroughManagementApi() {
        Policy policy = sequentialPolicy().scope("SYSTEM");
        PostPolicyServiceRequest expectedRequest = expectedRequest().scope(new PolicySystemScope());
        when(managementPoliciesApi.postPolicy(expectedRequest))
            .thenReturn(Mono.just(new PostPolicyServiceResponse().id(CREATED_POLICY_ID)));

        StepVerifier.create(approvalsIntegrationService.createPolicy(policy))
            .assertNext(created -> assertThat(created.getInternalId()).isEqualTo(CREATED_POLICY_ID))
            .verifyComplete();

        verifyNoInteractions(policiesApi, accessGroupService);
    }

    @Test
    void shouldKeepUsingServiceApiV2ForPoliciesWithoutExpression() {
        Policy zeroApprovalPolicy = new Policy().name("Zero Approval policy").scope("SYSTEM");
        when(policiesApi.postScopedPolicy(any())).thenReturn(Mono.just(new PostPolicyServiceApiResponse()
            .policy(new PolicyServiceApiResponseDto().id(CREATED_POLICY_ID).name("Zero Approval policy"))));

        StepVerifier.create(approvalsIntegrationService.createPolicy(zeroApprovalPolicy))
            .assertNext(created -> assertThat(created.getInternalId()).isEqualTo(CREATED_POLICY_ID))
            .verifyComplete();

        verifyNoInteractions(managementPoliciesApi);
    }

    @Test
    void shouldIgnoreItemsWhenPolicyAlsoDefinesExpression() {
        Policy policy = sequentialPolicy().scope("SYSTEM")
            .items(List.of(new PolicyItem().approvalTypeId("type-a").numberOfApprovals(BigDecimal.ONE)));
        when(managementPoliciesApi.postPolicy(expectedRequest().scope(new PolicySystemScope())))
            .thenReturn(Mono.just(new PostPolicyServiceResponse().id(CREATED_POLICY_ID)));

        StepVerifier.create(approvalsIntegrationService.createPolicy(policy))
            .expectNextCount(1)
            .verifyComplete();

        verifyNoInteractions(policiesApi);
    }

    @Test
    void shouldFailWithoutCallingApiWhenApprovalTypeOfUnitIsUnresolved() {
        Policy policy = sequentialPolicy().scope("SYSTEM");
        policy.getExpression().getOperands().get(1).approvalTypeId(null).approvalTypeName("Level Z");

        StepVerifier.create(approvalsIntegrationService.createPolicy(policy))
            .expectErrorSatisfies(error -> assertThat(error)
                .isInstanceOf(PolicyException.class)
                .hasMessageContaining("'Level Z'"))
            .verify();

        verifyNoInteractions(managementPoliciesApi, policiesApi);
    }

    @Test
    void shouldFailWithoutCallingApiWhenNodeHasBothOperatorAndApprovalType() {
        Policy policy = sequentialPolicy().scope("SYSTEM");
        policy.getExpression().getOperands().get(0).approvalTypeName("Level A");

        assertRejectedWithoutApiCall(policy, "has both 'operator' and an approval type");
    }

    @Test
    void shouldFailWithoutCallingApiWhenNodeHasNeitherOperatorNorApprovalType() {
        Policy policy = sequentialPolicy().scope("SYSTEM");
        policy.getExpression().getOperands().get(0).operator(null);

        assertRejectedWithoutApiCall(policy, "has neither 'operator' nor an approval type");
    }

    @Test
    void shouldFailWithoutCallingApiWhenUnitHasOperands() {
        Policy policy = sequentialPolicy().scope("SYSTEM");
        policy.getExpression().getOperands().get(1).operands(List.of(unit("type-a"), unit("type-b")));

        assertRejectedWithoutApiCall(policy, "cannot have 'operands'");
    }

    @Test
    void shouldFailWithoutCallingApiWhenUnitMissesCount() {
        Policy policy = sequentialPolicy().scope("SYSTEM");
        policy.getExpression().getOperands().get(1).count(null);

        assertRejectedWithoutApiCall(policy, "is missing 'count'");
    }

    @Test
    void shouldWrapManagementApiErrorInPolicyException() {
        Policy policy = sequentialPolicy().scope("SYSTEM");
        WebClientResponseException badRequest = new WebClientResponseException(
            HttpStatus.BAD_REQUEST.value(), "Bad Request", null,
            "{\"key\":\"approval.api.error.sequential.disabled\"}".getBytes(), null);
        when(managementPoliciesApi.postPolicy(any())).thenReturn(Mono.error(badRequest));

        StepVerifier.create(approvalsIntegrationService.createPolicy(policy))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(PolicyException.class).hasCause(badRequest);
                assertThat(((PolicyException) error).getHttpResponse()).contains("sequential");
            })
            .verify();
    }

    @Test
    void shouldFailExpressionPolicyWhenManagementClientIsNotConfigured() {
        ApprovalsIntegrationService withoutManagementClient = new ApprovalsIntegrationService(approvalTypesApi,
            approvalTypeAssignmentsApi, policiesApi, policyAssignmentsApi, accessGroupService);

        StepVerifier.create(withoutManagementClient.createPolicy(sequentialPolicy().scope("SYSTEM")))
            .expectErrorSatisfies(error -> assertThat(error)
                .isInstanceOf(PolicyException.class)
                .hasMessageContaining("Approval Management Service API"))
            .verify();

        verifyNoInteractions(policiesApi);
    }

    private void assertRejectedWithoutApiCall(Policy policy, String expectedMessage) {
        StepVerifier.create(approvalsIntegrationService.createPolicy(policy))
            .expectErrorSatisfies(error -> assertThat(error)
                .isInstanceOf(PolicyException.class)
                .hasMessageContaining(expectedMessage))
            .verify();

        verifyNoInteractions(managementPoliciesApi, policiesApi);
    }

    private static Policy sequentialPolicy() {
        return new Policy()
            .name(POLICY_NAME)
            .description("sequential")
            .expression(new PolicyExpression()
                .operator(OperatorEnum.THEN)
                .operands(List.of(
                    new PolicyExpression()
                        .operator(OperatorEnum.OR)
                        .operands(List.of(unit("type-a"), unit("type-b"))),
                    unit("type-c"))));
    }

    private static PolicyExpression unit(String approvalTypeId) {
        return new PolicyExpression().approvalTypeId(approvalTypeId).count(1);
    }

    private static PostPolicyServiceRequest expectedRequest() {
        return new PostPolicyServiceRequest()
            .name(POLICY_NAME)
            .description("sequential")
            .expression(new PolicyExpressionOperator()
                .operatorType(PolicyExpressionOperatorType.THEN)
                .operands(List.of(
                    new PolicyExpressionOperator()
                        .operatorType(PolicyExpressionOperatorType.OR)
                        .operands(List.of(expectedUnit("type-a"), expectedUnit("type-b"))),
                    expectedUnit("type-c"))));
    }

    private static PolicyExpressionUnit expectedUnit(String approvalTypeId) {
        return new PolicyExpressionUnit().approvalTypeId(approvalTypeId).count(1);
    }
}
