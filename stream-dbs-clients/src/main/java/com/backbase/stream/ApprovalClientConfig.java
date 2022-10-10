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
    public ApiClient apiClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat)
            .setBasePath(createBasePath());
    }

    @Bean
    public ApprovalTypesApi approvalTypesApi(ApiClient apiClient) {
        return new ApprovalTypesApi(apiClient);
    }

    @Bean
    public ApprovalTypeAssignmentsApi approvalTypeAssignmentsApi(ApiClient apiClient) {
        return new ApprovalTypeAssignmentsApi(apiClient);
    }

    @Bean
    public PoliciesApi policiesApi(ApiClient apiClient) {
        return new PoliciesApi(apiClient);
    }

    @Bean
    public PolicyAssignmentsApi policyAssignmentsApi(ApiClient apiClient) {
        return new PolicyAssignmentsApi(apiClient);
    }

}
