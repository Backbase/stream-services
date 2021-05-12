package com.backbase.stream.configuration;

import com.backbase.dbs.approval.api.integration.ApiClient;
import com.backbase.dbs.approval.api.integration.v2.ApprovalTypeAssignmentsApi;
import com.backbase.dbs.approval.api.integration.v2.ApprovalTypesApi;
import com.backbase.dbs.approval.api.integration.v2.PoliciesApi;
import com.backbase.dbs.approval.api.integration.v2.PolicyAssignmentsApi;
import com.backbase.stream.config.BackbaseStreamConfigurationProperties;
import com.backbase.stream.service.ApprovalIntegrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@Import({AccessControlConfiguration.class})
@EnableConfigurationProperties({BackbaseStreamConfigurationProperties.class})
@RequiredArgsConstructor
@Slf4j
public class ApprovalIntegrationConfiguration {

    private final BackbaseStreamConfigurationProperties backbaseStreamConfigurationProperties;

    @Bean
    public ApprovalIntegrationService approvalIntegrationService(ApiClient approvalIntegrationApiClient) {

        ApprovalTypesApi approvalTypesApi = new ApprovalTypesApi(approvalIntegrationApiClient);
        ApprovalTypeAssignmentsApi approvalTypeAssignmentsApi =
            new ApprovalTypeAssignmentsApi(approvalIntegrationApiClient);
        PoliciesApi policiesApi = new PoliciesApi(approvalIntegrationApiClient);
        PolicyAssignmentsApi policyAssignmentsApi = new PolicyAssignmentsApi(approvalIntegrationApiClient);
        return new ApprovalIntegrationService(approvalTypesApi, approvalTypeAssignmentsApi,
            policiesApi, policyAssignmentsApi);
    }

    @Bean
    protected ApiClient approvalIntegrationApiClient(WebClient dbsWebClient, ObjectMapper objectMapper,
        DateFormat dateFormat) {

        ApiClient apiClient = createApiClient(dbsWebClient, objectMapper, dateFormat);
        apiClient.setBasePath(backbaseStreamConfigurationProperties.getDbs().getApprovalsBaseUrl());
        return apiClient;
    }

    @NotNull
    ApiClient createApiClient(WebClient dbsWebClient, ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(dbsWebClient, objectMapper, dateFormat);
    }

}
