package com.backbase.stream.portfolio.configuration;

import org.junit.jupiter.api.Test;

import com.backbase.portfolio.integration.api.service.ApiClient;

class PortfolioApiConfigurationTest extends BaseApiConfigurationTest<PortfolioApiConfiguration, ApiClient> {

    @Override
    public ApiClient getClient() {
        return getConfig().portfolioApiClient(dbsWebClient, objectMapper, dateFormat);
    }

    @Override
    public PortfolioApiConfiguration getConfig() {
        return new PortfolioApiConfiguration(backbaseStreamConfigurationProperties);
    }

    @Override
    public String getBasePath(ApiClient apiClient) {
        return apiClient.getBasePath();
    }

    @Test
    void transactionCategoryManagementApi() {
        assertBaseUrl(getConfig().transactionCategoryManagementApi(getClient()).getApiClient());
    }

    @Test
    void transactionManagementApi() {
        assertBaseUrl(getConfig().transactionManagementApi(getClient()).getApiClient());
    }

    @Test
    void positionManagementApi() {
        assertBaseUrl(getConfig().positionManagementApi(getClient()).getApiClient());
    }

    @Test
    void aggregatePortfolioManagementApi() {
        assertBaseUrl(getConfig().aggregatePortfolioManagementApi(getClient()).getApiClient());
    }

    @Test
    void portfolioValuationManagementApi() {
        assertBaseUrl(getConfig().portfolioValuationManagementApi(getClient()).getApiClient());
    }

    @Test
    void portfolioPositionsHierarchyManagementApi() {
        assertBaseUrl(getConfig().portfolioPositionsHierarchyManagementApi(getClient()).getApiClient());
    }

    @Test
    void portfolioBenchmarksManagementApi() {
        assertBaseUrl(getConfig().portfolioBenchmarksManagementApi(getClient()).getApiClient());
    }

    @Test
    void portfolioCumulativePerformanceManagementApi() {
        assertBaseUrl(getConfig().portfolioCumulativePerformanceManagementApi(getClient()).getApiClient());
    }

    @Test
    void portfolioManagementApi() {
        assertBaseUrl(getConfig().positionManagementApi(getClient()).getApiClient());
    }

    @Test
    void subPortfolioManagementApi() {
        assertBaseUrl(getConfig().subPortfolioManagementApi(getClient()).getApiClient());
    }

}