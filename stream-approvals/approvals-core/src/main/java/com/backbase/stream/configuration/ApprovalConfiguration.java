package com.backbase.stream.configuration;

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
public class ApprovalConfiguration {

    private final BackbaseStreamConfigurationProperties backbaseStreamConfigurationProperties;

    @Bean
    public ApprovalIntegrationService approvalIntegrationService(
        com.backbase.dbs.approval.api.integration.ApiClient approvalIntegrationApiClient/*,
        com.backbase.dbs.approval.api.service.ApiClient approvalServiceApiClient*/) {

//        ApprovalsApi approvalsServiceApi = new ApprovalsApi(approvalServiceApiClient);
        ApprovalTypesApi approvalTypesApi = new ApprovalTypesApi(approvalIntegrationApiClient);
        ApprovalTypeAssignmentsApi approvalTypeAssignmentsApi =
            new ApprovalTypeAssignmentsApi(approvalIntegrationApiClient);
        PoliciesApi policiesApi = new PoliciesApi(approvalIntegrationApiClient);
        PolicyAssignmentsApi policyAssignmentsApi = new PolicyAssignmentsApi(approvalIntegrationApiClient);
        return new ApprovalIntegrationService(approvalTypesApi, approvalTypeAssignmentsApi,
            policiesApi, policyAssignmentsApi);
    }

    @Bean
    protected com.backbase.dbs.approval.api.integration.ApiClient approvalIntegrationApiClient(WebClient dbsWebClient,
        ObjectMapper objectMapper, DateFormat dateFormat) {

        com.backbase.dbs.approval.api.integration.ApiClient apiClient =
            new com.backbase.dbs.approval.api.integration.ApiClient(dbsWebClient, objectMapper, dateFormat);
        apiClient.setBasePath(backbaseStreamConfigurationProperties.getDbs().getApprovalsBaseUrl());
        log.info("ApprovalsBaseUrl: {}", backbaseStreamConfigurationProperties.getDbs().getApprovalsBaseUrl());
        return apiClient;
    }

    /*@Bean
    protected com.backbase.dbs.approval.api.service.ApiClient approvalServiceApiClient(WebClient dbsWebClient,
        ObjectMapper objectMapper, DateFormat dateFormat) {

        com.backbase.dbs.approval.api.service.ApiClient apiClient =
            new com.backbase.dbs.approval.api.service.ApiClient(dbsWebClient, objectMapper, dateFormat);
        apiClient.setBasePath(backbaseStreamConfigurationProperties.getDbs().getApprovalsBaseUrl());
        log.info("ApprovalsBaseUrl: {}", backbaseStreamConfigurationProperties.getDbs().getApprovalsBaseUrl());
        return apiClient;
    }*/

}
