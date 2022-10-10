package com.backbase.stream;

import com.backbase.buildingblocks.webclient.client.ApiClientConfig;
import com.backbase.dbs.approval.api.service.ApiClient;
import com.backbase.dbs.approval.api.service.v2.ApprovalTypeAssignmentsApi;
import com.backbase.dbs.approval.api.service.v2.ApprovalTypesApi;
import com.backbase.dbs.approval.api.service.v2.PoliciesApi;
import com.backbase.dbs.approval.api.service.v2.PolicyAssignmentsApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("backbase.communication.services.approval")
public class ApprovalClientConfig extends ApiClientConfig {

    public static final String APPROVAL_SERVICE_ID = "approval-service";

    public ApprovalClientConfig() {
        super(APPROVAL_SERVICE_ID);
    }

    @Bean
    public ApiClient approvalsApiClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat)
            .setBasePath(createBasePath());
    }

    @Bean
    public ApprovalTypesApi approvalTypesApi(ApiClient approvalsApiClient) {
        return new ApprovalTypesApi(approvalsApiClient);
    }

    @Bean
    public ApprovalTypeAssignmentsApi approvalTypeAssignmentsApi(ApiClient approvalsApiClient) {
        return new ApprovalTypeAssignmentsApi(approvalsApiClient);
    }

    @Bean
    public PoliciesApi policiesApi(ApiClient approvalsApiClient) {
        return new PoliciesApi(approvalsApiClient);
    }

    @Bean
    public PolicyAssignmentsApi policyAssignmentsApi(ApiClient approvalsApiClient) {
        return new PolicyAssignmentsApi(approvalsApiClient);
    }

}
