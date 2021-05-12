package com.backbase.stream.configuration;

import com.backbase.dbs.approval.api.client.ApiClient;
import com.backbase.dbs.approval.api.client.v2.ApprovalTypesApi;
import com.backbase.dbs.approval.api.client.v2.PoliciesApi;
import com.backbase.stream.config.BackbaseStreamConfigurationProperties;
import com.backbase.stream.service.ApprovalClientService;
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
public class ApprovalClientConfiguration {

    private final BackbaseStreamConfigurationProperties backbaseStreamConfigurationProperties;

    @Bean
    public ApprovalClientService approvalClientService(ApiClient approvalClientApiClient) {

        ApprovalTypesApi approvalTypesApi = new ApprovalTypesApi(approvalClientApiClient);
        PoliciesApi policiesApi = new PoliciesApi(approvalClientApiClient);
        return new ApprovalClientService(policiesApi, approvalTypesApi);
    }

    @Bean
    protected ApiClient approvalClientApiClient(WebClient dbsWebClient,
        ObjectMapper objectMapper, DateFormat dateFormat) {

        ApiClient apiClient = createApiClient(dbsWebClient, objectMapper, dateFormat);
        apiClient.setBasePath(backbaseStreamConfigurationProperties.getDbs().getApprovalsBaseUrl());
        log.info("ApprovalsBaseUrl: {}", backbaseStreamConfigurationProperties.getDbs().getApprovalsBaseUrl());
        return apiClient;
    }

    @NotNull
    ApiClient createApiClient(WebClient dbsWebClient, ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(dbsWebClient, objectMapper, dateFormat);
    }

}
