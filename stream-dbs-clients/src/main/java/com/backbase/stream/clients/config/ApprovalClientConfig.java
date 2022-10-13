package com.backbase.stream.clients.config;

import com.backbase.dbs.approval.api.service.ApiClient;
import com.backbase.dbs.approval.api.service.v2.ApprovalTypeAssignmentsApi;
import com.backbase.dbs.approval.api.service.v2.ApprovalTypesApi;
import com.backbase.dbs.approval.api.service.v2.PoliciesApi;
import com.backbase.dbs.approval.api.service.v2.PolicyAssignmentsApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("backbase.communication.services.approval")
public class ApprovalClientConfig extends DbsApiClientConfig {

    public static final String APPROVAL_SERVICE_ID = "approval-service";

    public ApprovalClientConfig() {
        super(APPROVAL_SERVICE_ID);
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiClient approvalsApiClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat)
            .setBasePath(createBasePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public ApprovalTypesApi approvalTypesApi(ApiClient approvalsApiClient) {
        return new ApprovalTypesApi(approvalsApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public ApprovalTypeAssignmentsApi approvalTypeAssignmentsApi(ApiClient approvalsApiClient) {
        return new ApprovalTypeAssignmentsApi(approvalsApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public PoliciesApi policiesApi(ApiClient approvalsApiClient) {
        return new PoliciesApi(approvalsApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public PolicyAssignmentsApi policyAssignmentsApi(ApiClient approvalsApiClient) {
        return new PolicyAssignmentsApi(approvalsApiClient);
    }

}
