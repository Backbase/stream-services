package com.backbase.stream.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.backbase.dbs.approval.api.management.service.v1.model.PostPolicyServiceResponse;
import com.backbase.dbs.approval.api.service.v2.ApprovalTypeAssignmentsApi;
import com.backbase.dbs.approval.api.service.v2.ApprovalTypesApi;
import com.backbase.dbs.approval.api.service.v2.PoliciesApi;
import com.backbase.dbs.approval.api.service.v2.PolicyAssignmentsApi;
import com.backbase.stream.approval.model.Policy;
import com.backbase.stream.approval.model.PolicyExpression;
import com.backbase.stream.approval.model.PolicyExpression.OperatorEnum;
import com.backbase.stream.exceptions.PolicyException;
import com.backbase.stream.service.AccessGroupService;
import com.backbase.stream.service.ApprovalsIntegrationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ApprovalsServiceConfigurationTest {

    private final PoliciesApi policiesApi = mock(PoliciesApi.class);
    private final com.backbase.dbs.approval.api.management.service.v1.PoliciesApi managementPoliciesApi =
        mock(com.backbase.dbs.approval.api.management.service.v1.PoliciesApi.class);

    @Test
    void shouldCreateExpressionPoliciesWhenManagementClientIsAvailable() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("managementPoliciesApi", managementPoliciesApi);
        when(managementPoliciesApi.postPolicy(any()))
            .thenReturn(Mono.just(new PostPolicyServiceResponse().id("policy-id")));

        StepVerifier.create(integrationService(beanFactory).createPolicy(expressionPolicy()))
            .assertNext(created -> assertThat(created.getInternalId()).isEqualTo("policy-id"))
            .verifyComplete();
    }

    @Test
    void shouldStartWithoutManagementClientAndRejectOnlyExpressionPolicies() {
        ApprovalsIntegrationService service = integrationService(new DefaultListableBeanFactory());

        StepVerifier.create(service.createPolicy(expressionPolicy()))
            .expectErrorSatisfies(error -> assertThat(error)
                .isInstanceOf(PolicyException.class)
                .hasMessageContaining("Approval Management Service API"))
            .verify();

        verifyNoInteractions(policiesApi);
    }

    private ApprovalsIntegrationService integrationService(DefaultListableBeanFactory beanFactory) {
        return new ApprovalsServiceConfiguration().approvalIntegrationService(mock(ApprovalTypesApi.class),
            mock(ApprovalTypeAssignmentsApi.class), policiesApi, mock(PolicyAssignmentsApi.class),
            mock(AccessGroupService.class),
            beanFactory.getBeanProvider(com.backbase.dbs.approval.api.management.service.v1.PoliciesApi.class));
    }

    private static Policy expressionPolicy() {
        return new Policy()
            .name("A then B")
            .scope("SYSTEM")
            .expression(new PolicyExpression()
                .operator(OperatorEnum.THEN)
                .operands(List.of(
                    new PolicyExpression().approvalTypeId("type-a").count(1),
                    new PolicyExpression().approvalTypeId("type-b").count(1))));
    }
}
