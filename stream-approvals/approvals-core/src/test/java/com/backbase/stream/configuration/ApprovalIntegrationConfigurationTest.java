package com.backbase.stream.configuration;

import com.backbase.dbs.approval.api.integration.ApiClient;
import com.backbase.stream.config.BackbaseStreamConfigurationProperties;
import com.backbase.stream.config.BackbaseStreamConfigurationProperties.DbsConnectionProperties;
import com.backbase.stream.service.ApprovalIntegrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClient;

public class ApprovalIntegrationConfigurationTest {

    @Test
    public void approvalIntegrationService() {
        BackbaseStreamConfigurationProperties properties = Mockito.mock(BackbaseStreamConfigurationProperties.class);

        ApprovalIntegrationConfiguration approvalIntegrationConfiguration =
            Mockito.spy(new ApprovalIntegrationConfiguration(properties));

        ApprovalIntegrationService approvalIntegrationService =
            approvalIntegrationConfiguration.approvalIntegrationService(Mockito.mock(ApiClient.class));

        Assertions.assertNotNull(approvalIntegrationService);

    }

    @Test
    public void approvalIntegrationApiClient() {
        BackbaseStreamConfigurationProperties properties = Mockito.mock(BackbaseStreamConfigurationProperties.class);

        ApprovalIntegrationConfiguration approvalIntegrationConfiguration =
            Mockito.spy(new ApprovalIntegrationConfiguration(properties));

        String approvalBaseUrl = "http://approval";
        WebClient dbsWebClient = Mockito.mock(WebClient.class);
        ObjectMapper objectMapper = Mockito.mock(ObjectMapper.class);
        DateFormat dateFormat = Mockito.mock(DateFormat.class);
        ApiClient apiClientReturned = Mockito.mock(ApiClient.class);
        DbsConnectionProperties dbsConnectionProperties = Mockito.mock(DbsConnectionProperties.class);

        Mockito.when(properties.getDbs()).thenReturn(dbsConnectionProperties);
        Mockito.when(dbsConnectionProperties.getApprovalsBaseUrl()).thenReturn(approvalBaseUrl);

        Mockito.doReturn(apiClientReturned).when(approvalIntegrationConfiguration)
            .createApiClient(dbsWebClient, objectMapper, dateFormat);

        ApiClient apiClient =
            approvalIntegrationConfiguration.approvalIntegrationApiClient(dbsWebClient, objectMapper, dateFormat);

        Assertions.assertNotNull(apiClient);
        Assertions.assertSame(apiClientReturned, apiClient);
        Mockito.verify(apiClientReturned).setBasePath(approvalBaseUrl);
    }

}