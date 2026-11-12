package com.backbase.stream.clients.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.backbase.buildingblocks.webclient.InterServiceWebClientConfiguration;
import com.backbase.stream.webclient.configuration.DbsWebClientConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.webclient.autoconfigure.WebClientAutoConfiguration;

class ApprovalClientConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withBean(DbsWebClientConfiguration.class)
        .withBean(WebClientAutoConfiguration.class)
        .withBean(InterServiceWebClientConfiguration.class)
        .withUserConfiguration(ApprovalClientConfig.class);

    @Test
    void shouldRegisterServiceApiV2AndManagementServiceApiV1ClientsSideBySide() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(com.backbase.dbs.approval.api.service.ApiClient.class);
            assertThat(context).hasSingleBean(com.backbase.dbs.approval.api.service.v2.PoliciesApi.class);
            assertThat(context).hasSingleBean(com.backbase.dbs.approval.api.service.v2.ApprovalTypesApi.class);
            assertThat(context).hasSingleBean(com.backbase.dbs.approval.api.service.v2.PolicyAssignmentsApi.class);
            assertThat(context).hasSingleBean(
                com.backbase.dbs.approval.api.service.v2.ApprovalTypeAssignmentsApi.class);
            assertThat(context).hasSingleBean(com.backbase.dbs.approval.api.management.service.ApiClient.class);
            assertThat(context).hasSingleBean(com.backbase.dbs.approval.api.management.service.v1.PoliciesApi.class);
            assertThat(context.getBean(com.backbase.dbs.approval.api.management.service.ApiClient.class).getBasePath())
                .isEqualTo("http://approval-service");
        });
    }
}
