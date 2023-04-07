package com.backbase.stream.configuration;

import com.backbase.dbs.approval.api.service.v2.ApprovalTypeAssignmentsApi;
import com.backbase.dbs.approval.api.service.v2.ApprovalTypesApi;
import com.backbase.dbs.approval.api.service.v2.PoliciesApi;
import com.backbase.dbs.approval.api.service.v2.PolicyAssignmentsApi;
import com.backbase.stream.service.ApprovalsIntegrationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({AccessControlConfiguration.class})
@RequiredArgsConstructor
@Slf4j
public class ApprovalsServiceConfiguration {

    @Bean
    public ApprovalsIntegrationService approvalIntegrationService(
            ApprovalTypesApi approvalTypesApi,
            ApprovalTypeAssignmentsApi approvalTypeAssignmentsApi,
            PoliciesApi policiesApi,
            PolicyAssignmentsApi policyAssignmentsApi) {

        return new ApprovalsIntegrationService(
                approvalTypesApi, approvalTypeAssignmentsApi, policiesApi, policyAssignmentsApi);
    }
}
